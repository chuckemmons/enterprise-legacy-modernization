# Legacy Enterprise Case Management (Struts 1 + Hibernate 3.5)

An **educational, intentionally legacy** Java EE web application designed as the starting point of a Strangler Fig modernization. Its business flow is **case opened → supervisor triage/assignment → worker processing → submitted for approval → supervisor approval/rejection → closure**. It uses classic Struts 1 Action classes, ActionForms, JSP/JSTL composed with Apache Tiles, a service layer, Hibernate native `Session` APIs and explicit DAO classes. **No REST API, JPA, annotations, or Spring** is used in application code.

> **Security and compatibility:** Struts 1.3.10 and Hibernate 3.5.6-Final are end-of-life. This code is for a private local development lab, NOT an Internet-facing or production service. Do not place sensitive data in it. A complete compile and Tomcat smoke test are required in your environment; the source package is not precompiled.

## Technology

- Java **8** (use a JDK 8 runtime for the old libraries)
- Apache Struts **1.3.10** with `.do` actions and Apache Tiles 1.3.10 JSP/JSTL layouts
- Hibernate **3.5.6-Final** (native `Session`, `Transaction`, HQL, XML `.hbm.xml` mappings)
- PostgreSQL **12+**, JDBC driver 42.2.27
- Maven **3.9.9**, bootstrapped by the checked-in wrapper; dependencies come from Maven Central
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
├── pom.xml                    # Maven WAR build and pinned dependencies
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
└── src/test/java/             # Pure, service/framework, and real PostgreSQL tests
```

## Quick start (Windows PowerShell)

Prerequisites: JDK 8, PostgreSQL, Tomcat 9. Run these commands from this application directory. The repository root contains the Maven wrapper; its first run needs Internet access. Make sure `java -version` points to Java 8 when running Tomcat.

1. Create the DB and user with an **edited password**. In `db/00_create_database.sql`, replace `REPLACE_WITH_SECURE_PASSWORD` (or create the role/database yourself). As postgres admin:

   ```powershell
   psql -U postgres -f db/00_create_database.sql
   ```

2. Apply schema as `legacy_app`:

   ```powershell
   psql -U legacy_app -d legacy_cases -f db/01_schema.sql
   ```

3. Set JDBC variables for standalone Maven/CLI commands. These select a bounded application-owned pool:

   ```powershell
   $env:DB_URL = 'jdbc:postgresql://localhost:5432/legacy_cases'
   $env:DB_USER = 'legacy_app'
   $env:DB_PASSWORD = 'your-replaced-db-password'
   ```

4. Build and run local tests:

   ```powershell
   ..\mvnw.cmd -f ../pom.xml clean verify
   ```

   Maven resolves the pinned dependencies and uses Surefire for unit tests and Failsafe for integration tests. It produces `../legacy-enterprise-case-management-ear/target/legacy-case-management.ear` AND `target/legacy-case-management.war`.

5. Bootstrap an admin from the command line (choose a unique 12+ character password):

   ```powershell
   $env:CASE_ADMIN_USER = 'admin'
   $env:CASE_ADMIN_PASSWORD = 'choose-a-unique-strong-password'
   ..\mvnw.cmd compile exec:java '-Dexec.mainClass=com.example.legacy.bootstrap.SeedAdmin'
   ```

6. Optional demo users and eight sample cases (shared only for local training):

   ```powershell
   $env:CASE_DEMO_PASSWORD = 'choose-another-strong-password'
   ..\mvnw.cmd compile exec:java '-Dexec.mainClass=com.example.legacy.bootstrap.SeedDemo'
   ```

   Login accounts: the admin account above, plus `requireSupervisorPermission`, `worker`, and `auditor` from the demo seed. Seed demo creates new sample cases every time it runs.

7. **Tomcat**: configure the external `jdbc/LegacyCases` pooled DataSource using [DEPLOYMENT.md](docs/DEPLOYMENT.md) and `config/tomcat/legacy-cases.xml.example`. Install the PostgreSQL driver in Tomcat's `lib`, then deploy `target/legacy-case-management.war`. The web application uses JNDI by default; clear `DB_URL` in the Tomcat process when using the container pool. Start Tomcat and visit:

   `http://localhost:8080/legacy-case-management/`

   **EAR**: deploy `../legacy-enterprise-case-management-ear/target/legacy-case-management.ear` to a **Java EE 6+ compatible full server**, configured for the matching Java 8 and Java EE / javax APIs. Its declared context root is `/cases`. Full-server compatibility has not been integration tested.

