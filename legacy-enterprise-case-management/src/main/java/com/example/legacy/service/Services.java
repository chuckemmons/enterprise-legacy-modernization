package com.example.legacy.service;
import com.example.legacy.db.Database;
import com.example.legacy.dao.*;
import com.example.legacy.model.*;
import com.example.legacy.util.*;
import java.util.*;
import org.hibernate.*;
public final class Services {
    private Services() {}
    public static Account account(Session s, String username) {
        return new AccountDao(s).username(username);
    }
    public static Account requireActive(Session s, String username) {
        Account user = account(s, username);
        if (user == null || !user.isEnabled()) throw new SecurityException("Account unavailable");
        return user;
    }
    public static Account authenticate(final String username, final String password) {
        return Database.tx(new Database.Work<Account>() {
            public Account run(Session s) {
                Account u = account(s, username);
                if (u == null || !u.isEnabled() || !Passwords.matches(password, u.getPasswordHash())) return null;
                // Return a detached object, with credential removed from the HTTP session by caller.
                return u;
            }
        });
    }
    public static ServiceCase caseById(Session s, long id) {
        ServiceCase c = new CaseDao(s).id(id);
        if (c == null || c.isArchived()) throw new IllegalArgumentException("Case not found");
        return c;
    }
    public static void supervisor(Account user) {
        if (!Workflow.isSupervisor(user.getRole())) throw new SecurityException("Supervisor permission required");
    }
    public static void worker(Account user) {
        if (!Workflow.canWork(user.getRole())) throw new SecurityException("Read-only access");
    }
    public static void admin(Account user) {
        if (!"ADMIN".equals(user.getRole())) throw new SecurityException("Administrator permission required");
    }
    public static void writeAudit(Session s, String type, Long id, String action, String actor, String detail) {
        AuditEvent event = new AuditEvent(); event.setEntityType(type); event.setEntityId(id);
        event.setActionName(action); event.setActor(actor); event.setDetails(Strings.clean(detail, 4000));
        event.setCreatedAt(new Date()); new AuditDao(s).save(event);
    }
    public static void notice(Session s, String username, Long caseId, String subject, String body) {
        if (username == null || username.isEmpty()) return;
        CaseNotice notice = new CaseNotice(); notice.setRecipient(username); notice.setCaseId(caseId);
        notice.setSubject(Strings.clean(subject, 180)); notice.setMessage(Strings.clean(body, 4000));
        notice.setCreatedAt(new Date()); notice.setRead(false); new NoticeDao(s).save(notice);
    }
    public static ServiceCase createCase(final String actor, final String title, final String description,
                                          final String category, final String priority, final String department, final Date dueAt) {
        return Database.tx(new Database.Work<ServiceCase>() {
            public ServiceCase run(Session s) {
                Account u = requireActive(s, actor); worker(u);
                ServiceCase c = new ServiceCase(); c.setCaseNumber("TMP-" + UUID.randomUUID().toString().replace("-", ""));
                c.setTitle(Strings.required(title, 180, "Title"));
                c.setDescription(Strings.required(description, 4000, "Description"));
                c.setCategory(Strings.required(category, 60, "Category"));
                c.setPriority(Strings.choice(priority, "LOW", "MEDIUM", "HIGH", "CRITICAL"));
                c.setDepartment(Strings.required(department, 80, "Department"));
                c.setCreatedBy(actor); c.setStatus("NEW"); c.setCreatedAt(new Date());
                c.setUpdatedAt(new Date()); c.setDueAt(dueAt); c.setArchived(false); c.setOverdueNoticeSent(false);
                new CaseDao(s).save(c); new CaseDao(s).flush();
                c.setCaseNumber(String.format("CASE-%06d", c.getId()));
                writeAudit(s, "CASE", c.getId(), "CREATE", actor, "Case opened: " + c.getCaseNumber());
                return c;
            }
        });
    }
    public static List<ServiceCase> search(final String search, final String status, final String assignee, final int page, final int size) {
        return Database.tx(new Database.Work<List<ServiceCase>>() {
            @SuppressWarnings("unchecked") public List<ServiceCase> run(Session s) {
                return new CaseDao(s).search(search,status,assignee,page,size);
            }
        });
    }
    public static ServiceCase update(final String actor, final long id, final Integer version,
                                     final String title, final String description, final String priority, final Date due) {
        return Database.tx(new Database.Work<ServiceCase>() {
            public ServiceCase run(Session s) {
                Account a = requireActive(s, actor); worker(a);
                ServiceCase c = caseById(s, id);
                if ("CLOSED".equals(c.getStatus())) throw new IllegalArgumentException("Closed cases cannot be edited");
                if ("WORKER".equals(a.getRole()) && !actor.equals(c.getAssignedTo()) && !actor.equals(c.getCreatedBy()))
                    throw new SecurityException("Case not assigned to you");
                if (version == null || !version.equals(c.getVersion()))
                    throw new IllegalArgumentException("Another user updated this case. Refresh and retry.");
                c.setTitle(Strings.required(title, 180, "Title"));
                c.setDescription(Strings.required(description, 4000, "Description"));
                c.setPriority(Strings.choice(priority, "LOW", "MEDIUM", "HIGH", "CRITICAL"));
                c.setDueAt(due); c.setUpdatedAt(new Date());
                writeAudit(s, "CASE", id, "UPDATE", actor, "Case details updated"); return c;
            }
        });
    }
    public static void assign(final String actor, final long id, final String username) {
        Database.tx(new Database.Work<Void>() {
            public Void run(Session s) {
                supervisor(requireActive(s, actor));
                Account target = requireActive(s, username); worker(target);
                ServiceCase c = caseById(s, id);
                if ("CLOSED".equals(c.getStatus())) throw new IllegalArgumentException("Closed cases cannot be reassigned");
                String previous = c.getAssignedTo();
                c.setAssignedTo(username); c.setUpdatedAt(new Date());
                writeAudit(s, "CASE", id, "ASSIGN", actor, "From " + previous + " to " + username);
                notice(s, username, id, "Case assigned: " + c.getCaseNumber(), c.getTitle()); return null;
            }
        });
    }
    public static void transition(final String actor, final long id, final String destination) {
        Database.tx(new Database.Work<Void>() {
            public Void run(Session s) {
                Account a = requireActive(s, actor);
                ServiceCase c = caseById(s, id);
                if (!Workflow.mayTransition(a.getRole(), c.getStatus(), destination))
                    throw new SecurityException("Transition not permitted");
                if ("WORKER".equals(a.getRole()) && !actor.equals(c.getAssignedTo()))
                    throw new SecurityException("Only the assigned worker may change status");
                if ("PENDING_APPROVAL".equals(c.getStatus()) && "APPROVED".equals(destination) && actor.equals(c.getCreatedBy()))
                    throw new IllegalArgumentException("Creator cannot self-approve");
                String old = c.getStatus(); c.setStatus(destination); c.setUpdatedAt(new Date());
                if ("CLOSED".equals(destination)) c.setClosedAt(new Date());
                writeAudit(s, "CASE", id, "TRANSITION", actor, old + " -> " + destination);
                notice(s, c.getCreatedBy(), id, "Case status: " + c.getCaseNumber(), old + " -> " + destination);
                if (!actor.equals(c.getAssignedTo())) notice(s, c.getAssignedTo(), id, "Case status: " + c.getCaseNumber(), old + " -> " + destination);
                return null;
            }
        });
    }
    public static void comment(final String actor, final long id, final String body) {
        Database.tx(new Database.Work<Void>() {
            public Void run(Session s) {
                worker(requireActive(s, actor));
                ServiceCase c = caseById(s, id);
                CaseNote n = new CaseNote(); n.setCaseId(id); n.setAuthor(actor);
                n.setBody(Strings.required(body, 4000, "Comment")); n.setCreatedAt(new Date()); new CaseNoteDao(s).save(n);
                writeAudit(s, "CASE", id, "COMMENT", actor, "Comment added");
                if (!actor.equals(c.getAssignedTo())) notice(s, c.getAssignedTo(), id, "Case comment: " + c.getCaseNumber(), actor + " added a comment");
                return null;
            }
        });
    }
    public static void archive(final String actor, final long id) {
        Database.tx(new Database.Work<Void>() {
            public Void run(Session s) {
                admin(requireActive(s, actor)); ServiceCase c = caseById(s, id);
                c.setArchived(true); c.setUpdatedAt(new Date());
                writeAudit(s, "CASE", id, "ARCHIVE", actor, "Case archived (soft delete)"); return null;
            }
        });
    }
    @SuppressWarnings("unchecked") public static List<CaseNote> notes(Session s, long id) {
        return new CaseNoteDao(s).byCase(id);
    }
    @SuppressWarnings("unchecked") public static List<CaseDocument> docs(Session s, long id) {
        return new CaseDocumentDao(s).byCase(id);
    }
    @SuppressWarnings("unchecked") public static List<Account> users(Session s) {
        return new AccountDao(s).all();
    }
}
