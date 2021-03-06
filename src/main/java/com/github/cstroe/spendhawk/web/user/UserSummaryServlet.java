package com.github.cstroe.spendhawk.web.user;

import com.github.cstroe.spendhawk.dao.UserDao;
import com.github.cstroe.spendhawk.util.HibernateUtil;
import com.github.cstroe.spendhawk.util.TemplateForwarder;
import org.hibernate.criterion.Restrictions;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/summary")
public class UserSummaryServlet extends HttpServlet {

    private static final String TEMPLATE = "/template/user/summary.ftl";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Long userId = Long.parseLong(request.getParameter("user.id"));

        try {
            HibernateUtil.getSessionFactory().getCurrentSession().beginTransaction();
            UserDao user = (UserDao) HibernateUtil.getSessionFactory().getCurrentSession()
                    .createCriteria(UserDao.class)
                    .add(Restrictions.eq("id", userId))
                    .uniqueResult();



            request.setAttribute("fw", new TemplateForwarder(request));
            request.setAttribute("user", user);
            request.setAttribute("accounts", user.getAccounts());
            request.getRequestDispatcher(TEMPLATE).forward(request,response);
            HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();
        } catch(Exception ex) {
            HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().rollback();
            throw new ServletException(ex);
        }
    }
}
