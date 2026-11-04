# Legacy Enterprise Case Management (Struts 1 + Hibernate 3.5)

An **educational, intentionally legacy** Java EE web application designed as the starting point of a Strangler Fig modernization. Its business flow is **case opened → supervisor triage/assignment → worker processing → submitted for approval → supervisor approval/rejection → closure**. It uses classic Struts 1 Action classes, ActionForms, JSP/JSTL, a service layer, Hibernate native `Session` APIs and explicit DAO classes. **No REST API, JPA, annotations, Spring, or Maven build** is used in application code.

> **Security and compatibility:** Struts 1.3.10 and Hibernate 3.5.6-Final are end-of-life. This code is for a private local development lab, NOT an Internet-facing or production service. Do not place sensitive data in it. A complete compile and Tomcat smoke test are required in your environment; the source package is not precompiled.

## Technology

- Java **8** (use a JDK 8 runtime for the old libraries)
- Apache Struts **1.3.10** with `.do` actions and JSP/JSTL views
- Hibernate **3.5.6-Final** (native `Session`, `Transaction`, HQL, XML `.hbm.xml` mappings)
- PostgreSQL **12+**, JDBC driver 42.2.27
- Apache Ant **1.10+**, Ivy **2.5.3** downloaded by the Ant build (Ivy resolves dependencies from Maven Central; **Maven is not used as a build tool**)
- Apache Tomcat **9** for the deployable WAR (uses pre-Jakarta `javax.servlet` API)
- Java EE EAR packaging for a **compatible full Java EE application server** (Tomcat cannot deploy EARs)

## What's implemented

| Area | Legacy behavior |
|---|---|
| Authentication | Form login/logout, salted PBKDF2 password hashes, servlet sessions, basic roles |
| Users | Admin creates users, changes their role and disables/reactivates them |
| Cases | Create, list, basic title/case-number search, filter, paginate, edit, soft archive |
| Workflow | `NEW → TRIAGED → IN_PROGRESS → PENDING_APPROVAL → APPROVED/REJECTED → CLOSED`, role permissions, assignments |
| Discussion | Add and display dated case notes |
| Attachments | Attach and download small files (2 MB maximum, DB `bytea`) |
| Audit | Persistent mutation log visible to auditor, supervisor and admin |
| Notifications | In-app alerts for assignments, status changes and notes |
| Reports | Counts by status/priority, overdue count, capped CSV case export |
| Batch | Manual/cron-triggered overdue alerts and optional SMTP sender |
| Data integrity | Hibernate transactions, HQL parameter binding, optimistic version for case detail edits |
| Basic web controls | JSP output escaping, CSRF on state-changing actions, user revalidation, file download as attachment |

### Reserved for your Strangler Fig implementation

See `docs/MODERNIZATION_BACKLOG.md`: Angular search, richer case dashboards, granular authorization, external identity/SSO, configurable approvals, document storage migration, richer reminders/escalation, integration APIs, accessible UX and deeper integration testing. Those features are **not** implemented here.

## Package structure

```
legacy-enterprise-case-management/
├── build.xml                  # Ant bootstrap, dependency retrieval, compile, WAR and EAR
├── ivy.xml                    # Version-pinned libraries, resolved by Ivy
├── ivysettings.xml
├── db/                        # PostgreSQL initialization SQL
├── docs/                      # Architecture + modernization user stories
├── src/main/
│   ├── java/com/example/legacy/
│   │   ├── bootstrap/         # Admin, demo-data and job entry points
│   │   ├── dao/               # Hibernate-only persistence and HQL
│   │   ├── db/                # SessionFactory and transaction lifecycle
│   │   ├── model/             # JavaBeans mapped with XML
│   │   ├── service/           # Workflow + validation/business rules
│   │   ├── util/              # Validation, password hashes, workflow rules
│   │   └── web/               # Struts Actions, ActionForm and servlet filter
│   ├── resources/             # hibernate.cfg.xml and *.hbm.xml
│   ├── webapp/                # JSPs and site.css; no frontend JS framework
│   └── application/           # EAR application.xml
└── src/test/java/             # Dependency-free tests for core pure logic
```

## Quick start (Windows PowerShell)

Prerequisites: JDK 8, Ant 1.10+, PostgreSQL, Tomcat 9. You need Internet access for the first Ivy dependency resolution. Make sure `java -version` points to Java 8 when running Tomcat.

1. Create the DB and user with an **edited password**. In `db/00_create_database.sql`, replace `REPLACE_WITH_SECURE_PASSWORD` (or create the role/database yourself). As postgres admin:

   ```powershell
   psql -U postgres -f db/00_create_database.sql
   ```

