# Local Tomcat/PostgreSQL smoke-test checklist

This file is a manual test plan, not a claim that tests have run. Perform these tests with seeded local accounts.

- [ ] Ant resolves Ivy dependencies, compiles all sources and builds both WAR and EAR artifacts.
- [ ] Tomcat 9 starts the WAR on Java 8, initializes native Hibernate SessionFactory and validates DB schema.
- [ ] `admin` can log in and create a user; invalid password is rejected; logout invalidates session.
- [ ] Anonymous `.do` request redirects to login.
- [ ] POST without a CSRF token is blocked (403).
- [ ] Supervisor can assign a case, worker receives an in-app notice.
- [ ] Worker can create a case and view it after a separate login.
- [ ] New case follows NEW → TRIAGED → IN_PROGRESS → PENDING_APPROVAL → APPROVED → CLOSED.
- [ ] Supervisor can reject a pending case; worker can reopen REJECTED → IN_PROGRESS.
- [ ] Worker cannot approve; supervisor cannot self-approve a case they created.
- [ ] Worker cannot edit or change status of another user's assigned case.
- [ ] Concurrent edits to a case using stale versions are rejected.
- [ ] Worker can add a note and a small document; download returns original file as an attachment.
- [ ] Auditor cannot create or modify a case; auditor can read reports and audit history.
- [ ] Case search/filter and page navigation work with 30+ seeded cases.
- [ ] A supervisor can export CSV; a cell beginning with `=` cannot become a formula on import.
- [ ] Run `ant job-overdue` after creating a past-due case; notification appears in UI.
- [ ] Account disable blocks subsequent authenticated requests and a role change applies on the next request.
- [ ] Validate EAR with a separate full Java EE server; Tomcat is WAR-only.

Expected outstanding work: verify JSP taglib behavior, Hibernate dialect against installed PostgreSQL version and legacy dependency interactions in a real Tomcat process. Older Struts/Hibernate versions may require environment-specific tuning.
