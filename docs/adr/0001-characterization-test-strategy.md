# ADR-0001: Characterize legacy behavior through a shared test contract

- Status: Accepted for the legacy harness; replacement verification pending.
- Story: MOD-001
- Recorded: 2026-11-09

## Context

MOD-001 requires repeatable evidence that migration preserves login, creation, assignment,
processing, approval, closure, audit and attachments. Existing browser tests exercise web flows
but do not specify exact cross-implementation persisted outputs. The legacy application remains
Java 8/Struts/Hibernate; the replacement will use Java 21/Spring/Angular.

## Options considered

1. Extend browser assertions alone. This covers HTTP behavior but couples detailed comparisons
   to page layouts and makes persisted state harder to inspect.
2. Use isolated unit tests with mocked persistence. This is fast but does not verify real
   Hibernate mappings, transactions, or committed audit and attachment state.
3. Use shared scenarios with implementation-specific adapters. This exercises real services and
   persistence while defining a contract the replacement can independently implement.

## Decision

Choose option 3. Sharing business implementation code risks reproducing bugs in expected results.
Instead, keep declarative inputs and reviewed
expected outputs in a separate `characterization-tests` module, with a dependency-free Java 8
adapter contract/runner usable from either runtime. The legacy adapter uses real services,
transactions and production mappings; browser/security checks stay in `functional-tests`.

H2 provides portable default execution; PostgreSQL uses production DDL in a generated schema.
Each adapter owns only its fixture database/schema and its own runtime. One implementation never
writes into another implementation's fixture. Generated identities are normalized, while status,
ownership, audit order/content and document bytes remain observable business contracts.

## Consequences

- The legacy suite can run locally without a database server using H2.
- PostgreSQL execution checks the same scenarios against production DDL, but requires a
  dedicated test database account with schema creation permission.
- A separate Java 21 replacement harness can consume the packaged fixtures, baselines, and
  runner without loading the legacy Hibernate runtime.
- Baselines must be reviewed independently of actual results; they are never regenerated
  automatically to make tests pass.
- Passing the legacy suite establishes a comparison baseline. It does not meet the backlog's
  two-implementation acceptance criterion until an independent replacement passes as well.

## Test strategy

Run five independent scenarios, taking a full durable snapshot after every operation. Compare
to cumulative declarative baseline patches, including after rejected mutations. Test the
comparator itself for unexpected audit fields, ownership differences, incomplete baselines and
cleanup after setup failures. Archive JUnit results, snapshots and field-level diffs in CI.
Run PostgreSQL and existing browser coverage before accepting migrated capabilities. Enable
`characterization.requireReplacement` when enforcing two-implementation acceptance.

From the repository root with JDK 8 configured:

```powershell
.\mvnw.cmd -Pcharacterization -pl characterization-tests -am verify
.\mvnw.cmd '-Pcharacterization,functional' verify
```

Require a registered replacement adapter in the Java 8 harness with:

```powershell
.\mvnw.cmd -Pcharacterization -pl characterization-tests -am '-Dcharacterization.requireReplacement=true' verify
```

This gate must fail while no replacement adapter exists. For a Java 21-only replacement,
run an independent harness against the identical packaged fixtures and baselines instead.
See the [module README](../../characterization-tests/README.md) for PostgreSQL setup
and the [requirements review](../../characterization-tests/docs/MOD-001-verification.md)
for verified coverage and remaining acceptance work.

## Rollback and ownership

The technical lead owns baseline review; implementation owners supply their adapters. Intentional
behavior changes need an explicit contract decision and reviewed baseline diff. A failed parity
gate blocks migration of that capability; runtime routing rollback remains MOD-003's responsibility.
Disable the optional Maven profile to omit the harness. No legacy application business logic,
schema, routes or deployment configuration changes are required. The only legacy build change
attaches its existing classes as a test-consumable artifact.

If the build integration must be rolled back, revert the MOD-001 change and verify the
previous build and test suites. Runtime request routing is unaffected by disabling this
test harness. Adapter cleanup closes its own Hibernate factory and database connection and
shuts down its H2 database or drops only its generated PostgreSQL schema.
