package com.github.cstroe.spendhawk.web.transaction;

import com.github.cstroe.spendhawk.entity.CashFlow;
import com.github.cstroe.spendhawk.entity.Transaction;
import com.github.cstroe.spendhawk.util.DateUtil;
import com.github.cstroe.spendhawk.util.Ex;
import com.github.cstroe.spendhawk.util.HibernateUtil;
import com.github.cstroe.spendhawk.web.AccountServlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.Optional;

import static com.github.cstroe.spendhawk.util.ServletUtil.servletPath;

@WebServlet("/transaction")
public class TransactionView extends HttpServlet {

    private static final String TEMPLATE = "/template/transactions/view.ftl";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            String transactionIdRaw = request.getParameter("id");
            String fromAccountId = request.getParameter("from");
            if(transactionIdRaw != null) {
                Long transactionId = Long.parseLong(transactionIdRaw);
                // Begin unit of work
                HibernateUtil.getSessionFactory().getCurrentSession().beginTransaction();

                Transaction transaction = Transaction.findById(transactionId)
                    .orElseThrow(Ex::transactionNotFound);

                if(fromAccountId != null) {
                    request.setAttribute("fromAccountId", Long.parseLong(fromAccountId));
                } else {
                    CashFlow cf =transaction.getCashFlows().iterator().next();
                    request.setAttribute("fromAccountId", cf.getAccount().getId());
                }

                request.setAttribute("transaction", transaction);
                request.getRequestDispatcher(TEMPLATE).forward(request, response);
            }
            // End unit of work
            HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();
        } catch (Exception ex) {
            HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().rollback();
            throw new ServletException(ex);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Optional<String> updateCashflows = Optional.ofNullable(request.getParameter("update.cashflows"));
        Optional<String> updateDescription = Optional.ofNullable(request.getParameter("update.description"));
        if (updateCashflows.isPresent()) {
            doUpdateCashflowAmounts(request, response);
        } else if(updateDescription.isPresent()) {
            doUpdateDescription(request, response);
        } else {
            doView(request, response);
        }
    }

    private void doUpdateDescription(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        try {
            HibernateUtil.getSessionFactory().getCurrentSession().beginTransaction();

            String description = Optional.ofNullable(request.getParameter("description"))
                .orElseThrow(() -> new RuntimeException("Description required."));
            Long fromAccountId = Long.parseLong(request.getParameter("fromAccountId"));
            Long transactionId = Long.parseLong(request.getParameter("transactionId"));

            Transaction t = Transaction.findById(transactionId)
                .orElseThrow(Ex::transactionNotFound);

            t.setDescription(description);
            t.save();

            HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();

            response.sendRedirect(request.getContextPath() + servletPath(TransactionView.class,
                    "id", transactionId, "from", fromAccountId));
        } catch(Exception ex) {
            HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().rollback();
            throw new ServletException(ex);
        }
    }

    private void doUpdateCashflowAmounts(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        try {
            HibernateUtil.getSessionFactory().getCurrentSession().beginTransaction();

            String[] cfId = request.getParameterValues("cfid[]");
            String[] cfAmount = request.getParameterValues("cfamount[]");
            Long fromAccountId = Long.parseLong(request.getParameter("fromAccountId"));
            Long transactionId = Long.parseLong(request.getParameter("transactionId"));

            for (int i = 0; i < cfId.length; i++) {
                if (cfId[i] == null || cfAmount == null ||
                        cfId[i].isEmpty() || cfAmount[i].isEmpty()) {
                    continue;
                }

                CashFlow c = CashFlow.findById(Long.parseLong(cfId[i]))
                        .orElseThrow(Ex::cashFlowNotFound);
                c.setAmount(Double.parseDouble(cfAmount[i]));
                c.save();
            }

            // End unit of work
            HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();

            response.sendRedirect(request.getContextPath() + servletPath(TransactionView.class,
                    "id", transactionId, "from", fromAccountId));
        } catch(Exception ex) {
            HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().rollback();
            throw new ServletException(ex);
        }
    }

    private void doView(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        try {
            Long accountId;
            Date effectiveDate;

            String transactionIdRaw = Optional.ofNullable(request.getParameter("id"))
                    .orElseThrow(Ex::transactionIdRequired);
            String fromAccountIdRaw = Optional.ofNullable(request.getParameter("fromAccountId"))
                    .orElseThrow(Ex::accountIdRequired);
            accountId = Long.parseLong(fromAccountIdRaw);
            Long transactionId = Long.parseLong(transactionIdRaw);

            // Begin unit of work
            HibernateUtil.getSessionFactory().getCurrentSession().beginTransaction();
            Transaction transaction = Transaction.findById(transactionId)
                    .orElseThrow(Ex::transactionNotFound);

            effectiveDate = transaction.getEffectiveDate();

            for (CashFlow cashFlow : transaction.getCashFlows()) {
                cashFlow.delete();
            }
            transaction.delete();

            // End unit of work
            HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();

            response.sendRedirect(request.getContextPath() + servletPath(AccountServlet.class,
                    "id", accountId, "relDate", AccountServlet.formatter.format(DateUtil.asLocalDate(effectiveDate))));
        } catch (Exception ex) {
            HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().rollback();
            throw new ServletException(ex);
        }
    }
}
