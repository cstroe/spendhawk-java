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
        Long accountId;
        Date effectiveDate;
        try {
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

            for(CashFlow cashFlow : transaction.getCashFlows()) {
                cashFlow.delete();
            }
            transaction.delete();

            // End unit of work
            HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();
        } catch (Exception ex) {
            HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().rollback();
            throw new ServletException(ex);
        }

        response.sendRedirect(request.getContextPath() + servletPath(AccountServlet.class,
                "id", accountId, "relDate", AccountServlet.formatter.format(DateUtil.asLocalDate(effectiveDate))));
    }
}
