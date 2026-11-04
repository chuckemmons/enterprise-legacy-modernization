package com.example.legacy.web;
import com.example.legacy.model.Account;
import javax.servlet.http.*;
import org.apache.struts.action.*;
import java.net.URLEncoder;
import java.text.*;
import java.util.*;
/** Shared servlet-facing helpers. Most business rules deliberately live below Struts. */
public final class Web {
 private Web() {}
 public static String actor(HttpServletRequest r) {
   HttpSession s=r.getSession(false);
   if(s==null || s.getAttribute("actor")==null) throw new SecurityException("Please sign in");
   return (String)s.getAttribute("actor");
 }
 public static String role(HttpServletRequest r) { return (String)r.getSession().getAttribute("role"); }
 public static boolean supervisor(HttpServletRequest r) { return "ADMIN".equals(role(r)) || "SUPERVISOR".equals(role(r)); }
 public static boolean admin(HttpServletRequest r) { return "ADMIN".equals(role(r)); }
 public static void requireSupervisor(HttpServletRequest r) { if(!supervisor(r)) throw new SecurityException("Supervisor role required"); }
 public static void requireAdmin(HttpServletRequest r) { if(!admin(r)) throw new SecurityException("Administrator role required"); }
 public static void requireReporting(HttpServletRequest r) { if(!(supervisor(r) || "AUDITOR".equals(role(r)))) throw new SecurityException("Reporting permission required"); }
 public static int page(HttpServletRequest r) {try {return Math.min(100000,Math.max(0,Integer.parseInt(r.getParameter("page"))));}catch(Exception e){return 0;}}
 public static long id(HttpServletRequest r) {try {long id=Long.parseLong(r.getParameter("id"));if(id<1)throw new NumberFormatException();return id;}catch(Exception e){throw new IllegalArgumentException("Invalid record ID");}}
 public static Integer version(HttpServletRequest r) {try{return Integer.valueOf(r.getParameter("version"));}catch(Exception e){throw new IllegalArgumentException("Missing record version");}}
 public static Date date(String date) {
   if(date==null || date.trim().isEmpty()) return null;
   try {SimpleDateFormat d=new SimpleDateFormat("yyyy-MM-dd");d.setLenient(false);return d.parse(date.trim());}
   catch(ParseException e){throw new IllegalArgumentException("Date must be yyyy-mm-dd");}
 }
 public static String fmt(Date d) {return d==null?"":new SimpleDateFormat("yyyy-MM-dd").format(d);}
 public static ActionForward jsp(String name) {return new ActionForward("/WEB-INF/views/"+name+".jsp");}
 public static ActionForward go(HttpServletRequest r,String path,String message) {
   String extra=path.contains("?")?"&":"?";
   try {return new ActionForward(path+extra+"message="+URLEncoder.encode(message,"UTF-8"),true);}
   catch(Exception e){throw new IllegalStateException(e);}
 }
 public static ActionForward fail(HttpServletRequest r,Exception e) {
   r.setAttribute("error", e instanceof SecurityException?"Access denied":e instanceof IllegalArgumentException?e.getMessage():"The request could not be completed. Check server logs.");
   return jsp("error");
 }
 public static void requestUser(HttpServletRequest r,Account a) {r.setAttribute("currentUser",a);}
}
