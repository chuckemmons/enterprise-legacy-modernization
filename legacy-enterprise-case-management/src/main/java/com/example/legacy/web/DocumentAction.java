package com.example.legacy.web;
import com.example.legacy.db.Database;
import com.example.legacy.dao.CaseDocumentDao;
import com.example.legacy.model.*;
import com.example.legacy.service.Services;
import com.example.legacy.util.Strings;
import org.apache.struts.action.*;
import org.apache.struts.upload.FormFile;
import org.hibernate.Session;
import javax.servlet.http.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
public final class DocumentAction extends Action {
 private static final int LIMIT=2*1024*1024;
 @Override public ActionForward execute(ActionMapping m,ActionForm form,HttpServletRequest r,HttpServletResponse resp) {
   try {
     final String actor=Web.actor(r);
     if("upload".equals(m.getParameter())) {
       UploadForm uf=(UploadForm)form;
       if(uf==null || uf.getCsrf()==null || !MessageDigest.isEqual(uf.getCsrf().getBytes("UTF-8"),r.getSession().getAttribute("csrf").toString().getBytes("UTF-8")))
         throw new SecurityException("Invalid upload token");
       final long caseId=Long.parseLong(uf.getId());
       FormFile file=uf.getFile();if(file==null || file.getFileSize()==0 || file.getFileSize()>LIMIT)throw new IllegalArgumentException("File must be 1 byte to 2 MB");
       final String name=Strings.filename(file.getFileName());
       final String contentType=file.getContentType()==null?"application/octet-stream":file.getContentType();
       if(!Arrays.asList("application/pdf","image/jpeg","image/png","text/plain","application/octet-stream").contains(contentType))
         throw new IllegalArgumentException("Unsupported file type");
       final byte[] bytes=file.getFileData();if(bytes.length>LIMIT)throw new IllegalArgumentException("File too large");
       Database.tx(new Database.Work<Void>() {public Void run(Session s) {
         Account a=Services.requireActive(s,actor);Services.worker(a);
         ServiceCase c=Services.caseById(s,caseId);
         if("CLOSED".equals(c.getStatus()))throw new IllegalArgumentException("Closed case");
         if("WORKER".equals(a.getRole())&&!actor.equals(c.getAssignedTo())&&!actor.equals(c.getCreatedBy()))throw new SecurityException("Not your case");
         CaseDocument d=new CaseDocument();d.setCaseId(caseId);d.setOriginalName(name);d.setContentType(contentType);
         d.setContentSize(bytes.length);d.setPayload(bytes);d.setUploadedBy(actor);d.setCreatedAt(new Date());new CaseDocumentDao(s).save(d);
         Services.writeAudit(s,"CASE",caseId,"UPLOAD",actor,"Uploaded " + name);return null;
       }});
       file.destroy();return Web.go(r,"/caseView.do?id="+caseId,"Document uploaded");
     }
     final long documentId=Web.id(r);
     CaseDocument d=Database.tx(new Database.Work<CaseDocument>() {public CaseDocument run(Session s) {
       Services.requireActive(s,actor);CaseDocument doc=new CaseDocumentDao(s).id(documentId);
       if(doc==null)throw new IllegalArgumentException("Document not found");Services.caseById(s,doc.getCaseId());return doc;
     }});
     resp.setContentType("application/octet-stream");resp.setContentLength(d.getContentSize());
     resp.setHeader("Content-Disposition","attachment; filename=\""+Strings.filename(d.getOriginalName())+"\"");
     resp.setHeader("X-Content-Type-Options","nosniff");resp.getOutputStream().write(d.getPayload());return null;
   }catch(Exception e){return Web.fail(r,e);}
 }
}
