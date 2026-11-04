package com.example.legacy.web;
import com.example.legacy.service.BackOffice;
import com.example.legacy.service.Services;
import com.example.legacy.model.ServiceCase;
import com.example.legacy.util.Strings;
import java.io.*;
import javax.servlet.http.*;
import org.apache.struts.action.*;
public final class ReportAction extends Action {
 @Override public ActionForward execute(ActionMapping m,ActionForm f,HttpServletRequest r,HttpServletResponse resp) {
   try {
     Web.requireReporting(r);
     if("page".equals(m.getParameter())) {
       r.setAttribute("statusRows",BackOffice.groupedStatus());r.setAttribute("priorityRows",BackOffice.groupedPriority());
       r.setAttribute("overdue",BackOffice.countOverdue());return Web.jsp("reports");
     }
     // CSV deliberately capped to 5,000 records to protect the legacy server.
     resp.setContentType("text/csv; charset=UTF-8");resp.setHeader("Content-Disposition","attachment; filename=\"cases.csv\"");
     PrintWriter w=resp.getWriter();w.println("Case Number,Title,Category,Priority,Status,Assignee,Department");
     for(int page=0;page<10;page++) {
       java.util.List<ServiceCase> batch=Services.search(null,null,null,page,500);
       if(batch.isEmpty())break;
       for(ServiceCase c:batch)w.println(Strings.csv(c.getCaseNumber())+","+Strings.csv(c.getTitle())+","+Strings.csv(c.getCategory())+","+Strings.csv(c.getPriority())+","+Strings.csv(c.getStatus())+","+Strings.csv(c.getAssignedTo())+","+Strings.csv(c.getDepartment()));
       if(batch.size()<500)break;
     }
     return null;
   }catch(Exception e){return Web.fail(r,e);}
 }
}
