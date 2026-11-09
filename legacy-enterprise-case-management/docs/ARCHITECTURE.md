# Architecture

```text
Browser: Tiles layouts + JSP/JSTL, HTML forms, .do navigation
                    |
       Tomcat 9 / Struts 1 ActionServlet
                    |
    RequestLoggingFilter + SecurityFilter
           Action + ActionForm       presentation / HTTP adapters
                    | getData() / setData()
           service.dto transport values
                    |
     CaseService -> DefaultCaseService
       AuthenticationService / BackOfficeService
       AccountAccessService / AuditService
       NotificationService / DocumentService
                   CasePolicyService     application services / business rules
                    |
        service.mapper DTO/entity mappers
                    |
      AccountDao / CaseDao / CaseNoteDao
      CaseDocumentDao / AuditDao / NoticeDao   persistence
                    |
       DatabaseUtil + Hibernate 3.5
          SessionFactory / Session
                    |
          XML .hbm.xml mappings
                    |
                PostgreSQL

CLI seeders / job entry points
          -> BootstrapService / BatchJobService -> persistence
```

The application remains a Java 8, Struts 1, Hibernate 3.5 WAR deployed on Tomcat 9, with Apache Tiles composing its JSP views. A Java EE EAR can wrap the WAR for a compatible full application server. The existing Struts routes, submitted HTML field names, database schema, and Hibernate XML mappings are preserved. Every route binds an explicit request-scoped ActionForm, and JSPs read response DTOs through that form.

## Layer responsibilities

### Presentation layer

Action classes route requests and cast the ActionForm populated by Struts. They call operation-specific form methods such as `getCreateData()`, `getUpdateData()`, and `getSearchData()` to build immutable input DTOs, pass those DTOs and the session-derived actor to a service, then assemble view DTOs and call `form.setData(...)` before rendering a JSP or serializing a response. `getData()` remains available for form/view snapshots and round trips. Actions never call `getParameter()` for page input and never receive Hibernate entities. They own response headers, attachment streaming, and CSV serialization. They do not open transactions, invoke DAOs, depend on Hibernate, or decide business permissions.

`WebUtil` contains HTTP/session helpers, date formatting/parsing, CSRF token generation and validation, and navigation/error handling. `FormDataUtil` parses identifiers, versions, and pagination from already-bound form fields; services still enforce record existence, permissions, and optimistic concurrency. `SecurityFilter` handles authentication redirects, HTTP security headers, CSRF protection, and POST-only mutations. It calls `AccountAccessService.activeAccount` to refresh account state without accessing persistence. `AuthAction` manages session replacement and logout; username normalization and password verification belong to `AuthenticationService`. Multipart tokens are checked in `DocumentAction`, since Struts binds those form fields after the servlet filter runs. Multipart files are released in a `finally` block on success and failure.

`ApplicationListener` only manages application lifecycle and calls `DatabaseUtil.closeSessionFactory` during undeployment; this lifecycle hook is the web package's sole direct database-utility dependency.

### Tiles view composition

The application uses the Struts 1 Tiles integration supplied by `org.apache.struts:struts-tiles:1.3.10`. Maven resolves and packages it with the other runtime dependencies. Servlet/JSP APIs are provided by Tomcat and are available to compilation and framework tests without being bundled in the WAR. `TilesPlugin` loads `/WEB-INF/tiles-defs.xml`, validates it against the bundled Tiles 1.3 DTD, and creates the default module's definitions factory. The controller explicitly uses `org.apache.struts.tiles.TilesRequestProcessor` for definition-aware action and validation forwards. This wiring follows the [Apache Struts Tiles integration](https://svn.apache.org/repos/asf/struts/struts1/tags/STRUTS_1_2_9/legacy/api-1.1/org/apache/struts/tiles/package-summary.html).

`layout.main` defines `/WEB-INF/views/layouts/main.jsp` and shared header, message-summary, and footer attributes. Eleven `page.*` definitions inherit the layout and supply a title message key and body JSP: login, dashboard, cases, case creation/detail, users, audit, reports, notifications, upload errors, and errors. The layout imports `titleKey`, resolves it with `bean:message`, and owns the document, stylesheet, and main content container. Each component is a separately translated JSP with UTF-8/tag-library directives from `fragments/taglibs.jspf`; body JSPs contain only page content. `fragments/header.jsp` renders role-dependent navigation, `fragments/messages.jsp` renders `<html:errors/>` and escaped feedback, and `fragments/footer.jsp` renders the shared footer. Tiles inserts these components without flushing the response.

