# Deployment and integration checks

Use Java 8 for this application’s Maven build and runtime. From this application directory, run `..\mvnw.cmd -f ../pom.xml verify` to build and test the WAR and EAR. Import the root `pom.xml` into IntelliJ and reload Maven projects. The `legacy-case-management:war exploded` artifact includes Maven import's source-aware exploded artifact, so IDE updates can compile module classes and copy changed web resources without first packaging a WAR. Follow the [IntelliJ development workflow](../../README.md#intellij) for update policies, Java HotSwap, and redeployment limits. For command-line staging, run `..\mvnw.cmd compile war:exploded`; the output is `target/legacy-case-management`. Servlet and JSP APIs remain provided by the container.

## Tomcat and JNDI

The web application resolves `java:comp/env/jdbc/LegacyCases` when its first database operation opens the shared SessionFactory. Login-page rendering and service/Action construction remain lazy.

1. Initialize the application database using `db/01_schema.sql` as its owner.
2. Copy the PostgreSQL driver from the WAR’s `WEB-INF/lib/postgresql-42.2.27.jar` to `CATALINA_BASE/lib` so Tomcat's DataSource factory can load it.
3. Copy `config/tomcat/legacy-cases.xml.example` to `CATALINA_BASE/conf/Catalina/localhost/legacy-case-management.xml`. Set the database URL and credentials in that external deployment file. Keep its context filename aligned with the deployed WAR/context name.
4. Deploy `target/legacy-case-management.war` and restart Tomcat after installing the driver or changing its Context resource configuration.

The template configures a bounded DBCP DataSource, connection validation, and a finite borrow timeout. Tomcat owns this pool and its shutdown; undeployment closes only the application's SessionFactory. Avoid setting `DB_URL` in the Tomcat process when using JNDI, because an explicit JDBC override selects standalone mode.

Operational records use the JUL category `com.example.legacy.observability.Telemetry`. Request completions and transaction durations share `X-Request-ID` and identify the actual legacy implementation. Exception messages and causes are excluded. Administrators can use `operations.do` for request lookup and exports; `health/live` and `health/ready` provide minimal monitoring probes. See [MOD-002 operations](../../docs/MOD-002-OPERATIONS.md) for collection, security, retention and comparison setup. Persisted audit events remain separate.

## Standalone CLI operation

CLI commands use an explicit JDBC URL and a bounded application-owned pool:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk8'
$env:DB_URL = 'jdbc:postgresql://localhost:5432/legacy_cases'
$env:DB_USER = 'legacy_app'
$env:DB_PASSWORD = '<local database password>'
$env:CASE_ADMIN_USER = 'admin'
$env:CASE_ADMIN_PASSWORD = '<initial account password, at least 12 characters>'
..\mvnw.cmd compile exec:java '-Dexec.mainClass=com.example.legacy.bootstrap.SeedAdmin'
```

The same connection settings apply to `seed-demo`, `job-overdue`, and `job-email`. CLI finally blocks close the SessionFactory and its owned pool. Service methods never open an independent transaction for a nested audit or notification write. Email delivery remains best-effort and is not atomic with SMTP.

## JUnit 4 tests

Run the default suites on Java 8:

```powershell
..\mvnw.cmd verify
```

Run them independently with `..\mvnw.cmd test` and `..\mvnw.cmd -DskipUnitTests verify`. Unit classes are packaged under `com.example.legacy.unit` with the `Test` suffix; integration classes are under `com.example.legacy.integration` with the `IT` suffix. Maven writes separate JUnit XML reports to `target/surefire-reports` and `target/failsafe-reports`. Shared fixtures compile with test sources and are not discovered as tests.

The integration target needs no external database. All six DAO integration classes use an isolated H2 database with real production Hibernate mappings. Checked-in schema/seed scripts under `src/test/resources/h2` populate known data before every test. Writes roll back afterward; each DAO class's pool and in-memory database are closed after its tests. Test dependencies and resources remain outside WAR/EAR packaging.

The optional PostgreSQL service suite preserves native constraint and concurrent-update checks:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk8'
$env:IT_DB_URL = 'jdbc:postgresql://localhost:5432/legacy_cases_test'
$env:IT_DB_USER = 'legacy_test'
$env:IT_DB_PASSWORD = '<test database password>'
..\mvnw.cmd -Ppostgresql verify
```

`PostgreSqlServiceIntegrationTest` creates its own `enterprise_it_<UUID>` schema for each JUnit test and executes the real SQL bootstrap inside it. It checks aggregate/attachment round trips, native database constraints, rollback after an audit write fails, and concurrent service updates that both read the same version. Exactly one update and its audit event must commit. A separate assertion verifies stale submitted-version rejection.

JUnit teardown closes each test-owned factory/pool and drops only its generated schema. Existing application schemas and tables are not reset. Missing PostgreSQL connection settings fail the optional target rather than silently skipping tests. Expected constraint/optimistic-lock errors may appear in Hibernate's captured test output even when their assertions pass.
