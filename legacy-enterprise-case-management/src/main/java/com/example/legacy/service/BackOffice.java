package com.example.legacy.service;
import com.example.legacy.db.Database;
import com.example.legacy.dao.*;
import com.example.legacy.model.*;
import com.example.legacy.util.*;
import java.util.*;
import org.hibernate.*;
public final class BackOffice {
    private BackOffice() {}
    public static void createUser(final String actor, final String username, final String password,
                                  final String display, final String email, final String role, final String department) {
        Database.tx(new Database.Work<Void>() {
            public Void run(Session s) {
                Services.admin(Services.requireActive(s, actor));
                String u = Strings.required(username, 60, "Username").toLowerCase(Locale.ROOT);
                if (!u.matches("[a-z][a-z0-9._-]{2,59}")) throw new IllegalArgumentException("Username must start with a letter (3-60 characters)");
                if (Services.account(s, u) != null) throw new IllegalArgumentException("Username is already taken");
                Account a = new Account(); a.setUsername(u); a.setPasswordHash(Passwords.hash(password));
                a.setDisplayName(Strings.required(display, 120, "Name"));
                a.setEmail(Strings.clean(email, 254));
                a.setRole(Strings.choice(role, Workflow.ROLES));
                a.setDepartment(Strings.required(department, 80, "Department"));
                a.setEnabled(true); a.setCreatedAt(new Date()); new AccountDao(s).save(a); new AccountDao(s).flush();
                Services.writeAudit(s, "ACCOUNT", a.getId(), "CREATE", actor, "Created " + u + " with role " + role);
                return null;
            }
        });
    }
    public static void changeRole(final String actor, final long id, final String role) {
        Database.tx(new Database.Work<Void>() {
            public Void run(Session s) {
                Services.admin(Services.requireActive(s, actor));
                Account a = new AccountDao(s).id(id);
                if (a == null) throw new IllegalArgumentException("Unknown user");
                if (a.getUsername().equals(actor)) throw new IllegalArgumentException("Cannot change your own role");
                String old = a.getRole(); a.setRole(Strings.choice(role, Workflow.ROLES));
                Services.writeAudit(s, "ACCOUNT", id, "ROLE_CHANGE", actor, a.getUsername() + ": " + old + " -> " + role);
                return null;
            }
        });
    }
    public static void toggleUser(final String actor, final long id) {
        Database.tx(new Database.Work<Void>() {
            public Void run(Session s) {
                Services.admin(Services.requireActive(s, actor));
                Account a = new AccountDao(s).id(id);
                if (a == null) throw new IllegalArgumentException("Unknown user");
                if (a.getUsername().equals(actor)) throw new IllegalArgumentException("Cannot disable yourself");
                a.setEnabled(!a.isEnabled());
                Services.writeAudit(s, "ACCOUNT", id, "ENABLE", actor, a.getUsername() + " enabled=" + a.isEnabled());
                return null;
            }
        });
    }
    @SuppressWarnings("unchecked") public static List<AuditEvent> audit(final int page) {
        return Database.tx(new Database.Work<List<AuditEvent>>() {
            public List<AuditEvent> run(Session s) {
                return new AuditDao(s).recent(page);
            }
        });
    }
    @SuppressWarnings("unchecked") public static List<CaseNotice> notifications(final String user) {
        return Database.tx(new Database.Work<List<CaseNotice>>() {
            public List<CaseNotice> run(Session s) {
                Services.requireActive(s,user);
                return new NoticeDao(s).forUser(user);
            }
        });
    }
    public static void readNotification(final String user, final long noticeId) {
        Database.tx(new Database.Work<Void>() {
            public Void run(Session s) {
                Services.requireActive(s,user);
                CaseNotice n=new NoticeDao(s).id(noticeId);
                if (n == null || !n.getRecipient().equals(user)) throw new SecurityException("Notification not accessible");
                n.setRead(true); return null;
            }
        });
    }
    @SuppressWarnings("unchecked") public static List<Object[]> groupedStatus() {
        return Database.tx(new Database.Work<List<Object[]>>() {
            public List<Object[]> run(Session s) {
                return new CaseDao(s).grouped("status");
            }
        });
    }
    @SuppressWarnings("unchecked") public static List<Object[]> groupedPriority() {
        return Database.tx(new Database.Work<List<Object[]>>() {
            public List<Object[]> run(Session s) {
                return new CaseDao(s).grouped("priority");
            }
        });
    }
    public static Long countOverdue() {
        return Database.tx(new Database.Work<Long>() {
            public Long run(Session s) {
                return new CaseDao(s).countOverdue();
            }
        });
    }
}