Actions return logical view definitions through `WebUtil.view(PageView)`, such as `PageView.CASE_VIEW`, and retain request-scoped forms/DTOs for the body JSP. Browser redirects continue targeting `.do` routes. The controller keeps `inputForward="false"`: mapping inputs are either definition names, resolved by Tiles, or `.do` input actions that reload display data. A rejected multipart upload goes directly to `page.upload-errors`, so Tiles composes the page on the current request without invoking another Action or reparsing the multipart body. Service validation error forwards use the same destinations and preserve the Struts error request attribute. CSV/download responses continue streaming from Actions.

Servlet-container 403/500 dispatches bypass the Struts processor. Their `web.xml` locations point to `/WEB-INF/views/error-page.jsp`, which explicitly inserts `page.error`; `error.jsp` remains the body fragment. JSPs, definitions, and layouts stay under `WEB-INF`. Maven WAR packaging includes the definitions and Tiles JAR; the IntelliJ exploded artifact includes them as well.


### Shared codes, constants, and messages

`domain.Role`, `CaseStatus`, and `Priority` define the supported business codes. Workflow, authorization, service validation, and form choices reference these enums. DTOs and XML-mapped entities continue carrying their existing String codes through `name()`, preserving stored values and submitted fields. `AuditEntity` and `AuditAction` type the internal audit-writing API; conversion to stored strings happens when the audit DTO is built. `ReportGrouping` restricts DAO grouping to supported mapped properties, and overdue queries bind `CaseStatus.CLOSED` as a parameter.

Presentation adapters use `PageView` for Tiles definitions and `ActionOperation` for configured Struts operation parameters. `Routes`, `WebKeys`, `FormFields`, and `HttpConstants` hold navigation, request/session, validation-field, and HTTP contract values. `EnvironmentKeys`, `ContentTypes`, and `CaseDefaults` hold deployment settings, attachment media types, and creation defaults. Technology-specific names such as crypto algorithms, Hibernate settings, SMTP properties, and seed account identifiers remain private constants in their owning classes. `JobType` defines accepted CLI jobs.

`ApplicationResources.properties` owns page titles, navigation, headings, labels, help/empty-state text, date display patterns, CSV headings, validation and business messages, confirmations, and audit/notification templates. Tiles definitions contain `titleKey` resource identifiers. JSPs render text through `bean:message`; Java message identifiers are centralized in `MessageKeys`. `MessagesUtil` uses standard Java ResourceBundle/MessageFormat, keeping servlet dependencies out of services. Durable audit/notification messages retain English formatting. Expected service failures carry keys and arguments; Actions resolve field-label arguments and messages in the presentation locale, then escape string arguments. Confirmations and CSV headings also use the presentation locale. Resource values use Java-8-compatible Unicode escapes. HTML field values and business-error text retain their existing escaping.

The stateless application-scoped `ViewOptionsUtil` exposes fresh enum-option arrays, defaults, role codes, route paths, and HTTP/session keys to JSP EL. Thus dropdowns and role checks use the same definitions as service rules without embedding comma-separated code lists or Java scriptlets. View choices remain presentation hints; services still enforce permissions independently. Framework declarations (Struts/Tiles definition names, resource paths, XML property names, form binding names, HQL syntax, and tag attributes) remain explicit in their configuration files. Contract tests check their correspondence with Java symbols and message keys.

### Manual service wiring

The application uses a conventional legacy Struts Action-to-service-to-DAO arrangement with explicit construction (manual dependency wiring). Each Action declares its service collaborators as `private final` instance fields and initializes them once inside its public no-argument constructor, for example `this(new DefaultCaseService());`, delegating to an injection constructor that assigns the final interface reference. Every Action also accepts its service collaborators explicitly for tests. The `execute` method invokes those service instances. The filter follows the same approach for account revalidation. CLI entry points create local service instances for their invocation.

Services construct and retain any service collaborators as final instance fields. Each service declares the mappers it uses as private final instance fields and initializes them explicitly inside its constructor, for example `this.accountMapper = new AccountMapper();`. All mapper operations are instance methods; mapper constructors perform no persistence work and mapper objects retain no DTO/entity/session state. `CaseService` is an interface implemented by `DefaultCaseService`. Its full constructor accepts the transaction template, DAOs, mappers, access/policy collaborators, and audit/notification services. Other database services expose equivalent constructor seams. Stateless DAOs/mappers and collaborator services allow focused test subclasses; fields retained by services remain final. The dependency graph has no construction cycles. Service constructors perform no database work.