### Manual jobs

```powershell
..\mvnw.cmd compile exec:java '-Dexec.mainClass=com.example.legacy.bootstrap.RunJobs' '-Dexec.args=overdue'  # Create in-app alerts for overdue assigned cases
..\mvnw.cmd compile exec:java '-Dexec.mainClass=com.example.legacy.bootstrap.RunJobs' '-Dexec.args=email'    # Optional SMTP delivery of unsent in-app alerts
```

Optional `SMTP_HOST`, `SMTP_PORT`, `SMTP_FROM`, `SMTP_USER`, `SMTP_PASSWORD` environment variables configure email. Schedule the commands using Windows Task Scheduler or cron as desired; the WAR does not start a scheduler in the application container. The current SMTP sender is intentionally simple legacy-style code, not robust production queue processing.

### Recommended trial workflow

1. Login as `worker` and create a case.
2. Login as `requireSupervisorPermission`; open the case, assign it to `worker`, change `NEW → TRIAGED`.
3. Login as `worker`; advance `TRIAGED → IN_PROGRESS`, add a note, upload a file, then send to `PENDING_APPROVAL`.
4. Login as `requireSupervisorPermission`; choose `APPROVED` (or `REJECTED` to return work), then `CLOSED`.
5. Login as `auditor`; view the change history and reports without modifying data.

## Important limitations / intentional legacy tech debt

- The use of Struts 1 / Hibernate 3.5 carries known unpatched security and compatibility risks. Run it in an isolated development environment.
- **Java 8 target does not guarantee compatibility with all combinations of current Java runtimes or PostgreSQL versions.** Use JDK 8 and validate Hibernate 3.5 against your PostgreSQL server.
- Built-in Hibernate connection pooling is demonstration-only. Production would require proper DataSource pooling, TLS, secrets management, hardening, integration tests and a supported framework.
- Uploaded content is accepted within coarse type/size constraints; it has **no malware scanning**. Store only harmless test files.
- Session authentication is not enterprise SSO. The app has no login throttling, self-service password reset, fine-grained ACL or modern accessibility audit.
- File download permission is broadly application-wide; case-specific document entitlements are a modernization user story.
- Portable H2 and embedded Tomcat browser tests supplement the optional PostgreSQL integration checks; EAR deployment still requires validation on the intended full Java EE server.

## Tests

`..\mvnw.cmd verify` runs JUnit 4 unit tests and seeded H2/framework integration tests. `..\mvnw.cmd -Ppostgresql verify` runs the optional native PostgreSQL suite.

Run `..\mvnw.cmd -f ../pom.xml -Pfunctional verify` for the separate [`functional-tests`](../functional-tests/README.md) Selenium page-object module. It uses embedded Tomcat, seeded H2, and a downloaded, pinned portable Chrome/ChromeDriver pair. It requires JDK 8 and the Maven wrapper, with no installed browser, servlet container or database server. See [browser coverage and deployment checks](docs/SMOKE_TESTS.md).

## License

The training application source is provided as an example. All external dependencies retain their own upstream licenses; do not repackage or redistribute third-party jars without checking those licenses.

See [deployment and integration checks](docs/DEPLOYMENT.md) for JNDI setup, request-correlated operational logging, and `..\mvnw.cmd -DskipUnitTests verify` using seeded H2 databases, plus optional `..\mvnw.cmd -Ppostgresql verify` checks using generated PostgreSQL schemas.

All tests use JUnit 4: `*Test` unit classes run in the test phase and `*IntegrationTest` integration classes run in integration-test/verify. Run `..\mvnw.cmd test` or `..\mvnw.cmd -DskipUnitTests verify` individually; reports are under `target/surefire-reports and target/failsafe-reports`.
