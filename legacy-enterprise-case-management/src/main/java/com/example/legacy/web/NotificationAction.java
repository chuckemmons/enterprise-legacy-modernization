package com.example.legacy.web;
import com.example.legacy.service.BackOffice;
import javax.servlet.http.*;
import org.apache.struts.action.*;
public final class NotificationAction extends Action {
 @Override public ActionForward execute(ActionMapping m,ActionForm f,HttpServletRequest r,HttpServletResponse resp) {
   try {
     String actor=Web.actor(r);
     if("list".equals(m.getParameter())){r.setAttribute("notices",BackOffice.notifications(actor));return Web.jsp("notifications");}
     BackOffice.readNotification(actor,Web.id(r));return Web.go(r,"/notifications.do","Notification marked as read");
   }catch(Exception e){return Web.fail(r,e);}
 }
}
