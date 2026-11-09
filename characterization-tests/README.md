# MOD-001 characterization tests

Design: [ADR-0001](../docs/adr/0001-characterization-test-strategy.md).
Acceptance evidence and outstanding work: [MOD-001 requirements review](docs/MOD-001-verification.md).

## In plain language

This module is a safety net for modernization. It describes how the existing application
behaves so that changes, or a replacement application, can be checked against the same expectations.

For example, a supervisor assigns a case to a worker, the worker processes it, and the supervisor
approves it. The tests check the resulting case status, ownership, audit history and attachments
after each action. They also check that forbidden actions fail without unexpectedly changing
saved data.

The main pieces are:

| Piece | What it does |
| --- | --- |
| Fixture | Supplies test accounts, input data and an ordered list of actions. |
| Adapter | Translates those actions into calls to a particular application's services. |
| Snapshot | Describes the action's result and the saved state afterward as named fields. |
| Baseline | Contains the reviewed, checked-in expected results. |
| Runner | Executes each action and compares its snapshot with the baseline. |

```text
Fixture actions -> Application adapter -> Actual snapshot
                                                |
                                   Compare with baseline
                                                |
                                    Pass or report differences
```

Passing means the tested implementation matched the expectations for these scenarios. The
repository currently supplies only the legacy adapter, so a passing default run verifies legacy
behavior. A replacement still needs its own adapter and successful run against the same baselines.

## How the code works

[CharacterizationIntegrationTest.java](src/test/java/com/example/characterization/CharacterizationIntegrationTest.java)
coordinates the tests. The supporting classes execute the actions and compare the results.

1. **Tell JUnit to repeat the test with different inputs.**
   `@RunWith(Parameterized.class)` makes JUnit 4 run the test method for every combination
   returned by `scenarios()`. `@Parameterized.Parameters(name = "{0}/{1}")` names each run
   using its adapter class and scenario.

2. **Choose the implementations to test.**
   `scenarios()` always adds `LegacyServiceAdapter.class`. It then uses
   `ServiceLoader.load(ImplementationAdapter.class)` to discover additional registered adapters.
   `List<Class<? extends ImplementationAdapter>>` holds adapter classes that implement the shared
   interface; these classes are used later to create fresh adapter objects.
   If `characterization.requireReplacement=true` and no additional adapter is registered,
   setup fails so a legacy-only run cannot satisfy the replacement requirement.

3. **Pair each implementation with the five scenarios.**
   The nested loops add an `Object[] {adapter, name}` for each combination of adapter and
   `login`, `lifecycle`, `rework`, `permissions` or `self-approval`.
   One adapter produces five test cases; two adapters produce ten. Each scenario contains
   multiple actions. The constructor stores the adapter class and scenario for one test case.

4. **Load the inputs for that test case.**
   `preservesBusinessBehavior()` loads `/fixtures/accounts.properties`, then merges in
   `/fixtures/<scenario>.properties` with `putAll()`. Scenario values override shared values
   with the same key. It also loads `/baselines/<scenario>.properties` as the expectations.

5. **Create the adapter and run the scenario.**
   `adapter.getDeclaredConstructor().newInstance()` creates a fresh adapter through its
   no-argument constructor. The test passes it, the fixture, the baseline and a report path to
   `ScenarioRunner.run(...)`. Reports go under `target/characterization/<Adapter>/` by default;
   `characterization.reports` can override the parent directory.

[ImplementationAdapter.java](src/main/java/com/example/characterization/ImplementationAdapter.java)
defines three operations: `initialize()` prepares isolated test state, `execute()` runs a
command and returns a snapshot, and `close()` releases resources.

[LegacyServiceAdapter.java](src/test/java/com/example/characterization/LegacyServiceAdapter.java)
implements that interface using real legacy services and Hibernate transactions. It creates
an isolated database environment, seeds the test accounts, translates commands into service
calls and reads back saved state. Generated identifiers are represented consistently, such
as `case-1`, so different implementations can be compared. Expected business exceptions become
result codes; database and programming failures still fail the test.

[ScenarioRunner.java](src/main/java/com/example/characterization/ScenarioRunner.java) runs each
numbered command and checks its snapshot. For example, the login fixture contains:

```properties
01.command=LOGIN|worker|invalid
```

This means "try to log in as worker with an invalid password." The baseline expects:

```properties
01.result=INVALID_CREDENTIALS
```

Expected fields carry forward between steps unless the next baseline step changes them.
If a failed operation should leave ownership and documents unchanged, those expectations remain
active. Missing fields, unexpected fields and differing values all cause failures. The runner
writes expected and actual snapshots and a difference report, and uses try-with-resources to
close the adapter even if initialization or execution fails.

## Technical scope

