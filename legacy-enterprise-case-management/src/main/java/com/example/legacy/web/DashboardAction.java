package com.example.legacy.web;
import org.apache.struts.action.*;
import javax.servlet.http.*;
import com.example.legacy.service.BackOffice;
public final class DashboardAction extends Action {
 @Override public ActionForward execute(ActionMapping m,ActionForm f,HttpServletRequest r,HttpServletResponse resp) {
   try {Web.actor(r);r.setAttribute("statuses",BackOffice.groupedStatus());r.setAttribute("overdue",BackOffice.countOverdue());return Web.jsp("dashboard");}
   catch(Exception e){return Web.fail(r,e);}
 }
}
