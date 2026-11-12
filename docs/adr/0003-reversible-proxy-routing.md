# ADR-0003: Switch read-only capabilities at a reverse proxy

- Status: Accepted for routing infrastructure; real replacement acceptance pending.
- Story: MOD-003
- Recorded: 2026-11-10

## Context

MOD-001 supplies behavioral contracts and MOD-002 supplies implementation ownership and
correlation. Only the legacy runtime exists in this repository. Struts uses servlet sessions,
per-request active-account refresh and CSRF-protected writes. Sharing its JSESSIONID cookie
does not authenticate a user in a separate application. Legacy remains the historical baseline.

## Options

1. Route inside Struts. This couples the frozen business application to replacement deployment
   and requires legacy releases to operate routing.
2. Introduce an external proxy with an explicit capability contract. Both backends retain
   their own security and business responsibilities; switching is independent of WAR releases.

## Decision

Choose NGINX as an external proxy. The Java `routing-gateway` Maven module validates a closed JSON schema and
generates a complete configuration with a content-derived revision. Only GET/HEAD search
and details routes can move, independently. All other application routes and all write
methods stay legacy. Unknown capability switches, URLs in endpoint fields, configuration
interpolation and missing readiness acknowledgment are rejected. Exact route locations avoid
prefix matches; case aliases cannot become additional modern entry points on Windows.
Normalized known paths are forwarded using canonical paths and unchanged query strings.

When the configured context is `/legacy_case_management`, the gateway accepts the entry URLs
`/legacy-case-management` and `/legacy-case-management/` as relative HTTP 308 redirects to
`/legacy_case_management/`. Query parameters and methods are preserved; redirects do not contact
either backend. This compatibility entry point leaves cookies, application paths and capability
ownership on the deployed context. Other context configurations do not acquire this alias.

The CLI is a standalone Java 8-compatible executable JAR, separate from the legacy WAR and EAR.
It renders a packaged NGINX template, validates JSON using Gson, atomically replaces files with
Java NIO and invokes NGINX using ProcessBuilder without a shell. JUnit unit and opt-in
real-NGINX integration tests use the same Maven build as the application. No scripting runtime
is required. Configuration revisions use deterministic sorted JSON and SHA-256.

The JAR plugin always recreates the unbundled application JAR before Shade replaces it with
the executable. This keeps repeated IntelliJ `package` launches from reusing an already bundled
JAR as Shade input. Only dependency manifests and JPMS module descriptors are excluded from
the bundle; the executable has its own main-class manifest and retains dependency classes,
license resources and Maven metadata.

For local IntelliJ startup, Tomcat's Before launch task runs the shared **Routing Gateway Start**
Maven run configuration. It does not use a direct Maven Goal task, which requires the referenced
module to be present in the IDE's imported Maven model and can abort startup after a module rename.
The shared configuration invokes the module POM directly. Existing application build/deployment
tasks remain in place; the Windows launcher manages a separate IDE gateway runtime.

Both backends must expose the same single-segment context path and the same public read
contracts, including input routes, query names, authorization and navigation. This deliberately
avoids cookie-path and redirect rewriting. Replacement resources/API reads live under
`<context>/modern/`; that namespace accepts GET/HEAD only and is disabled when all capabilities
are legacy. Replacement pages must send mutations to existing legacy form endpoints with
the legitimate legacy CSRF contract, or omit mutation controls until identity bridging exists.

The proxy neither invents authentication nor trusts client identity headers as evidence.
Before setting `replacement_ready=true`, operators must verify server-side identity bridging,
disabled-account/role refresh, logout, authorization and distinct replacement session cookies.
Replacement database credentials must enforce read-only access. The boolean is an operator
acknowledgment, not an automated security certification. No replacement is enabled by default.

The gateway overwrites request IDs with hyphenated NGINX-generated random IDs compatible with
the MOD-002 canonical UUID shape, including on proxy-generated errors. Deployments enable
legacy `trustRequestId` only behind the gateway and block direct backend access. Backend
X-Implementation is preserved, never synthesized. Safe access events record only request ID,
revision, bounded capability/implementation labels, status and duration. Native error logging
is directed to the OS null device because it can contain query values. Configuration validation
diagnostics are still available from `nginx -t`; request debugging needs an explicitly reviewed,
temporary logging policy. Log collection and retention remain deployment responsibilities.

Automatic retries, failover, caching and error interception are disabled. An unavailable modern
route fails closed; the operator controls rollback. The gateway does not shadow or dual-write.

## Switching and rollback

Serialize local switches with an exclusive lock, syntax-check a candidate, atomically replace
the active configuration and signal a graceful reload. Save one prior successful configuration.
Reject invalid candidates before changing active files. Restore the active file if signaling
fails. Applying identical content preserves the rollback target. Rollback validates and reloads
the prior configuration using the same process; repeated rollback toggles the last two versions.
Only one deployment controller may operate the gateway; render is for stopped instances only.

NGINX reload signaling is asynchronous. A successful command is not proof that new workers
became active: operators must check the response revision, actual owner and correlated backend
completion. Graceful reload allows existing requests to finish under the old configuration.
This is safe here because modern routes are read-only and legacy remains the sole writer.
There is no database restore during route rollback. Stale modern resources may return 404 after
full rollback; users reload the legacy page.

Write migration is deliberately unsupported. MOD-203 must introduce authoritative writer
ownership/fencing, drain semantics, optimistic concurrency/idempotency where applicable,
compatible schema evolution and tested persisted/audit rollback behavior before adding switches.
Routing alone cannot prove database safety for two writers. Background jobs remain legacy.

## Validation and limits

Unit tests cover validation and switch failure handling. Real NGINX transport tests exercise all
toggle combinations, GET/HEAD versus POST, unmigrated routes, query/body/cookie preservation,
redirects, resource isolation, spoofed correlation headers, safe logs, backend errors/outages,
invalid candidate rejection, no-op switches and restoration of the prior revision.

The backend fixtures are synthetic and do not establish real Struts/Boot equivalence. Before
accepting a migrated capability, run the existing browser and characterization suites through
the deployed topology, register the replacement MOD-001 adapter, and retain equivalent fixture,
authorization and correlated telemetry evidence before and after rollback. PostgreSQL, TLS,
backend isolation, session bridging and production log collection need deployment validation.

NGINX semantics: [proxy directives](https://nginx.org/en/docs/http/ngx_http_proxy_module.html),
[request variables](https://nginx.org/en/docs/http/ngx_http_core_module.html#variables),
[graceful reload](https://nginx.org/en/docs/control.html).