Struts 1 can reuse one Action instance for concurrent requests. Services therefore retain only immutable collaborator references; actors, forms, DTOs, Hibernate sessions, transactions, and other request/job data remain method-local. DAOs are stateless final service collaborators initialized in service constructors. Every DAO operation receives the current method-local session explicitly; no DAO retains a Session or Transaction. Actions never hold DAOs. No dependency-injection container or service locator is required.

### Form and DTO request/response flow

```text
Request parameters -> Struts ActionForm population
                   -> Action: form.getCreateData()/getUpdateData()/getSearchData()
                   -> Service: authorize, validate, map DTO to entity
                   -> DAO/Hibernate: persist or query
                   -> Service: map entity to DTO inside transaction
                   -> Action: assemble page DTO, form.setData(pageDto)
                   -> JSP/CSV/download reads the form's DTO data
```

| Routes/pages | Request-scoped form | Transport data |
|---|---|---|
| Login, authentication, logout | `authForm` / `AuthForm` | `AuthDto` and credential-free `AccountDto` |
| Case list/search, new/detail, and all case mutations | `caseForm` / `CaseForm` | `CasePageDto`, `CaseDto`, `CaseDetails` |
| User list/create/role/enable actions | `userForm` / `UserForm` | `UserPageDto`, `AccountDto` |
| Dashboard | `dashboardForm` / `ReportForm` | `ReportPageDto`, `CaseSummary` |
| Reports and CSV export | `reportForm` / `ReportForm` | `ReportPageDto`, `CaseSummary`, `CaseDto` rows |
| Audit history/pagination | `auditForm` / `AuditForm` | `AuditPageDto`, `AuditEventDto` |
| Notifications and mark-read | `notificationForm` / `NotificationForm` | `NotificationPageDto`, `NoticeDto` |
| Multipart uploads and downloads | `uploadForm` / `UploadForm` | `DocumentPageDto`, `DocumentDto` |

`BasePageForm` binds shared record/pagination/CSRF fields; CSRF tokens stay out of service DTOs, and actors always come from the authenticated session. Forms retain submitted string fields for Struts binding and parse typed values in the relevant operation mapping methods. `setData()` formats dates and versions for controls and exposes returned collections to the views. Request scope prevents one tab or route from reusing another request's bound fields. Passwords are input-only and are cleared from forms after processing; `AccountDto` has no password-hash property. The existing native HTML forms submit the same field names to `.do` mappings, where Struts performs ActionForm population; adopting ActionForms does not require replacing the HTML controls with tag-library controls.

### JSP submission validation and error presentation

| Layer | Validation responsibility |
|---|---|
| ValidatorForm | Validate user input from JSP submissions: required controls, display lengths, select values, email/date formats, identifier/version syntax, pagination, and declared upload metadata |
| Action | Process requests, map forms/DTOs, call services, and prepare the input page with validation errors and submitted values |
| Service | Enforce business rules, current permissions, ownership, workflow, optimistic version comparison, account availability/uniqueness checks, and transaction boundaries |
| DAO | Perform Hibernate persistence using the service-supplied Session; no form or business validation |
| Database | Enforce primary keys, NOT NULL/length restrictions, username/case-number uniqueness, and existing foreign-key referential integrity constraints |

Every Struts action mapping enables `validate="true"` and declares an `input` destination. `BasePageForm` extends Struts `ValidatorForm`, so all concrete page forms participate in Commons Validator. `ApplicationValidatorPlugIn` extends Struts ValidatorPlugIn and loads the standard rules packaged with Struts 1.3.10 and the application rules in `WEB-INF/validation.xml`. Basic required, length, username-mask, email, date, and numeric/version rules are declarative and use the existing message bundle. Custom `validate` methods call the framework validation first, then add shared ID/pagination syntax, offered-select-value, and multipart metadata checks; password clearing and safe input-forward handling remain in Java. The overridden `getValidationKey()` selects XML rule sets by form class and mapped operation (for example `CaseForm.create` and `CaseForm.update`), allowing one form bean to serve multiple routes without applying creation rules to every request. Display-only operations have explicit empty rule sets, and tests verify that every normal action mapping has a rule set. Validation is operation-specific: initial GET pages are not rejected for empty create controls, notes/assignment do not require case-edit fields, and role changes do not require a new-user password. Forms never query services or DAOs to validate submissions. The service layer retains independent enforcement for callers outside JSP submissions.

Struts runs form validation before `Action.execute`. On failure, its RequestProcessor saves errors under the standard Struts request key, rolls back multipart resources when present, and forwards to the configured input without executing the mutation Action. `ValidationInputAction` implements read-only input destinations for views that need fresh display data: case details/notes/accounts, user lists, reports, and other list pages. Those input routes skip repeated form checks to avoid forwarding loops; they never retry the mutation. When a record ID cannot safely load a detail page, the error page presents the original validation messages instead.

