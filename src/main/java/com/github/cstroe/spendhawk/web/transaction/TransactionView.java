package com.github.cstroe.spendhawk.web.transaction;

import com.github.cstroe.spendhawk.dao.TransactionDao;
import com.github.cstroe.spendhawk.repository.TransactionRepository;
import com.github.cstroe.spendhawk.util.Ex;
import com.github.cstroe.spendhawk.util.HibernateUtil;

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

    private TransactionRepository transactionRepository;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            String transactionIdRaw = request.getParameter("id");
            String fromAccountId = request.getParameter("from");
            if(fromAccountId == null) {
                fromAccountId = request.getParameter("fromAccountId");
            }
            if(fromAccountId == null) {
                throw new RuntimeException("Account ID required.");
            }

            if(transactionIdRaw != null) {
                Integer transactionId = Integer.parseInt(transactionIdRaw);
                // Begin unit of work
                HibernateUtil.getSessionFactory().getCurrentSession().beginTransaction();

                TransactionDao transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(Ex::transactionNotFound);

                request.setAttribute("fromAccountId", Long.parseLong(fromAccountId));
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
            doTransactionDelete(request, response);
        }
    }

    private void doUpdateDescription(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        try {
            String description = Optional.ofNullable(request.getParameter("description"))
                .orElseThrow(() -> new RuntimeException("Description required."));
            Integer fromAccountId = Integer.parseInt(request.getParameter("fromAccountId"));
            Integer transactionId = Integer.parseInt(request.getParameter("transactionId"));

            TransactionDao transaction = transactionRepository.findById(transactionId)
                .orElseThrow(Ex::transactionNotFound);

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

            String[] cfId = Optional.ofNullable(request.getParameterValues("cfid[]"))
                .orElseThrow(() -> new ServletException("cfid[] not initialized"));
            String[] cfAmount = Optional.ofNullable(request.getParameterValues("cfamount[]"))
                .orElseThrow(() -> new ServletException("cfamount[] not initialized"));
            String[] cfAccounts = Optional.ofNullable(request.getParameterValues("toAccountId[]"))
                .orElseThrow(() -> new ServletException("toAccountId[] not initialized"));

            Long fromAccountId = Long.parseLong(request.getParameter("fromAccountId"));
            Long transactionId = Long.parseLong(request.getParameter("transactionId"));

            for (int i = 0; i < cfId.length; i++) {
                if (cfId[i] == null || cfAmount[i] == null || cfAccounts[i] == null ||
                        cfId[i].isEmpty() || cfAmount[i].isEmpty() || cfAccounts[i].isEmpty()) {
                    continue;
                }
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

    private void doTransactionDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        try {
            Long accountId;
            Date effectiveDate = null;

            String transactionIdRaw = Optional.ofNullable(request.getParameter("id"))
                    .orElseThrow(Ex::transactionIdRequired);
            String fromAccountIdRaw = Optional.ofNullable(request.getParameter("fromAccountId"))
                    .orElseThrow(Ex::accountIdRequired);
            accountId = Long.parseLong(fromAccountIdRaw);
            Integer transactionId = Integer.parseInt(transactionIdRaw);

            // Begin unit of work
            HibernateUtil.getSessionFactory().getCurrentSession().beginTransaction();
            TransactionDao transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(Ex::transactionNotFound);

            // End unit of work
            HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();

//            if(effectiveDate == null) {
//                response.sendRedirect(request.getContextPath() + servletPath(AccountServlet.class,
//                        "id", accountId, "relDate", "currentMonth"));
//            } else {
//                response.sendRedirect(request.getContextPath() + servletPath(AccountServlet.class,
//                        "id", accountId, "relDate", AccountServlet.formatter.format(DateUtil.asLocalDate(effectiveDate))));
//            }
        } catch (Exception ex) {
            HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().rollback();
            throw new ServletException(ex);
        }
    }
}
