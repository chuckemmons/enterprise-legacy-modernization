package com.example.legacy.service;
import com.example.legacy.db.Database;
import com.example.legacy.dao.*;
import com.example.legacy.model.*;
import java.util.*;
import java.util.logging.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.hibernate.Session;
public final class BatchJobs {
    private static final Logger LOG=Logger.getLogger(BatchJobs.class.getName());
    private BatchJobs() {}
    @SuppressWarnings("unchecked") public static void createOverdueNotifications() {
        Database.tx(new Database.Work<Void>() {
            public Void run(Session s) {
                List<ServiceCase> cases=new CaseDao(s).overdueUnnotified();
                for (ServiceCase c:cases) {
                    String u=c.getAssignedTo()==null?c.getCreatedBy():c.getAssignedTo();
                    Services.notice(s,u,c.getId(),"Overdue case: " + c.getCaseNumber(),c.getTitle());
                    c.setOverdueNoticeSent(true);
                }
                return null;
            }
        });
    }
    @SuppressWarnings("unchecked") public static void sendPendingEmail() {
        final String host=System.getenv("SMTP_HOST");
        final String from=System.getenv("SMTP_FROM");
        if (host==null || host.isEmpty() || from==null || from.isEmpty()) return;
        List<Long> ids=Database.tx(new Database.Work<List<Long>>() {
            public List<Long> run(Session s) {
                return new NoticeDao(s).pendingIds();
            }
        });
        for(final Long id:ids) {
            try {
                // Best-effort email. Durable in-app alerts remain if SMTP fails.
                Boolean sent=Database.tx(new Database.Work<Boolean>() {
                    public Boolean run(Session s) {
                        CaseNotice n=new NoticeDao(s).id(id);
                        if (n==null || n.getSentAt()!=null) return false;
                        Account recipient=Services.account(s,n.getRecipient());
                        if (recipient==null || recipient.getEmail()==null || recipient.getEmail().trim().isEmpty()) {
                            n.setSentAt(new Date()); return false;
                        }
                        Properties p=new Properties(); p.put("mail.smtp.host",host);
                        p.put("mail.smtp.port",System.getenv("SMTP_PORT")==null?"25":System.getenv("SMTP_PORT"));
                        p.put("mail.smtp.starttls.enable","true");
                        String user=System.getenv("SMTP_USER"); final String password=System.getenv("SMTP_PASSWORD");
                        if(user!=null && !user.isEmpty()) p.put("mail.smtp.auth","true");
                        final String smtpUser=user;
                        javax.mail.Session mailSession=javax.mail.Session.getInstance(p, user==null?null:new Authenticator(){
                            protected PasswordAuthentication getPasswordAuthentication() { return new PasswordAuthentication(smtpUser,password); }
                        });
                        try {
                            MimeMessage m=new MimeMessage(mailSession);m.setFrom(new InternetAddress(from));
                            m.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient.getEmail(),true));
                            m.setSubject(n.getSubject(),"UTF-8");m.setText(n.getMessage(),"UTF-8");
                            Transport.send(m);
                        } catch (MessagingException e) { throw new IllegalStateException("SMTP failure",e); }
                        n.setSentAt(new Date()); return true;
                    }
                });
            } catch (RuntimeException e) { LOG.log(Level.WARNING,"Notification delivery failed (will retry)",e); }
        }
    }
}