Case update redisplay reloads server-owned display information while retaining the submitted title, description, priority, due date, and optimistic-lock token. Other failed case commands reload normal edit controls while retaining their assignment/status/note controls. Account redisplay preserves nonsecret input and refreshes the account list. Password fields are cleared when validation fails and are never populated into HTML values. Upload validation checks metadata without materializing file bytes; rejected multipart requests are cleaned up by Struts, and valid requests still undergo the service's actual-byte validation. Upload errors forward directly to the `page.upload-errors` Tiles definition, whose body retains the case ID and displays a new file control. They must not forward through a second Struts Action: Struts would otherwise attempt to parse the consumed multipart body again after rollback. The browser must reselect a rejected upload.

`src/main/resources/ApplicationResources.properties` is configured as the default Struts message bundle. The shared Tiles message component declares the Struts HTML tag library and renders `<html:errors/>`, so every input/error page displays the same accessible error summary. Known business correction messages from case/user/document services are saved by the Action and forwarded to the input page; permission and unexpected failures use safe message keys on the error page. Business-message text is HTML-escaped before passing it to the Struts error renderer. Login credential failures also use the bundle. Client-provided form strings continue to be escaped with JSTL `c:out` in controls and views.

The existing PostgreSQL schema remains the final data-integrity boundary: for example, the service checks whether a username is available, while the unique constraint protects against concurrent inserts. A form only checks username syntax; it does not attempt a uniqueness lookup. Case version syntax is a form concern; comparing that token with the stored version remains a service/Hibernate concern. A select value may be syntactically valid while its transition or caller is forbidden by business policy.

### Immutable DTOs and builders

All transport values in `service.dto` are read-only snapshots: instance APIs expose getters only, fields are private and final, concrete DTO classes are final, and constructors are nonpublic. Creation uses `Type.builder()...build()`. To derive another value, use `Type.builder(existing)...build()`; copying never modifies the existing DTO. The abstract `PageDto` owns final identity/pagination values and supplies a typed common builder for its final concrete page DTOs. This keeps inherited builder methods fluent without exposing setters on DTOs.

```java
NoticeDto original = NoticeDto.builder()
        .recipient("worker")
        .subject("Case assigned")
        .read(false)
        .build();
NoticeDto readNotice = NoticeDto.builder(original).read(true).build();
```

Dates and binary payloads are copied when supplied to builders, captured at construction, and returned from getters. Lists and sets are defensive, unmodifiable snapshots, preserving list order and workflow-set insertion order. Their elements are immutable DTOs or immutable scalar values. Builder reuse cannot change a previously built value; null collection inputs become empty read-only collections. DTO builders assemble transport data rather than applying authorization, persistence, or business validation.

ValidatorForms remain mutable for Struts field binding. Each `getData()` assembles a fresh immutable DTO with builders, and `setData()` populates the form's own display controls from getters. Mappers build immutable DTOs from Hibernate entities and use getter values when applying permitted changes to mutable entities. Services create new state snapshots for controlled entity updates and return new response DTOs instead of mutating request DTOs. Actions consume those returned values. Services still own transaction boundaries and business checks; persistent entities retain the mutable JavaBean accessors required by Hibernate XML mappings.

Authentication/account input credentials remain request-scoped. Response builders omit passwords, and forms store redacted immutable snapshots while clearing their mutable password controls independently. A service cannot clear an immutable caller's request in place. Account DTOs continue to omit password hashes entirely.

### ValidatorForm pagination compatibility

`ValidatorForm` reserves its integer `page` property for multi-step validation. List pagination is therefore bound to the separate string property `pageNumber`, while service DTOs retain their integer `page` field. Case/audit JSP links now submit `pageNumber`. Existing `.do?page=...` URLs remain supported: `BasePageForm.reset()` captures the legacy query value before Struts populates the bean, preferring `pageNumber` when both are supplied. Reset does not inspect multipart parameters before the upload handler parses them.

These forms use a single validation stage. `BasePageForm.validate()` always sets the framework page to zero before invoking Commons Validator, so a submitted legacy `page` parameter cannot select a later stage and skip required checks. Read-only input forwards normalize pagination after rebinding to keep malformed query values out of JSP arithmetic; original validation errors and correctable edit/version fields are retained. `getData()`/`setData()` continue converting pagination between the form and DTO without changing service or persistence APIs.

### Service layer

