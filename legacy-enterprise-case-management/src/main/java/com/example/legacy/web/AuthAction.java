package com.example.legacy.web;
import com.example.legacy.model.Account;
import com.example.legacy.service.Services;
import org.apache.struts.action.*;
import javax.servlet.http.*;
public final class AuthAction extends Action {
 @Override public ActionForward execute(ActionMapping mapping,ActionForm form,HttpServletRequest r,HttpServletResponse resp) {
   String op=mapping.getParameter();
   if("login".equals(op)) return Web.jsp("login");
   if("logout".equals(op)) {HttpSession s=r.getSession(false);if(s!=null)s.invalidate();return new ActionForward("/login.do",true);}
   if("authenticate".equals(op)) {
     try {
       String user=r.getParameter("username");String password=r.getParameter("password");
       Account a=Services.authenticate(user==null?"":user.trim().toLowerCase(java.util.Locale.ROOT),password);
       if(a==null) {r.setAttribute("error","Invalid credentials or disabled account");return Web.jsp("login");}
       HttpSession old=r.getSession(false);if(old!=null)old.invalidate();
       HttpSession s=r.getSession(true);s.setAttribute("actor",a.getUsername());s.setAttribute("role",a.getRole());s.setAttribute("displayName",a.getDisplayName());
       byte[] token=new byte[32];new java.security.SecureRandom().nextBytes(token);
       s.setAttribute("csrf",java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(token));
       return new ActionForward("/dashboard.do",true);
     } catch(Exception e){return Web.fail(r,e);}
   }
   return Web.jsp("error");
 }
}
