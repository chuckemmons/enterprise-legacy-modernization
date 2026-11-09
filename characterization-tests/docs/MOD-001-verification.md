# MOD-001 requirements review

Reviewed: 2026-11-09

## Conclusion

The legacy characterization suite implements and verifies all business coverage requested
by MOD-001. The shared fixtures, baselines, and runner provide a reusable comparison contract.
**MOD-001's full acceptance criterion remains pending:** no replacement implementation adapter
is present, so equivalent behavior across both implementations has not yet been demonstrated.

Source requirement: [modernization backlog](../../docs/MODERNIZATION_BACKLOG.md).
Design: [ADR-0001](../../docs/adr/0001-characterization-test-strategy.md).

## Coverage against the backlog

Scenario steps below refer to `src/main/resources/fixtures/<scenario>.properties`;
expected outputs are in the corresponding `baselines/<scenario>.properties`.

| Requirement | Evidence | Result |
| --- | --- | --- |
| Repeatable integration suite | Real legacy services, Hibernate production mappings, committed transactions, fresh database per scenario, fixed fixtures and independent expected baselines | Verified on H2 |
| Login | `login` steps 1-5: invalid password, disabled account, and successful worker, supervisor, and auditor authentication | Passed |
| Case creation | `lifecycle` step 2: NEW status, creator, department, unassigned ownership, one persisted case, CREATE audit | Passed |
| Assignment | `lifecycle` step 3: worker ownership and supervisor ASSIGN audit; unauthorized assignment in `permissions` step 3 | Passed |
| Processing | `lifecycle` steps 4-5: TRIAGED and IN_PROGRESS; `permissions` step 6 rejects an unassigned worker | Passed |
| Approval | `lifecycle` steps 8-9: PENDING_APPROVAL and APPROVED; `rework` exercises rejection/resubmission; `self-approval` and `permissions` reject forbidden approvals | Passed |
| Closure | `lifecycle` step 10: CLOSED and closure timestamp; steps 12-14 reject edit, reassignment, and upload while preserving state | Passed |
| Audit | Full ordered audit snapshots after each operation: count, entity, action, actor, details; timestamp validity and ordering checked by adapter | Passed |
| Attachments | `lifecycle` steps 6-7 and 11: upload, worker download, auditor download after closure; compare metadata, uploader, size, SHA-256, and fixture bytes | Passed |
| Same input fixture | Every registered adapter receives the same shared account and scenario resources and expected baselines; resources and runner are packaged in the module JAR | Implemented; replacement run pending |
| Equivalent status, ownership, and audit on both implementations | Strict snapshot comparator checks missing, unexpected, and changed fields against the same expected contract | Legacy verified; replacement acceptance pending |
| Architecture decision record | ADR-0001 captures context, options, decision, consequences, test strategy, rollback, and ownership | Documented |

There are five scenarios with 49 action checkpoints in total. Expected snapshots carry forward
between steps, so failed operations must preserve previously expected state and audit output.
Baselines are not derived from actual snapshots at runtime.

## Verification performed

Environment: Windows, Eclipse Temurin JDK 8.0.504.1, repository Maven wrapper.

```powershell
.\mvnw.cmd -Pcharacterization -pl characterization-tests -am verify
```

Passed: three comparator/cleanup tests and five characterization integration scenarios;
zero failures, errors, or skips in the characterization module. Comparator tests verify
ownership and unexpected audit differences, rejection of incomplete baselines, and resource
cleanup after initialization failure. All five generated scenario difference reports were empty.

```powershell
.\mvnw.cmd -Pcharacterization -pl characterization-tests -am '-Dcharacterization.requireReplacement=true' verify
```

The acceptance gate fails as expected with
`Replacement comparison required, but no adapter is registered`.
This is evidence that a legacy-only run cannot claim replacement acceptance.

```powershell
.\mvnw.cmd '-Pcharacterization,functional' verify
```

Passed: the complete reactor, including three characterization comparator tests, five
characterization integration scenarios, and all 19 browser integration tests, with no failures,
errors, or skips in those modules. This also repeated the characterization scenarios successfully
in fresh databases. The browser suite covers login/logout, disabled accounts, case creation,
assignment, processing, approval/rejection/rework, closure, attachments, CSRF, stale edits,
authorization, and responsive interaction.

The initial browser run exposed a timing race in the existing mobile navigation test:
its failure HTML showed Bootstrap's `collapsing` class when Selenium clicked a clipped link.
The test now waits for `#primary-navigation.collapse.show` before clicking. All 19 browser
tests passed on the subsequent complete run. No production UI behavior was changed.

Also verified that the built module JAR contains all shared fixture and baseline resources,
`ScenarioRunner`, and `ImplementationAdapter`. Relative documentation links resolve and
`git diff --check` reports no whitespace errors.

Generated logs and reports are under ignored `target` directories; they are execution evidence,
not committed source. The module README lists the report locations for local or CI inspection.

## Remaining acceptance work

1. Implement an adapter for the independent replacement application. A Java 21-only replacement
   should use a separate Java 21 harness consuming this module's packaged runner and resources.
2. Run the replacement against the identical fixtures and baselines. Require successful matching
   status, ownership, audit, and attachment output before claiming full MOD-001 acceptance.
3. Run PostgreSQL mode with a dedicated test database and `IT_DB_URL`, `IT_DB_USER`, and
   `IT_DB_PASSWORD` configured. These settings were not available in this review, so PostgreSQL
   behavior is not certified by the H2 results.

Browser coverage belongs to the existing `functional-tests` module. H2 service comparisons alone
do not verify HTTP sessions, CSRF, JSP forms, multipart handling, or production deployment.
Preserve the baseline contract when beginning MOD-002 and rerun the suites after its changes.
