# MOD-002 operations

Design and boundaries: [ADR-0002](adr/0002-legacy-observability.md).

## Use the baseline

Sign in as an active administrator, then open `<application-context>/operations.do`.
Use Request ID lookup to display the HTTP event, unexpected failure type and transactions for
one request. The response headers expose `X-Request-ID` and `X-Implementation: legacy`.
The operations URL uses the existing login/session authorization and refreshes role/disabled
state on every request. Non-administrators receive 403; anonymous users are redirected to login.

- `operations.do?view=events`: export the latest 1,000 events as JSON.
- `operations.do?view=metrics`: cumulative Prometheus text histograms in seconds.
- `health/live`: minimal process/application liveness, without accessing the database.
- `health/ready`: database query plus transaction completion; 200/up or 503/down.

Probe endpoints are intentionally unauthenticated and reveal no credentials or database details.
Restrict `/health/*` to monitoring networks in deployment ingress/container configuration. SQL
timeout is three seconds; configure bounded datasource acquisition/connect timeouts separately.

Run safe deployment probes from the repository root:

```powershell
.\scripts\Check-LegacyObservability.ps1 -BaseUrl http://localhost:8080/legacy-case-management
```

This writes `target/observability-checks.json` and fails if either probe or ownership/correlation
contract fails. It never includes exception text or the supplied URL in the artifact. Probes
do not exercise authenticated workflows: also run the existing functional and characterization
suites against isolated fixtures. Do not run business-write fixtures against production data.

## Fields and comparison contract

An export is `{ "implementation": "legacy", "startedAtEpochMillis": 0, "capacity": 1000,
"events": [...] }`. Each event includes:

| Field | Meaning |
|---|---|
| `timestamp` | UTC ISO-8601 event timestamp |
| `implementation`, `version` | Actual handling runtime and build version |
| `requestId` | Canonical UUID shared across the request, or `background` for standalone work |
| `operation` | Known route, `other`, or `background` |
| `event` | `request`, `transaction`, `failure`, or `check` |
| `status` | HTTP status; zero for non-HTTP transaction/failure events |
| `outcome` | `complete`/`error`, or readiness `up`/`down` |
| `durationSeconds` | Elapsed monotonic duration in seconds |
| `errorType` | Failure class only; optional, without messages/causes |

The dashboard can load a replacement export with this envelope and fields through Compare
exported snapshot. Requests are grouped by actual implementation; exact ID lookup includes
all associated events. Snapshot percentiles are not cumulative production percentiles.

Metrics use `legacy_duration_seconds_{bucket,count,sum}` with `kind`, `implementation`,
`operation`, `outcome` and cumulative `le` bucket boundaries. All time series reset on restart.
Divide sum by count for average duration; use histogram buckets for cumulative percentiles.
Request IDs must never become metric labels. Status remains in events; `error` counts unexpected
handled failures and HTTP 5xx. HTTP 4xx remain visible by status and are completed requests.

Before enabling upstream IDs, change the `RequestLoggingFilter` init parameter `trustRequestId`
in `WEB-INF/web.xml` to true. This is safe only if the trusted ingress overwrites client-supplied
IDs and direct application access is blocked. Invalid IDs are replaced with server UUIDs.
The proxy must preserve the backend ownership header; it must not infer ownership from a toggle.

## Durable collection and credential policy

Events are logged by `com.example.legacy.observability.Telemetry` through JUL. The default
container formatter wraps JSON in its normal log prefix. A dedicated handler can use
`com.example.legacy.observability.JsonLineFormatter` for one JSON object per line. The handler
must resolve that formatter through the application classloader (or place the formatter/classes
in the container logging classpath). Example settings to merge into the container logging
configuration, adjusting the path and classloading for the server:

```properties
com.example.legacy.observability.Telemetry.handlers=java.util.logging.FileHandler
com.example.legacy.observability.Telemetry.useParentHandlers=false
com.example.legacy.observability.Telemetry.level=INFO
java.util.logging.FileHandler.pattern=/var/log/legacy/telemetry-%g.jsonl
java.util.logging.FileHandler.limit=10485760
java.util.logging.FileHandler.count=10
java.util.logging.FileHandler.append=true
java.util.logging.FileHandler.formatter=com.example.legacy.observability.JsonLineFormatter
```

Use a dedicated FileHandler configuration; the formatter intentionally ignores other logger
categories. Set the path to a restricted directory and ship rotated files to the deployment's
collector with its retention policy. Do not replace a shared container handler's formatter.
Do not enable SQL/bind-value tracing, HTTP body/header dumps or credential-bearing diagnostic
logs in third-party libraries or the proxy. `hibernate.show_sql` remains false. Application
telemetry uses a strict allowlist and excludes exception messages, causes and suppressed text.
In-process history is bounded and lost on redeployment; it cannot prove every historical request
without durable collection. Log-storage failures do not fail business transactions.

## Verify

With JDK 8 as JAVA_HOME:

```powershell
.\mvnw.cmd '-Pcharacterization,functional' verify
```

Tests include real WAR probes, anonymous/worker/admin access, dashboard JavaScript loading,
credential canaries in exports, request/transaction correlation, histograms, lifecycle failure
and bounded history. Deployment-specific PostgreSQL checks and dual-runtime routing/rollback
evidence remain required before final migration acceptance. The repository currently contains
only the legacy implementation; replacement/proxy behavior is not claimed as verified.

Validated on 2026-11-09 with JDK 8: the complete `characterization,functional` reactor passed
377 tests (313 application unit, 35 application integration, 3 comparison-runner unit,
5 characterization and 21 browser tests), including the coverage gate. Credential canaries
were absent from the captured verification output. The PowerShell probe script was also checked
with controlled successful and failing HTTP responses; these script checks do not claim a
live PostgreSQL deployment was exercised.
