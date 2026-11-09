# Strangler Fig modernization backlog

The business scenario stays the same. Begin with an operational Struts 1/Hibernate 3.5/JSP baseline; incrementally move capabilities into Java 21, Spring Boot, Spring Security, JPA (new system only), PostgreSQL, and Angular. **The legacy baseline itself has NO REST API or JPA.**

## Phase 0 — Characterization and safety

**MOD-001 — Characterization tests.** As a technical lead, I want a repeatable integration suite covering login, case creation, assignment, processing, approval, closure, audit and attachments so migration preserves business behavior. Acceptance: same input fixture generates equivalent status, ownership and audit output on both implementations.

Legacy baseline and shared comparison contract: [characterization-tests](../characterization-tests/README.md).
Run with `-Pcharacterization`; replacement parity acceptance remains pending a replacement adapter.
Design: [ADR-0001](adr/0001-characterization-test-strategy.md).
Coverage and validation: [MOD-001 requirements review](../characterization-tests/docs/MOD-001-verification.md).

**MOD-002 — Legacy observability.** As an operator, I want request IDs, transaction duration metrics, structured logs and functional checks so I can compare new and old behavior. Acceptance: dashboard traces each migrated request to its responsible implementation; no passwords logged.

**MOD-003 — Reversible proxy routing.** As an architect, I want feature-by-feature routing so individual workflows can be transferred to the new application and rolled back without database corruption. Acceptance: route toggles are documented and tested under both versions.

## Phase 1 — Read-only user experience

**MOD-101 — Angular case search.** As a worker, I want advanced keyword search, saved filters, sortable columns and accessible pagination so I can find my assigned cases quickly. Acceptance: Angular retrieves matching cases from a new Spring Boot endpoint while legacy Struts continues handling writes.

**MOD-102 — Angular case details.** As a supervisor, I want a modern read-only detail page with audit timeline and attachment metadata so I can quickly review case progress. Acceptance: identical record data to legacy JSP for a fixed fixture, consistent authorization.

**MOD-103 — Case workload dashboard.** As a manager, I want average processing time, overdue trends, assignment distribution and exportable charts. Acceptance: dashboard matches independent SQL verification; aggregation handles empty data and timezone boundaries.

## Phase 2 — Identity, policy and selected write operations

**MOD-201 — Modern enterprise login.** As an administrator, I want OIDC/SSO and session/permission bridging without disrupting users during coexistence. Acceptance: no credential duplication; sessions, logout and authorization are validated across legacy and new routes.

**MOD-202 — Granular case permissions.** As a compliance officer, I want case-level department ownership and document download entitlements. Acceptance: workers may only access authorized cases; a cross-department forbidden-resource test passes.

**MOD-203 — Modern assignment service.** As a supervisor, I want reassignment rules and transactional audit events to be owned by a Spring Boot component while other operations remain legacy. Acceptance: one authoritative writer, no lost updates, audit records preserved, rollback documented.

**MOD-204 — Configurable approvals.** As a business analyst, I want category-based approval chains and escalation policies rather than hard-coded transitions. Acceptance: configurable rules determine reviewers; self-approval blocked; historical cases retain valid state transitions.

## Phase 3 — Documents, notifications and back-office

**MOD-301 — Managed document storage.** As a worker, I want documents stored in object storage with scanning, type verification and retention rules. Acceptance: old `bytea` documents remain available and downloads enforce owner authorization.

**MOD-302 — Reliable notifications.** As a manager, I want scheduled overdue reminders, retry policies and email templates. Acceptance: exactly-once-like idempotent delivery handling, retry observability and no message loss after restart.

**MOD-303 — External integration API.** As another agency system, I want a versioned, authenticated integration interface to exchange case status updates. Acceptance: OAuth2 service identity, documented contracts, idempotent writes and replay protection. This capability does not exist in the Struts baseline.

**MOD-304 — Performance and compliance.** As an architect, I want workload tests, accessibility testing, dependency scanning, security checks and upgrade documentation. Acceptance: tracked performance/error/security measures meet agreed thresholds.

## Modernization sequence

1. Establish baseline tests and deploy both runtimes privately.
2. Route only read-only case-search URLs to Spring Boot/Angular first.
3. Migrate a single write workflow, then notifications, then document storage.
4. Retire Struts actions/DAO code only when old routes have no callers and historical data remains accessible.

For each story capture an Architecture Decision Record with context, options, chosen design, test strategy, rollback and ownership boundaries. Keep this project-wide backlog in the repository-root `docs/` directory and decision records in `docs/adr/`. Use Java 21/Angular in the replacement only; keep this repository frozen as the historical comparison.
