# Architecture: the legacy baseline

```
Browser (JSP views, HTML POST forms)
              |
       Tomcat 9 / WAR          [Java EE EAR wraps WAR for a full server]
              |
  SecurityFilter (login + CSRF)
              |
    Struts 1 ActionServlet
              |
     Action + ActionForm
              |
   Services / BackOffice
              |
 DAO classes: AccountDao, CaseDao, CaseNoteDao, CaseDocumentDao,
              AuditDao, NoticeDao
              |
   Hibernate 3.5 SessionFactory
              |
   XML Hibernate hbm mappings
              |
         PostgreSQL
```

The Struts Actions should contain only request routing, form handling and model-to-view preparation. The service layer owns transaction boundaries (`Database.tx`), workflow authorization and audit/notification side effects. DAO classes hold all native Hibernate Session/HQL persistence queries; no `EntityManager` or JPA APIs are used. `.hbm.xml` maps plain model beans to database tables. Web views are JSP/JSTL; all authenticated navigation uses `.do` Struts mappings.

## Roles and workflow

| Current status | Allowed next | Authorized |
|---|---|---|
| NEW | TRIAGED | SUPERVISOR, ADMIN |
| TRIAGED | IN_PROGRESS | WORKER (assigned), SUPERVISOR, ADMIN |
| IN_PROGRESS | PENDING_APPROVAL | WORKER (assigned), SUPERVISOR, ADMIN |
| PENDING_APPROVAL | APPROVED, REJECTED | SUPERVISOR, ADMIN |
| REJECTED | IN_PROGRESS | WORKER (assigned), SUPERVISOR, ADMIN |
| APPROVED | CLOSED | SUPERVISOR, ADMIN |

A case creator cannot approve their own case. An administrator can archive a case (soft-delete). All mutations are recorded in `audit_events` where supported by the legacy service. `AUDITOR` can view data and reports but not mutate it.

## Tables

- `app_users`: native credential/role store
- `service_cases`: case content, owner, status, due date and optimistic version
- `case_notes`: case comments
- `case_documents`: small binary attachments
- `audit_events`: append-only-style application event log
- `case_notices`: persistent in-app notices with read/email timestamps

## Controlled modernization seams

1. **Reverse proxy**: route selected browser paths to the new app without changing the legacy URLs in bulk.
2. **Stable data ownership**: initially read legacy case tables through a new readonly adapter; move one writing capability only after defining its new owner.
3. **Identity**: use a compatible user identity bridge while migrating login. Avoid two independent incompatible session-cookie systems.
4. **Audit**: preserve the authoritative event history while write paths move.
5. **Rollback**: retain a path-by-path reversible routing switch; never concurrently write the same row through two implementations without explicit concurrency handling.

See `MODERNIZATION_BACKLOG.md` for phased user stories.
