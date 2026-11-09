# Portable functional tests

This optional Maven module runs JUnit 4 Selenium page-object tests against the
production WAR in embedded Tomcat 9 with a private, seeded H2 database.

## Run

From the repository root, use a JDK 8 `JAVA_HOME`:

```powershell
.\mvnw.cmd -Pfunctional verify
```

Maven builds the application WAR, compiles browser tests and the dialect fixture,
unpacks the WAR into `functional-tests/target/webapp`, overlays the test-only H2
dialect/configuration, and provisions portable Chrome and ChromeDriver. Failsafe
then runs the `*IntegrationTest` tests. Production WAR/EAR configuration remains PostgreSQL.

The matching browser versions are pinned in `pom.xml`. Override them together:

```powershell
.\mvnw.cmd -Pfunctional '-Dchrome.version=157.0.8103.38' verify
```

Binaries are cached in `functional-tests/build/browser` and survive `mvn clean`.
No installed browser, Tomcat, database server, or fixed HTTP port is required.
Maven resolves Java dependencies; Ant and Ivy are not used. Selenium 4.13.0 is
retained for Java 8 compatibility.

Linux containers running as root can explicitly opt out of Chrome's sandbox
with `-Dchrome.noSandbox=true`. Hosts still need Chrome's normal OS/shared libraries.
Portable archive names support Windows x64, Linux x64/ARM64, and macOS Intel/ARM64.
Use a complete release from the official
[Chrome for Testing dashboard](https://googlechromelabs.github.io/chrome-for-testing/).

## Isolation

- `src/test/java/.../pages` owns locators, submissions, and explicit waits;
  `CaseManagementIntegrationTest` owns behavioral assertions.
- `EmbeddedApplication` binds Tomcat to loopback on a random available port and
  creates a unique H2 database for each run. Hibernate validates production mappings.
- `BrowserSession` resets fixtures and starts a fresh browser profile for each test.
- `src/test/resources/h2` supplies five users and 35 cases, plus related records.
  Enabled users `admin`, `requireSupervisorPermission`, `worker`, and `auditor` share the test-only
  password `FunctionalPassword!2026`; `disabled` cannot authenticate.
- `src/fixture` is staged only into the test WAR. Hibernate is needed to compile
  the dialect but excluded from the browser JVM; it loads inside the WAR classloader.
- Failsafe clears `DB_URL`, `DB_USER`, and `DB_PASSWORD` in the forked browser JVM.
  Direct IDE runs with a nonempty `DB_URL` fail before deployment.

Browser sessions, Tomcat, Hibernate, connection pools, and H2 shut down after tests,
including failures. Direct IDE test execution requires staging and browser
provisioning first; prefer the Maven `functional` profile.

## IntelliJ and reports

Import the root POM, activate the `functional` profile, and reload Maven projects.
Dependencies and test/fixture source roots are imported from the POM. The legacy
manually maintained `.iml` libraries have been removed.

JUnit reports are in `functional-tests/target/failsafe-reports`. ChromeDriver logs,
downloads, screenshots, HTML, and failure URLs are in
`functional-tests/target/artifacts/<test-method>`. `clean` removes these generated
files and the staged WAR while retaining the browser cache.

See the application's [smoke-test checklist](../legacy-enterprise-case-management/docs/SMOKE_TESTS.md)
for coverage and the remaining PostgreSQL and full Java EE deployment checks.