DTO field validation lives in `service.validation`, independently of controllers, mappers, and persistence. Each input/write DTO has a stateless `DtoValidator<T>` implementation whose `validate(dto)` method returns an immutable normalized copy (or the original immutable DTO when normalization is unnecessary). Services use that result before mapping or opening a transaction. Invalid fields fail fast through the existing `BusinessValidationException` message-key contract. Required fields, lengths, enum selections, pagination, optional email syntax, upload bounds, and submitted version presence/range are checked here. Optional typed dates remain optional; no new due-date policy is imposed.

Permissions, database lookups, uniqueness, workflow rules, and comparing the submitted version with persisted state remain service/policy responsibilities inside transactions. The upload validator delegates to the nested document validator; it derives the actual content size and excludes client-supplied server-owned metadata. Audit and notification validators check assembled write DTOs before persistence. Login validation preserves passwords and accepts legacy password lengths while discarding a caller-supplied account response. Presentation/page DTOs and read responses are not revalidated as commands. Form validation remains for field-level HTTP feedback, and mappers only translate validated values.

MVC-facing service methods accept DTOs and the trusted session actor rather than servlet requests, ActionForms, Struts files, or JSP objects. Entity/session helpers remain internal to the service package. Query results are mapped to DTOs inside the service transaction before its commit, so no managed entity or credential hash crosses into a form, action, session adapter, or JSP. Each database-backed use case delegates its transaction lifecycle to `service.transaction.HibernateTransactionTemplate`. Its callback validates the active actor and required permissions, invokes DAOs with the supplied session, and applies mutations and their audit/notification side effects in that same transaction. Actions do not supply an open Hibernate session.

The template opens a fresh Session and begins a Transaction for each invocation, commits successful work (including null results), and closes the session in finally. RuntimeException and Error failures during work or commit trigger rollback; Hibernate StaleStateException failures are translated to ConcurrentUpdateException while retaining the Hibernate cause; rollback and close failures are suppressed on the original failure without replacing it. Begin failures still close the opened session, and standalone close failures propagate. The template retains only its optional factory reference; sessions and transactions are method-local. `execute(Work<T>)` returns callback results after commit and cleanup, while `executeWithoutResult(Action)` handles writes without dummy return values. Services create a private final template inside their constructors:

```java
this.transactionTemplate = new HibernateTransactionTemplate(sessionFactory);
```

Service operations express their complete unit of work through callbacks; DTO mapping happens before the session closes:

```java
return transactionTemplate.execute(session -> {
    accountAccessService.requireAdministratorPermission(accountAccessService.requireActiveAccount(session, actor));
    return accountMapper.mapToDtos(accountDao.findAllAccounts(session));
});
```

Write callbacks use the same supplied session for entity changes, audit events, and notifications. They do not open/close sessions or begin/commit/roll back transactions themselves. This is a local template implementation using the existing Hibernate APIs; it does not require Spring or change the Action/service/DAO boundaries.

| Service | Responsibilities |
|---|---|
| `CaseService` / `DefaultCaseService` | Interface and implementation for case search/detail, creation, updates, assignment, transitions, notes, and archival |
| `AuthenticationService` | Username normalization, password verification, and credential-free authentication responses |
| `AccountAccessService` | Current account lookup and reusable account-level authorization |
| `AuditService` | Audit writes on the caller-supplied session and authorized audit history |
| `NotificationService` | Notice creation on the caller-supplied session, recipient queries, and mark-read commands |
| `CasePolicyService` | Case editing and workflow permissions, including worker ownership and creator self-approval rules; shared by mutations and detail-screen capability flags |
| `DocumentService` | Attachment size/media-type validation, filename normalization, case authorization, transactional upload/audit, and authorized downloads |
| `BackOfficeService` | Administrator account commands, dashboard/report summaries, reporting authorization, and the 5,000-case export limit |
| `BootstrapService` | Validated initial administrator provisioning and demo accounts/cases for explicit CLI seed commands |
| `BatchJobService` | Explicit overdue-notification and best-effort SMTP delivery jobs |

`service.dto.CaseDetails` carries `CaseDto`, `CaseNoteDto`, attachment metadata `DocumentDto`, safe `AccountDto` values, and computed edit/comment/workflow capabilities. `CaseSummary` uses named `CountDto` rows rather than positional Hibernate query arrays. `CasePageDto`, `UserPageDto`, `ReportPageDto`, `AuditPageDto`, `NotificationPageDto`, and `DocumentPageDto` hold presentation snapshots assembled by Actions. Case services accept `CreateCaseDto`, `UpdateCaseDto`, `AssignCaseDto`, `TransitionCaseDto`, `AddCaseCommentDto`, `CaseSearchCriteriaDto`, and `RecordReferenceDto`; account commands use `CreateAccountDto`/`ChangeAccountRoleDto`, uploads use `UploadDocumentDto`, and audit pagination uses `PageRequestDto`. Services return DTO records, details, summaries, or collections independently of page DTOs. `AuthDto` carries credentials on input and omits them from authentication responses. Forms expose these returned DTO graphs directly to the JSPs; DTOs contain no persistence objects or framework dependencies. Capability flags guide rendering, while mutation services independently enforce the rules on every call.

