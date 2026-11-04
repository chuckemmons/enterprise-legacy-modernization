package com.example.legacy.web;
import com.example.legacy.db.Database;
import com.example.legacy.model.Account;
import com.example.legacy.service.*;
import org.hibernate.Session;
import javax.servlet.http.*;
import org.apache.struts.action.*;
public final class UserAction extends Action {
 @Override public ActionForward execute(ActionMapping m,ActionForm f,HttpServletRequest r,HttpServletResponse resp) {
   try {
     Web.requireAdmin(r);String actor=Web.actor(r);String op=m.getParameter();
     if("list".equals(op)) {
       r.setAttribute("users",Database.tx(new Database.Work<java.util.List<Account>>() {
         public java.util.List<Account> run(Session s){return Services.users(s);}
       }));return Web.jsp("users");
     }
     if("create".equals(op))BackOffice.createUser(actor,r.getParameter("username"),r.getParameter("password"),r.getParameter("name"),r.getParameter("email"),r.getParameter("role"),r.getParameter("department"));
     else if("role".equals(op))BackOffice.changeRole(actor,Web.id(r),r.getParameter("role"));
     else if("disable".equals(op))BackOffice.toggleUser(actor,Web.id(r));
     else throw new IllegalArgumentException("Unknown action");
     return Web.go(r,"/users.do","User updated");
   }catch(Exception e){return Web.fail(r,e);}
 }
}
