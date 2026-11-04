package com.example.legacy.web;
import com.example.legacy.service.BackOffice;
import javax.servlet.http.*;
import org.apache.struts.action.*;
public final class AuditAction extends Action {
 @Override public ActionForward execute(ActionMapping m,ActionForm f,HttpServletRequest r,HttpServletResponse resp) {
   try{Web.requireReporting(r);r.setAttribute("events",BackOffice.audit(Web.page(r)));r.setAttribute("page",Web.page(r));return Web.jsp("audit");}
   catch(Exception e){return Web.fail(r,e);}
 }
}