Reporting authorization is enforced inside the service against the current enabled account. Any active role can load dashboard totals; only ADMIN, SUPERVISOR, and AUDITOR can load reports, audit history, or exports. Export selection and its limit belong to `BackOfficeService`; CSV escaping and HTTP output stay in `ReportAction`.

Attachment validation enforces 1 byte through 2 MB and the existing PDF, JPEG, PNG, text, and generic-binary media-type allowlist. Struts caps multipart requests at 3 MB. `UploadForm.getData()` copies file metadata and bytes to a `DocumentPageDto`; `DocumentService` validates both declared size and actual bytes against the stricter 2 MB business limit before storage. Struts `FormFile` objects stay in the HTTP layer and are released even when validation fails. Uploads and case updates use the same case-edit policy. Downloads require an active account and a nonarchived parent case.

### Application failures and operational logs

`ApplicationException` retains a message key and defensive argument array. `BusinessValidationException` represents correctable inputs, `AccessDeniedException` represents authorization failure, `RecordNotFoundException` represents unavailable records, and `ConcurrentUpdateException` represents submitted-version or Hibernate optimistic-lock conflicts. `MessageArgument` marks nested resource keys such as field labels. Actions forward business/concurrency corrections to the configured input page; other expected failures use the error view. Generic IllegalArgumentException is treated as an unexpected programming failure rather than exposing its text as validation feedback.

`RequestLoggingFilter` runs before security on every original request. It creates a trusted server-generated request ID and returns it as `X-Request-ID`. `BasePageForm` records only a parsed numeric record ID or an invalid marker. `RequestLoggingUtil` logs unexpected failures once with their original cause, request ID, servlet operation, and safe record ID; it does not add submitted bodies, credentials, or query strings. `WebUtil.fail` uses the same log-once marker and a generic browser message. Expected application corrections do not produce error-level operational logs. Business audit rows remain separate from JUL operational logging.

`ValidationLimits` is the shared source for field lengths, password minimum, pagination/export bounds, and attachment size. `ContentTypes.supportedAttachment` is shared by form and business validation. JSP controls read named limits through `ViewOptionsUtil`. `validation.xml` uses symbolic `limit.*` variables; `ApplicationValidatorPlugIn` resolves those values and message arguments before publishing validator resources. XML field labels reference the application message bundle. Constructor-time initialization is complete before requests access these resources.

### Persistence layer

DAOs own native Hibernate Session/HQL queries, pagination, and persistence operations. They have no-argument constructors and accept a Session as the first argument of every operation, such as `accountDao.save(session, account)` or `auditDao.save(session, event)`. They never open, close, begin, commit, or roll back sessions/transactions, reference HTTP objects, or enforce role/business policies. Related data and audit/notification operations share one service-owned session and transaction. No EntityManager or JPA APIs are used. Plain model beans are mapped to tables with `.hbm.xml` files.

The six `.hbm.xml` files mirror the column constraints in `db/01_schema.sql`: explicit required/nullable properties, exact VARCHAR lengths, unique username/case-number values, timestamp/boolean/version schema defaults, and PostgreSQL TEXT/BYTEA types. Primary-key and sequence-generator mappings retain their existing identity behavior. In particular, the case description stays unbounded TEXT at the database layer, and attachment bytes stay BYTEA; JSP/service input limits remain separate business/presentation checks.

Mapping defaults describe generated schema DDL; they do not initialize Java entity properties or replace service initialization. Length and unique metadata likewise do not replace PostgreSQL enforcement. The application uses `hbm2ddl.auto=validate`, so updating mapping metadata does not alter an existing database. Initialize database constraints with the SQL schema script. Foreign keys on note/document `case_id` remain SQL-owned because those properties are scalar identifiers rather than mapped entity associations; SQL also owns the schema's indexes. Hibernate-generated DDL is therefore not a replacement for the full bootstrap schema.

