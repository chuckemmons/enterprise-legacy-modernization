# Functional browser tests and deployment smoke checks

Run `..\mvnw.cmd -f ../pom.xml -Pfunctional verify` from the application directory
using JDK 8. This includes the sibling [`functional-tests`](../../functional-tests/README.md) module.
This runs Selenium/ChromeDriver against the application WAR in embedded Tomcat 9
with a private, prepopulated H2 database. Both the pinned portable Chrome for
Testing binary and its matching driver are downloaded to the module build cache;
an installed browser, Tomcat or PostgreSQL is unnecessary.

## Automated browser coverage

`com.example.legacy.functional.CaseManagementIntegrationTest` contains independent JUnit 4 tests
with fresh browser sessions and reset fixture data before every test:

| Test | Behavior exercised |
|---|---|
| `showsLoginErrorsAndProtectsPagesAfterLogout` | Anonymous redirect, invalid password, successful login and logout/session invalidation |
| `rejectsLoginForDisabledAccounts` | Disabled seeded account rejected at login |
| `createsCaseWithNoteAndDocumentAsWorker` | Create case, add note, multipart upload, byte-exact attachment download and visibility after another login |
| `showsValidationErrorsForBlankTitle` | Server ValidatorForm errors rendered in JSP, preserving submitted description |
| `blocksMutationWithoutCsrfToken` | Tokenless browser POST is rejected and case remains unchanged |
| `notifiesWorkerAfterSupervisorAssignment` | Assignment, recipient notification and mark-read action |
| `supportsRejectionReworkAndClosure` | NEW → TRIAGED → IN_PROGRESS → PENDING_APPROVAL, rejection/rework, approval and closure with separate roles |
| `rejectsWorkerApprovalAndCreatorSelfApproval` | Tampered workflow requests rejected; another user's assigned case has no worker edit/transition controls |
| `rejectsStaleEditWithoutOverwritingCase` | Two preloaded tabs submit conflicting versions; stale edit cannot overwrite saved title |
| `filtersAndPaginatesSeededCases` | Distinct result pages over 34 active cases, combined filters and empty search results |
| `allowsAuditorToReadReportsAndExportSafeCsv` | Read-only controls, report/audit rendering, downloaded CSV formula-prefix escaping |
| `createsUsableAccountAsAdministrator` | Administrator creates an account that can authenticate using its persisted hash |
| `appliesAccountChangesToExistingSessions` | Independent admin/worker browsers; role change and account disable affect existing session |

JUnit reports are in `functional-tests/target/failsafe-reports`. Failure screenshots,
HTML, URLs, driver logs and downloaded files are under
`functional-tests/target/artifacts/<test-method>`. Page objects own selectors and
explicit navigation waits; test methods own assertions. The suite exercises real
Struts, Tiles, JSP, service, Hibernate and JNDI components, without mocked web requests.

The seeded H2 schema mirrors `db/01_schema.sql`; production Hibernate mappings
are validated unchanged. H2 compatibility does not replace PostgreSQL-native
constraint/locking tests (`..\mvnw.cmd -Ppostgresql verify`). Browser dependencies and test
configuration are never included in distributable application archives.

## Remaining deployment smoke checks

These checks require the intended deployment environment and remain manual:

- [ ] Build both distributable WAR and EAR using `..\mvnw.cmd -f ../pom.xml package`.
- [ ] Deploy the unmodified WAR on the target Java 8/Tomcat 9 installation;
      initialize Hibernate and validate the actual PostgreSQL schema/JNDI pool.
- [ ] Repeat the important role/workflow/file flows against that PostgreSQL deployment.
- [ ] Run `..\mvnw.cmd compile exec:java '-Dexec.mainClass=com.example.legacy.bootstrap.RunJobs' '-Dexec.args=overdue'` for a past-due assigned case and verify the resulting
      in-app notification. The browser suite checks assignment notices; it does
      not launch the external job scheduler or SMTP delivery.
- [ ] Exercise coarse upload limits with oversized/disallowed content as needed.
- [ ] Validate EAR deployment with a full Java EE server; Tomcat supports the WAR only.

This checklist describes coverage and runnable checks. Consult the generated
reports for the result of a particular execution.