This optional Maven module characterizes real legacy application services through Hibernate
and committed database transactions. Every scenario gets its own H2 database or PostgreSQL
schema. It uses the production classes artifact, mappings, messages and service implementations;
no business service is mocked or copied. The WAR build attaches a classes JAR for this purpose.

The existing [functional-tests](../functional-tests/README.md) module remains responsible for
browser login/logout, sessions, CSRF, JSP forms and multipart HTTP handling. Run both suites for
the complete application boundary. This module adds exact persisted behavioral comparisons.

## Run

From the repository root, with a JDK 8 `JAVA_HOME`:

```powershell
.\mvnw.cmd -Pcharacterization -pl characterization-tests -am verify
```

To also run the existing browser suite:

```powershell
.\mvnw.cmd -Pcharacterization,functional verify
```

For the same characterization scenarios against PostgreSQL, supply a dedicated test database
account with CREATE SCHEMA permission:

```powershell
$env:IT_DB_URL='jdbc:postgresql://localhost:5432/legacy_test'
$env:IT_DB_USER='legacy_test'
$env:IT_DB_PASSWORD='your-test-password'
.\mvnw.cmd -Pcharacterization,characterization-postgresql -pl characterization-tests -am verify
```

PostgreSQL mode fails if connection settings are missing; it does not silently skip tests.
Only a generated `characterization_<uuid>` schema is created and dropped. Production `DB_*`
variables are never read by this adapter. Default H2 mode requires no external services.

## Scenarios and expectations

Shared UTF-8 `.properties` resources are packaged in the module JAR, so a separate Java 21
replacement harness can consume the identical fixtures, baselines and runner:

- `fixtures/accounts.properties`: synthetic users/roles, case fields, attachment bytes.
- `fixtures/login.properties`: valid, invalid and disabled-account authentication.
- `fixtures/lifecycle.properties`: creation, assignment, processing, upload/download, approval,
  closure, download after closure, rejected closed-case mutations.
- `fixtures/rework.properties`: rejection, rework, resubmission, approval and closure.
- `fixtures/permissions.properties`: read-only creation/edit/upload, unauthorized assignment,
  unassigned-worker transition, stale update, disallowed upload type and worker approval.
- `fixtures/self-approval.properties`: creator supervisor cannot approve their own case.
- `baselines/*.properties`: checked-in expected output, independent of adapter execution.

Each `NN.command` is `OPERATION|actor|argument`; `steps` specifies the ordered step count.
Each baseline step is a patch to the previous **full expected snapshot**. Every step must
explicitly declare `NN.result`. Unchanged ownership, documents and audit fields remain expected
after failed operations. The comparator checks the union of expected and actual fields, so
missing and unexpected events/fields fail. Baselines are never automatically overwritten.
Review business intent and diffs before changing a baseline; acceptance changes need an ADR.

Snapshots compare status, creator, assignee, department, closure, title, counts, every audit
action/actor/entity/detail, and attachment name/type/size/uploader/SHA-256. Attachments are also
downloaded and compared byte-for-byte to the fixture. Audit events are read in insertion order;
timestamps must exist, fall within the execution interval and preserve causal order. Database
IDs/case numbers map to `case-1`; exact clock times, password hashes, session tokens and version
counter values are excluded. Stale-version behavior is exercised separately.

## Reports

JUnit results: `target/surefire-reports` and `target/failsafe-reports`.
Behavior artifacts: `target/characterization/<Adapter>/`, with expected/actual snapshots for
each step and `<scenario>-diff.txt` containing field-level mismatches (empty on success).
Archive these directories in CI and fail the job on Maven's exit code. Infrastructure failures
remain test errors, rather than being converted to successful business rejections.

## Replacement adapter

Implement `com.example.characterization.ImplementationAdapter` with a public no-argument
constructor. `initialize` must provision isolated state and seed the supplied accounts;
`execute` must execute the command as the specified trusted actor and return the same normalized
snapshot contract; `close` must release state even after partial initialization. Do not reuse
legacy business rules to implement the replacement or derive expected output from actual output.

For a replacement reachable from this test JVM, add its adapter dependency in this module and
register its class in `META-INF/services/com.example.characterization.ImplementationAdapter`.
The integration runner executes every scenario separately against legacy and each registered
adapter, using the same baseline. Enable the acceptance gate with:

```powershell
.\mvnw.cmd -Pcharacterization -pl characterization-tests -am '-Dcharacterization.requireReplacement=true' verify
```

This deliberately fails until a replacement adapter is registered. For a Java 21-only adapter,
use a separate Java 21 test harness depending on this module JAR and call `ScenarioRunner.run`
with these packaged fixtures/baselines. Matching the same contract establishes equivalence
without loading Hibernate 3 into the replacement runtime.

The current repository supplies only the legacy implementation. A green default run certifies
legacy baseline behavior; it does not claim that a replacement has passed MOD-001 acceptance.