2. Apply schema as `legacy_app`:

   ```powershell
   psql -U legacy_app -d legacy_cases -f db/01_schema.sql
   ```

3. Set DB variables before Ant or starting Tomcat. These override the defaults in the legacy Hibernate configuration.

   ```powershell
   $env:DB_URL = 'jdbc:postgresql://localhost:5432/legacy_cases'
   $env:DB_USER = 'legacy_app'
   $env:DB_PASSWORD = 'your-replaced-db-password'
   ```

4. Build and run local tests:

   ```powershell
   ant test-pure
   ant ear
   ```

   The Ant build downloads Apache Ivy once to `tools/ivy-2.5.3.jar`, then resolves the declared dependencies through Ivy. It produces `dist/legacy-case-management.ear` AND `dist/legacy-case-management.war`.

5. Bootstrap an admin from the command line (choose a unique 12+ character password):

   ```powershell
   $env:CASE_ADMIN_USER = 'admin'
   $env:CASE_ADMIN_PASSWORD = 'choose-a-unique-strong-password'
   ant seed-admin
   ```

6. Optional demo users and eight sample cases (shared only for local training):

   ```powershell
   $env:CASE_DEMO_PASSWORD = 'choose-another-strong-password'
   ant seed-demo
   ```

   Login accounts: the admin account above, plus `supervisor`, `worker`, and `auditor` from the demo seed. Seed demo creates new sample cases every time it runs.

7. **Tomcat**: copy `dist/legacy-case-management.war` to Tomcat 9's `webapps/` directory. Ensure the **Tomcat process** also has `DB_URL`, `DB_USER`, `DB_PASSWORD` environment variables (setting them only in a different PowerShell session is insufficient if Tomcat is running as a Windows service). Start Tomcat and visit:

   `http://localhost:8080/legacy-case-management/`

   **EAR**: deploy `dist/legacy-case-management.ear` to a **Java EE 6+ compatible full server**, configured for the matching Java 8 and Java EE / javax APIs. Its declared context root is `/cases`. Full-server compatibility has not been integration tested.

### Manual jobs

```powershell
ant job-overdue  # Create in-app alerts for overdue assigned cases
ant job-email    # Optional SMTP delivery of unsent in-app alerts
```

Optional `SMTP_HOST`, `SMTP_PORT`, `SMTP_FROM`, `SMTP_USER`, `SMTP_PASSWORD` environment variables configure email. Schedule the commands using Windows Task Scheduler or cron as desired; the WAR does not start a scheduler in the application container. The current SMTP sender is intentionally simple legacy-style code, not robust production queue processing.

### Recommended trial workflow

1. Login as `worker` and create a case.
2. Login as `supervisor`; open the case, assign it to `worker`, change `NEW → TRIAGED`.
3. Login as `worker`; advance `TRIAGED → IN_PROGRESS`, add a note, upload a file, then send to `PENDING_APPROVAL`.
4. Login as `supervisor`; choose `APPROVED` (or `REJECTED` to return work), then `CLOSED`.
5. Login as `auditor`; view the change history and reports without modifying data.

## Important limitations / intentional legacy tech debt

- The use of Struts 1 / Hibernate 3.5 carries known unpatched security and compatibility risks. Run it in an isolated development environment.
- **Java 8 target does not guarantee compatibility with all combinations of current Java runtimes or PostgreSQL versions.** Use JDK 8 and validate Hibernate 3.5 against your PostgreSQL server.
- Built-in Hibernate connection pooling is demonstration-only. Production would require proper DataSource pooling, TLS, secrets management, hardening, integration tests and a supported framework.
- Uploaded content is accepted within coarse type/size constraints; it has **no malware scanning**. Store only harmless test files.
- Session authentication is not enterprise SSO. The app has no login throttling, self-service password reset, fine-grained ACL or modern accessibility audit.
- File download permission is broadly application-wide; case-specific document entitlements are a modernization user story.
- Initial tests exercise framework-independent business rules only. No database/servlet integration tests have been executed by this repository.

## Tests

`ant test-pure` compiles and executes Java-only checks for workflow transitions, passwords, CSV guarding and filename normalization without external libraries. Add PostgreSQL-backed DAO/service integration tests and Tomcat smoke tests before relying on the app for live exercises.

## License

The training application source is provided as an example. All external dependencies retain their own upstream licenses; do not repackage or redistribute third-party jars without checking those licenses.