`DatabaseUtil` owns lazy SessionFactory configuration and shutdown. Web deployments resolve the container-managed `java:comp/env/jdbc/LegacyCases` DataSource declared in `web.xml`; credentials, pool limits, and validation belong in the external Tomcat Context configuration. An explicit `DB_URL` override selects `LocalConnectionProvider`, a bounded DBCP pool for CLI/test operation; `DB_USER` and `DB_PASSWORD` supply its credentials. This does not use Hibernate's built-in connection pool. Closing the factory closes the owned standalone pool; the container owns JNDI pool shutdown. See [DEPLOYMENT.md](DEPLOYMENT.md). Session and transaction lifecycles belong to the service layer through `HibernateTransactionTemplate`; DatabaseUtil and DAOs do not manage use-case transactions. Service methods return mapped DTO values after their session closes; there is no open-session-in-view dependency. `service.mapper` classes perform explicit, reflection-free conversion. Creation mappers omit generated IDs, lock versions, credentials, ownership, and lifecycle fields unless that mapper is handling trusted service-generated audit/notification data. Update mappers modify only the operation-specific fields on the existing managed entity. Services validate inputs and assign server-owned state before mapping; DAOs persist those mapped entities. Mutable dates and document bytes are copied at mapper boundaries. Attachment lists omit binary payloads entirely.


Database-backed services accept an optional SessionFactory through an overloaded constructor; their public no-argument constructors remain compatible with the Actions' manual wiring. Each constructor supplies that factory to its own transaction template and passes it to service collaborators as well. When no factory is supplied, the template resolves the shared application factory lazily before opening each session, so constructing an Action, service, or template does not connect to the database. Templates, DAOs, and mappers are reusable collaborators; sessions and transactions are never shared between requests.

`CasePolicyService` remains a pure policy service without a session factory. `AuditService.record` and `NotificationService.create` always use their supplied session and never start nested transactions. Internal helpers that accept an existing session (for example active-account lookup, audit writing, and notification creation) participate in their caller's transaction instead of opening a nested one. Explicit bootstrap/demo/job invocations retain their existing units of work: demo account provisioning is separate from each case creation/assignment, and email delivery has a bounded selection transaction followed by a separate transaction for each pending notice. SMTP delivery remains best-effort and is not atomic with the database commit.

## Naming and shared state

- Java sources follow Google Java Style. Class state declarations retain their initialization order; constructors and public/protected operations precede package-private operations, private helpers, and nested types. Overloads stay together. Run the repository's `scripts/Check-JavaStyle.ps1` for formatter and Google Checkstyle validation.
- Application service class names end in `Service`. Services have public no-argument constructors and instance methods. Actions initialize the services they use inside public no-argument constructors and retain them as private final instance fields.
- General-purpose shared helpers end in `Util`: `DatabaseUtil`, `WebUtil`, `WorkflowUtil`, `PasswordsUtil`, `StringsUtil`, and `DatesUtil`.
- General utilities retain static helper methods. Services and DTO/entity mappers are ordinary stateless objects with public constructors and instance methods, not static facades or singleton instances. The shared Hibernate SessionFactory is held internally by `DatabaseUtil`. Framework-managed Actions, filters, and listeners retain their framework role names.
- DAOs end in `Dao`, transport beans end in `Dto`, forms end in `Form`, and mapping classes end in `Mapper`. Aggregate/detail DTOs retain their descriptive `CaseSummary` and `CaseDetails` names. Persistent model beans retain their existing mapping names. Bootstrap classes retain their CLI entry-point names.
- All Java classes and methods are documented, and every `if` body and standalone `else` body uses curly braces. Conventional `else if` chains are retained.

## Tests and validation

All executable tests use JUnit 4.13.2. Unit tests live in `com.example.legacy.unit` and end in `Test`; integration tests live in `com.example.legacy.integration` and its subpackages and end in `IntegrationTest`. Shared Mockito servlet fixtures live in `com.example.legacy.support`. Fixture helpers are not executable test classes. There are no main-method test runners or custom assertion engines.

| Target | Scope | Reports |
|---|---|---|
| `..\mvnw.cmd test` | Unit tests with Mockito 4.11.0 collaborators where isolation is useful | `target/surefire-reports/TEST-*.xml` |
| `..\mvnw.cmd -DskipUnitTests verify` | Actual Struts/Tiles/configuration integration and seeded H2 DAO tests | `target/failsafe-reports/TEST-*.xml` |
| `..\mvnw.cmd verify` | Both default suites, without an external database | Both directories above |
| `..\mvnw.cmd -f ../pom.xml -Pfunctional verify` | Separate Selenium page-object module, embedded Tomcat and seeded H2 | `../functional-tests/target/failsafe-reports` |
| `..\mvnw.cmd -Ppostgresql verify` | Optional PostgreSQL service, native constraint, rollback, and optimistic-lock tests | `target/failsafe-reports/TEST-*.xml` |

