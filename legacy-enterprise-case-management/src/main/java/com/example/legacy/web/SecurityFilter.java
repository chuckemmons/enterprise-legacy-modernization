package com.example.legacy.web;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.security.*;
import java.util.*;
import com.example.legacy.db.Database;
import com.example.legacy.service.Services;
import com.example.legacy.model.Account;
import org.hibernate.Session;
/** Basic login and CSRF enforcement, intentionally separate from legacy business code. */
public final class SecurityFilter implements Filter {
 private final SecureRandom rand=new SecureRandom();
 public void init(FilterConfig cfg) {}
 public void destroy() {}
 public void doFilter(ServletRequest in,ServletResponse out,FilterChain chain) throws IOException,ServletException {
   HttpServletRequest r=(HttpServletRequest)in;HttpServletResponse resp=(HttpServletResponse)out;
   resp.setHeader("X-Content-Type-Options","nosniff");resp.setHeader("X-Frame-Options","DENY");
   resp.setHeader("Cache-Control","no-store");
   HttpSession s=r.getSession(true);
   if(s.getAttribute("csrf")==null) {byte[] token=new byte[32];rand.nextBytes(token);s.setAttribute("csrf",Base64.getUrlEncoder().withoutPadding().encodeToString(token));}
   String url=r.getServletPath();
   boolean publicPath="/login.do".equals(url)||"/authenticate.do".equals(url);
   if(!publicPath && s.getAttribute("actor")==null) {resp.sendRedirect(r.getContextPath()+"/login.do");return;}
   if(!publicPath) {
     try {
       final String username=(String)s.getAttribute("actor");
       Account a=Database.tx(new Database.Work<Account>() { public Account run(Session db) {return Services.requireActive(db,username);} });
       s.setAttribute("role",a.getRole());s.setAttribute("displayName",a.getDisplayName());
     } catch(SecurityException ex) {s.invalidate();resp.sendRedirect(r.getContextPath()+"/login.do");return;}
   }
   if("POST".equalsIgnoreCase(r.getMethod()) && !"/caseUpload.do".equals(url)) {
     String submitted=r.getParameter("csrf");Object original=s.getAttribute("csrf");
     if(submitted==null || original==null || !MessageDigest.isEqual(submitted.getBytes("UTF-8"),original.toString().getBytes("UTF-8"))) {
       resp.sendError(HttpServletResponse.SC_FORBIDDEN,"Invalid CSRF token");return;
     }
   }
   // Do not permit GET to perform mutations.
   if(("/authenticate.do".equals(url)||"/logout.do".equals(url)||"/caseCreate.do".equals(url)||
       "/caseUpdate.do".equals(url)||"/caseAssign.do".equals(url)||"/caseTransition.do".equals(url)||
       "/caseComment.do".equals(url)||"/caseArchive.do".equals(url)||"/caseUpload.do".equals(url)||
       "/userCreate.do".equals(url)||"/userRole.do".equals(url)||"/userDisable.do".equals(url)||
       "/notificationRead.do".equals(url)) && !"POST".equalsIgnoreCase(r.getMethod())) {
     resp.sendError(405,"POST required");return;
   }
   chain.doFilter(in,out);
 }
}
