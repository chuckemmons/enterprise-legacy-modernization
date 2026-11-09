# Enterprise legacy modernization

## Build

Use a **JDK 8** `JAVA_HOME` for building, testing, and running this legacy application.
The Maven build and tests also support **JDK 21**. Test dependencies retain Java 8
compatibility; on modular JDKs, Maven opens the JDK packages needed by the test
environment-variable fixtures in the forked test JVMs.
The checked-in Maven wrapper downloads Maven 3.9.9 on first use; no Maven or Ant
installation is required. Dependencies are resolved from Maven Central.

From this repository's root in PowerShell:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk8'
.\mvnw.cmd clean verify
```

On Linux/macOS use `sh ./mvnw` with a JDK 8 `JAVA_HOME`.

This runs the unit and H2/framework integration tests and produces:

- `legacy-enterprise-case-management/target/legacy-case-management.war`
- `legacy-enterprise-case-management-ear/target/legacy-case-management.ear`

The EAR embeds the WAR with the existing `/cases` context root. Tomcat uses the WAR.
Servlet/JSP APIs are provided by the container; test libraries are excluded from
both deployment artifacts. Production library versions match the previous build.

| Command from repository root | Behavior |
| --- | --- |
| `.\mvnw.cmd test` | Unit tests only |
| `.\mvnw.cmd verify` | Unit and H2/framework integration tests, WAR and EAR |
| `.\mvnw.cmd -Pfunctional verify` | Also stage the WAR and run Selenium with portable Chrome |
| `.\mvnw.cmd -Ppostgresql verify` | Also run PostgreSQL tests; requires `IT_DB_URL`, `IT_DB_USER`, optional `IT_DB_PASSWORD` |
| `.\mvnw.cmd -DskipTests package` | Build deployment artifacts without running tests |

Reports are in the application's `target/surefire-reports` and
`target/failsafe-reports`. Browser reports and failure artifacts are in
`functional-tests/target`; downloaded browsers stay in `functional-tests/build/browser`
so `clean` does not repeatedly download them. PostgreSQL and browser tests are opt-in.

Unit tests mirror the production packages under
`legacy-enterprise-case-management/src/test/java/com/example/legacy/unit/`:
for example, `unit.service`, `unit.service.mapper`, `unit.service.validation`,
`unit.web`, and `unit.db`. Shared fixtures live in `unit.support` or `support`.
They isolate database, servlet, SMTP, and command-line dependencies.

To measure unit coverage without running integration tests or retaining earlier
coverage data, run:

```powershell
.\mvnw.cmd -pl legacy-enterprise-case-management clean test org.jacoco:jacoco-maven-plugin:report
```

`verify` checks that both overall line and branch coverage are at least 81%,
which keeps SonarQube's combined coverage above 80%. No production classes are
excluded from coverage. The XML and HTML reports are under the application's
`target/site/jacoco/` directory.

## Command-line jobs

From `legacy-enterprise-case-management`, configure the existing database/seed/SMTP
environment variables as described in the [application guide](legacy-enterprise-case-management/README.md):

```powershell
..\mvnw.cmd compile exec:java '-Dexec.mainClass=com.example.legacy.bootstrap.SeedAdmin'
..\mvnw.cmd compile exec:java '-Dexec.mainClass=com.example.legacy.bootstrap.SeedDemo'
..\mvnw.cmd compile exec:java '-Dexec.mainClass=com.example.legacy.bootstrap.RunJobs' '-Dexec.args=overdue'
..\mvnw.cmd compile exec:java '-Dexec.mainClass=com.example.legacy.bootstrap.RunJobs' '-Dexec.args=email'
```

## IntelliJ

Import the root `pom.xml` as a Maven project (or choose **Add as Maven Project**),
then reload Maven projects. Select JDK 8 or JDK 21 for the Maven runner and importer.
Enable the `functional` Maven profile to import the optional Selenium module.
Maven import regenerates module definitions and dependencies; the obsolete
`main`/`test` module definitions and Ivy library directories have been removed.
After pulling this cleanup into an open IDE, reload Maven projects (or reopen the
project) to discard the old modules from memory.

The existing `legacy-case-management:war exploded` IntelliJ artifact includes
Maven import's `legacy-enterprise-case-management:war exploded` artifact. It builds
from the web facet, compiled module output, and Maven runtime libraries, rather
than copying the last packaged webapp. Its existing deployment directory remains
`out/artifacts/legacy_case_management_war_exploded`. Reload Maven projects after
opening the project so the referenced artifact and web facet are available.

In **Run | Edit Configurations | Tomcat 9**:

1. Use **JDK 8** as the server JRE and keep the existing database/JNDI settings.
2. Deploy `legacy-case-management:war exploded`; keep the application's existing
   context path (`/legacy_case_management` in the current local configuration).
3. Keep **Build** and **Build Artifacts: legacy-case-management:war exploded**
   under **Before launch**. A Maven `package` step is not needed for IDE deployment.
4. Set **On 'Update' action** to **Update classes and resources**, and
   **On frame deactivation** to **Update classes and resources**. Leave **Show
   dialog** enabled so you can choose **Redeploy** for changes that need it.
5. Start Tomcat with **Debug** for Java HotSwap. Save edits, then switch to the
   browser or press **Ctrl+F10** to update the application. Refresh the browser
   to see updated JSP/CSS/JavaScript. This updates deployment files; it does not
   automatically refresh the browser page.

| Change | Development update |
| --- | --- |
| JSPs (including `audit.jsp`), CSS, JavaScript, images | Update resources and refresh the browser; Tomcat recompiles changed JSPs |
| Existing Java method bodies | Update classes and resources in **Debug**; standard JVM HotSwap can reload supported edits |
| New/removed fields or methods, class hierarchy changes | **Redeploy**; standard JVM HotSwap cannot apply these changes |
| Struts/Tiles/Validator configuration, Hibernate mappings, cached message bundles | **Redeploy** to reload application configuration and caches |
| Maven dependencies | Reload Maven projects, then **Redeploy** |
| Container/JNDI configuration or server libraries | Restart Tomcat |

In ordinary **Run** mode, updating class files does not replace classes already
loaded by the JVM; use **Redeploy** for Java changes. Avoid `clean` while Tomcat
is using a generated deployment directory. Rebuild/redeploy after renaming or
deleting files so stale output is removed. See JetBrains' [application update
policies](https://www.jetbrains.com/help/idea/updating-applications-on-application-servers.html).

For an exploded webapp built entirely from the command line, without assembling
a WAR or EAR, run:

```powershell
.\mvnw.cmd -pl legacy-enterprise-case-management compile war:exploded
```

Its location is `legacy-enterprise-case-management/target/legacy-case-management`.
Run the same command again after edits to refresh it. This command does not watch
files or trigger server-side class reloading. Point a separately managed Tomcat
Context's `docBase` at that directory if using this workflow; the IntelliJ
deployment above instead uses its `out/artifacts` directory. Maven documents the
[exploded goal](https://maven.apache.org/plugins/maven-war-plugin/exploded-mojo.html).

## Java style

Java sources follow the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).
Within classes, keep state declarations in initialization order, followed by
constructors, public/protected operations, package-private operations, private
helpers, and nested types. Keep overloads together. This is the project's choice
of logical member ordering; Google does not prescribe one universal order.

Run formatting and Google Checkstyle checks from the repository root:

```powershell
.\scripts\Check-JavaStyle.ps1 -JavaHome 'C:\path\to\jdk21-or-newer'
```

Add `-Format` to apply google-java-format before checking. The script pins
google-java-format 2.0.0 and Checkstyle 10.26.1, checks production code, tests, and
functional fixtures, and downloads missing tools into the ignored `target/style`
directory. The first run needs network access. Warnings fail the check.

Style tools run on a separate modern JDK; application builds remain on Java 8.
Automated checks supplement review of documentation, initialization order, and
other rules requiring semantic judgment.

## SonarQube

Use the opt-in `sonar` Maven profile to build, test, install, and scan the reactor
in one command. Run Maven with JDK 21 (`JAVA_HOME` must point to that JDK); the
application still compiles to Java 8 bytecode. Set `SONAR_TOKEN` to your local
token, then run from the repository root:

```powershell
.\mvnw.cmd clean install -Psonar
```

The profile pins SonarScanner for Maven to 5.8.0.7211 and binds analysis to
`install`. The scanner defers analysis until the last reactor module, after the
tests and JaCoCo checks have passed. A plain `clean install` does not scan, and
`verify -Psonar` stops before the scan. Avoid parallel Maven builds (`-T`) with
this profile.

Defaults are `http://localhost:9000` and project key
`legacy-enterprise-case-management`. Override them with `-Dsonar.host.url=...`
and `-Dsonar.projectKey=...`. To analyze against Java 8's standard library, add
`"-Dsonar.java.jdkHome=C:\path\to\jdk8"`. Do not commit tokens.

If you need to compile and test on JDK 8, first run `.\mvnw.cmd clean install`
on JDK 8, then switch `JAVA_HOME` to JDK 21 and run
`.\mvnw.cmd -Psonar sonar:sonar "-Dsonar.java.jdkHome=C:\path\to\jdk8"`.

Maven supplies source, bytecode, and dependency paths, so a separate SonarScanner
CLI installation and hand-written `sonar.java.binaries` setting are unnecessary.
The `install` build generates JaCoCo coverage from the unit and application
integration tests. Sonar imports
`legacy-enterprise-case-management/target/site/jacoco/jacoco.xml` automatically;
open `legacy-enterprise-case-management/target/site/jacoco/index.html` to inspect
the same coverage locally. Browser-test coverage is not included in this report.

See [deployment](legacy-enterprise-case-management/docs/DEPLOYMENT.md) and
[functional tests](functional-tests/README.md) for environment-specific setup.