Maven compiles test sources into `target/test-classes`. Surefire runs `*Test` unit classes while excluding `*IntegrationTest`; Failsafe runs `*IntegrationTest` integration classes during `verify`. PostgreSQL is excluded unless `-Ppostgresql` is selected. Test-scoped JUnit, Mockito, and H2 dependencies are excluded from deployment archives. Mockito 4 retains Java 8 compatibility.

Unit tests cover workflow/password/output helpers, immutable DTOs, form/DTO conversion, mapper field whitelists, role policies, case/account/audit orchestration, authentication response redaction, transaction commit/rollback/cleanup, Action operation mapping, escaped errors, and request correlation/log-once behavior. Mockito replaces hand-written Hibernate, service, and servlet proxies. Argument capture, call-order verification, and explicit exception assertions check observable behavior. JUnit creates independent test instances and provides named results per test method.

Framework integration tests initialize the actual Validator and Tiles plug-ins, compare route/form/operation/message contracts, exercise validation forwarding/multipart rollback, and compile Hibernate mapping metadata against the SQL constraints. Their servlet container boundaries use Mockito fixtures while the integrated production framework components remain real.

### Seeded H2 DAO integration

`integration.dao` contains `AccountDaoIntegrationTest`, `CaseDaoIntegrationTest`, `CaseNoteDaoIntegrationTest`, `CaseDocumentDaoIntegrationTest`, `AuditDaoIntegrationTest`, and `NoticeDaoIntegrationTest`. Every class uses its own named in-memory H2 database and the actual Hibernate mapping files. `src/test/resources/h2/schema.sql` preserves the production column constraints, TEXT/BYTEA types, foreign keys, and explicit sequence names. H2 runs in [PostgreSQL compatibility mode](https://h2database.github.io/html/features.html#compatibility); a test-only `H2LegacyDialect` adapts Hibernate 3's numeric boolean literals to SQL TRUE/FALSE accepted by H2 2.x. Production mappings and dialect settings are unchanged.

JUnit `@BeforeClass` creates and validates the private schema. `@Before` reloads `h2/test-data.sql` before each test, resetting identities and populating accounts, cases, notes, document bytes, audit events, and notifications. Each test receives a fresh Hibernate session/transaction. `@After` rolls back writes and closes the session; `@AfterClass` closes the pool and shuts down the in-memory database. DAO tests execute real SQL for filtering, pagination/order, grouping, overdue selection, generated identities/versions, binary payload round trips, recipient scoping, uniqueness, and foreign-key enforcement. Flush/clear operations prevent cache-only round-trip assertions.

### Functional end-to-end module

The sibling [`functional-tests`](../../functional-tests/README.md) module is an
optional Maven reactor module. Its JUnit 4 `*IntegrationTest` tests use Selenium page objects
against the real WAR, embedded Tomcat on a loopback random port, and a unique H2
in-memory database bound under `jdbc/LegacyCases`. Each test reloads deterministic
seed data and starts a fresh browser profile. ChromeDriver explicitly launches a
matching, pinned portable Chrome for Testing binary downloaded to the module build
cache; installed browsers and Selenium Manager are not used.

The test JVM holds Selenium/Tomcat/H2; application/Hibernate libraries stay in the
WAR's webapp classloader. Maven stages only a test H2 configuration and boolean-literal
dialect into the exploded test WAR, retaining all production mappings and schema
validation. It clears production database environment overrides, closes browsers
and the application/database on completion, and captures failure screenshots,
HTML and driver logs. Production archives exclude all functional dependencies and
fixtures. Run `..\mvnw.cmd -f ../pom.xml -Pfunctional verify` separately from the default fast suites. See
[SMOKE_TESTS.md](SMOKE_TESTS.md) for browser coverage and remaining deployment checks.

### Optional PostgreSQL integration

`integration.service.PostgreSqlServiceIntegrationTest` uses JUnit `@Before`/`@After` to create, seed, and dispose an independent `enterprise_it_<UUID>` schema for every test. Configure `IT_DB_URL`, `IT_DB_USER`, and optional `IT_DB_PASSWORD`, then run `..\mvnw.cmd -Ppostgresql verify`. Missing settings fail this explicitly selected profile. The suite retains production mapping/BYTEA round trips, native SQLSTATE checks, atomic case/audit rollback after injected persistence failure, concurrent SQL version conflicts, and stale submitted-form checks. The pool closes before only the generated schema is dropped. See [DEPLOYMENT.md](DEPLOYMENT.md) for commands.

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
