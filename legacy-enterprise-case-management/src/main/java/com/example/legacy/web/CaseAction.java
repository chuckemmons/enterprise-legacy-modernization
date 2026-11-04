package com.example.legacy.web;
import com.example.legacy.db.Database;
import com.example.legacy.model.*;
import com.example.legacy.service.Services;
import com.example.legacy.util.Workflow;
import java.util.*;
import org.hibernate.Session;
import org.apache.struts.action.*;
import javax.servlet.http.*;
public final class CaseAction extends Action {
 @Override public ActionForward execute(ActionMapping m,ActionForm f,HttpServletRequest r,HttpServletResponse resp) {
   String op=m.getParameter();
   try {
     final String actor=Web.actor(r);
     if("list".equals(op)) {
       String search=r.getParameter("q"),status=r.getParameter("status"),assignee=r.getParameter("assignee");
       r.setAttribute("cases",Services.search(search,status,assignee,Web.page(r),25));
       r.setAttribute("q",search);r.setAttribute("status",status);r.setAttribute("assignee",assignee);r.setAttribute("page",Web.page(r));
       return Web.jsp("cases");
     }
     if("new".equals(op))return Web.jsp("case-new");
     if("view".equals(op)) {populate(r);return Web.jsp("case-view");}
     if("create".equals(op)) {
       ServiceCase c=Services.createCase(actor,r.getParameter("title"),r.getParameter("description"),r.getParameter("category"),r.getParameter("priority"),r.getParameter("department"),Web.date(r.getParameter("due")));
       return Web.go(r,"/caseView.do?id="+c.getId(),"Case created");
     }
     long id=Web.id(r);
     if("update".equals(op)) {
       Services.update(actor,id,Web.version(r),r.getParameter("title"),r.getParameter("description"),r.getParameter("priority"),Web.date(r.getParameter("due")));
       return Web.go(r,"/caseView.do?id="+id,"Case updated");
     }
     if("assign".equals(op)) {Services.assign(actor,id,r.getParameter("assignee"));return Web.go(r,"/caseView.do?id="+id,"Case assigned");}
     if("transition".equals(op)) {Services.transition(actor,id,r.getParameter("destination"));return Web.go(r,"/caseView.do?id="+id,"Workflow advanced");}
     if("comment".equals(op)) {Services.comment(actor,id,r.getParameter("body"));return Web.go(r,"/caseView.do?id="+id,"Comment added");}
     if("archive".equals(op)) {Services.archive(actor,id);return Web.go(r,"/cases.do","Case archived");}
     throw new IllegalArgumentException("Unknown action");
   }catch(Exception e){return Web.fail(r,e);}
 }
 private void populate(final HttpServletRequest r) {
   final long id=Web.id(r);final String actor=Web.actor(r);
   Database.tx(new Database.Work<Void>() {
     public Void run(Session s) {
       Account a=Services.requireActive(s,actor);ServiceCase c=Services.caseById(s,id);
       r.setAttribute("item",c);r.setAttribute("notes",Services.notes(s,id));r.setAttribute("docs",Services.docs(s,id));
       r.setAttribute("users",Services.users(s));r.setAttribute("nextStates",Workflow.next(c.getStatus(),a.getRole()));
       r.setAttribute("mayEdit",Workflow.canWork(a.getRole()) && !"CLOSED".equals(c.getStatus()) &&
          (!"WORKER".equals(a.getRole()) || actor.equals(c.getAssignedTo()) || actor.equals(c.getCreatedBy())));
       r.setAttribute("mayComment",Workflow.canWork(a.getRole()));return null;
     }
   });
 }
}
