# MOD-003 operations

The checked-in configuration routes every application request to legacy. Two independent
read-only capability switches are available; write migration remains disabled.
See [ADR-0003](adr/0003-reversible-proxy-routing.md) for the rationale and acceptance limits.

## Install and start

Install a JDK 8+ and a supported NGINX build from the deployment's trusted package source
or [official downloads](https://nginx.org/en/download.html). Build the Java CLI with the Maven
wrapper; its executable JAR includes its JSON dependency. No separate scripting runtime is needed.
The transport suite was run with NGINX 1.30.6 on Windows. Windows commands below are for local
development; use the deployment's service manager on Linux. The gateway is loopback HTTP by
default; establish production TLS, access control and service supervision before public use.

From the repository root in PowerShell, configure `routing-gateway/routes.json`:

| Field | Meaning |
| --- | --- |
| `listen` | IPv4:port; default `127.0.0.1:8088` |
| `context` | One external path segment, default `/cases`; identical in both backends |
| `legacy` | Private IPv4:port of the deployed legacy WAR |
| `replacement` | Private IPv4:port of the replacement; unused until enabled |
| `replacement_ready` | Operator acknowledgment of the readiness checks below |
| `capabilities.case-search` | `legacy` or `replacement` |
| `capabilities.case-details` | `legacy` or `replacement` |

Use `/legacy_case_management` for `context` if keeping the documented local IntelliJ deployment.
Endpoints are addresses, not URLs; DNS names, credentials and additional path segments are rejected.
The unused replacement endpoint may be offline when all switches are legacy.

```powershell
$nginx = 'C:/tools/nginx/nginx.exe' # Your installed executable
.\mvnw.cmd -pl routing-gateway verify
if ($LASTEXITCODE -ne 0) { throw 'Routing tool build failed' }
java -jar routing-gateway/target/routing-gateway.jar render
if ($LASTEXITCODE -ne 0) { throw 'Render failed' }
$prefix = (Resolve-Path routing-gateway/target/runtime).Path.Replace('\', '/') + '/'
& $nginx -p $prefix -c nginx.conf -t
if ($LASTEXITCODE -ne 0) { throw 'NGINX validation failed' }
Start-Process -FilePath $nginx -ArgumentList @('-p', ('"' + $prefix + '"'), '-c', 'nginx.conf') -WindowStyle Hidden
```

Browse `http://127.0.0.1:8088/cases/`. NGINX preserves response cookies and redirects; configure
backends to use the public origin and relative redirects. It passes the original Host, cookies,
request bodies, multipart data and query parameters. The 4 MiB proxy body limit accommodates
the legacy 3 MiB multipart limit. Paths outside the context return 404, except for the
documented hyphenated IDE entry-point redirects below.

In the deployed legacy descriptor, enable `RequestLoggingFilter`'s `trustRequestId=true`
only after blocking direct backend access. Redeploy that deployment overlay; the historical
WAR default remains false. The replacement must likewise accept correlation IDs only from
the trusted gateway. Check both runtimes' completion events against response IDs.

## IntelliJ Tomcat 9 startup (Windows)

The local **Tomcat 9** run configuration has a **Before launch | Run Another Configuration**
task targeting the shared **Routing Gateway Start** Maven configuration. It builds and starts
Routing Gateway before Tomcat starts. Existing Build and Build Artifacts tasks remain.
The shared configuration uses `routing-gateway/pom.xml` with separate command arguments:

```text
-Prouting-intellij -DskipTests package
```

Reload Maven projects after pulling these changes. Local Tomcat configuration lives in
`.idea/workspace.xml`; to recreate its hook, open **Run | Edit Configurations | Tomcat 9**,
remove the old **Run Maven Goal** gateway task if present, add **Before launch | Run Another
Configuration**, and select **Routing Gateway Start**. Put it before Build and Build Artifacts.
If the IDE was already open and the task is not visible, add it there so the live IDE retains it.
Use this shared configuration instead of a direct Maven Goal task: the latter can silently abort
Tomcat launch when the referenced module is absent from IntelliJ's imported Maven project model
after a rename. IntelliJ supports [before-launch tasks](https://www.jetbrains.com/help/idea/run-debug-configuration-tomcat-server.html).

The launcher uses `ROUTING_NGINX` (a full `nginx.exe` path), NGINX on PATH, or the existing
`routing-gateway/target/nginx-1.30.6/nginx.exe` test download, in that order. Set `ROUTING_NGINX`
in the environment inherited by IntelliJ and restart IntelliJ if needed. No binary is downloaded
by the startup task. Java comes from the Maven runner JDK.

The IDE setup has its own configuration and runtime:

- Routes: `routing-gateway/routes-intellij.json`, matching Tomcat's `/legacy_case_management`
  context and HTTP port 8080. Read-only toggles still default to legacy.
- Runtime: `routing-gateway/target/intellij-runtime`.
- Browser URL: `http://127.0.0.1:8088/legacy_case_management/`.

The local Tomcat **Server** tab browser URL is set to that proxy address. When recreating the
configuration, use `/legacy_case_management/` with underscores. The gateway also accepts
`http://localhost:8088/legacy-case-management` (with or without a trailing slash) and redirects
to the deployed underscore context using HTTP 308, preserving query parameters and the method.
This alias covers the application entry point; pages, assets and cookies use the canonical context.
Direct Tomcat access uses `http://localhost:8080/legacy_case_management/`; port 8080 does not
provide the hyphenated alias. Use port 8088 to exercise gateway routing.
Starting the proxy does not wait for backend readiness; requests can return 502 until Tomcat
finishes starting. Repeated starts verify the gateway process and apply route configuration
without starting a second master. A startup failure fails the Maven task and prevents Tomcat launch.
An already-running gateway on port 8088 from another runtime must be stopped separately first.

The gateway runs independently of Tomcat. Stopping Tomcat leaves it running for the next restart.
To stop the IDE gateway explicitly:

```powershell
.\scripts\Start-RoutingGateway.ps1 -Action Stop
```

For IDE route changes, edit `routes-intellij.json` and rerun **Routing Gateway Start**. For
explicit CLI apply/rollback operations, use `--config routing-gateway/routes-intellij.json`
and `--prefix routing-gateway/target/intellij-runtime` so they target this instance.
The startup script is Windows-specific; the Java routing CLI remains portable.

Repeated startup builds do not require `clean`: packaging recreates the unbundled JAR before
Shade bundles dependencies. If an older checkout prints hundreds of overlapping Gson/annotation
classes, reload Maven after updating `routing-gateway/pom.xml` and retry the launch. Avoid using
`clean` to work around this while NGINX is using files under the module's `target` directory.

## Capability contract

All paths below are relative to `context`.

| Capability | Switchable requests |
| --- | --- |
| `case-search` | GET/HEAD `/cases.do`, `/casesInput.do` |
| `case-details` | GET/HEAD `/caseView.do`, `/caseInput.do` |
| Modern supporting resources | GET/HEAD `/modern/*`, when at least one capability is modern |
| Everything else | Legacy, including all mutation methods on existing routes |

Non-read methods under `/modern/` return 405. With both switches legacy that namespace returns
404. Supply replacement static assets and read APIs under this namespace; replacement pages must
continue linking to stable public URLs. The proxy does not translate a Struts request contract into
a different Spring API. The replacement needs compatibility endpoints when preserving these URLs.

Before enabling a replacement capability:

1. Run real replacement fixture comparisons using the MOD-001 adapter/runner contract.
2. Verify authenticated and anonymous reads, active/disabled accounts, role changes, cross-user
   authorization, logout and expiration across both runtimes. Establish server-side session
   validation or an identity bridge; never infer authentication from a cookie or a client header.
3. Use a distinct replacement session-cookie name; preserve the legacy cookie and CSRF contract.
4. Enforce read-only database access for the replacement and leave jobs/writes on legacy.
5. Verify response `X-Implementation: replacement`, correlated completion telemetry and the
   public URL/cookie/redirect contract. Ensure API and asset URLs remain under `/modern/`.

Only then set `replacement_ready` to `true`. This setting records readiness acknowledgment;
the generator cannot verify the deployment's security or database permissions.

## Switch and rollback

Edit the desired capability value in `routing-gateway/routes.json` and apply from the repository root:

```powershell
java -jar routing-gateway/target/routing-gateway.jar apply --nginx $nginx
if ($LASTEXITCODE -ne 0) { throw 'Route switch failed' }
```

The tool locks local switches, validates a candidate with `nginx -t`, atomically installs it,
saves one prior configuration and signals reload. Never use `render` on a running instance.
Custom runtimes use the same `--prefix` argument for render, apply, rollback and NGINX startup.
The CLI defaults assume the repository root; elsewhere supply an absolute `--config` path and
`--prefix` directory. NGINX command diagnostics are written to `<prefix>/nginx-command.log`.
Keep the prefix on local storage with write permissions restricted to the deployment controller.

Immediately request the affected capability with an authenticated test session. Confirm
`X-Route-Revision` equals the revision in `target/runtime/nginx.conf`, `X-Implementation` reports
the actual expected backend, and the matching request ID appears in that backend's completion
telemetry. Recheck an unrelated read, a legacy write and authorization failures. A successful
reload signal alone does not establish activation; inspect these checks before accepting the switch.
Read-only requests already in flight may complete on the previous workers.

To restore the previous configuration:

```powershell
java -jar routing-gateway/target/routing-gateway.jar rollback --nginx $nginx
if ($LASTEXITCODE -ne 0) { throw 'Rollback failed' }
```

Verify the restored revision, owner, read results and a legacy write. Reconcile `routes.json`
with the rolled-back state before another apply, which otherwise reapplies its desired state.
Repeated rollback alternates the previous two configurations. Repeated identical applies preserve
the rollback target. To disable only one capability, set it to `legacy` and apply instead.
Reload old browser tabs after full rollback; old modern resources intentionally return 404.

For an all-legacy emergency state, set both switches to `legacy` and apply. No database restore
or schema change is part of this operation. Proxy failures return errors and never automatically
replay requests to the other backend. Do not add fallback/retry behavior for mutations.

If a switch process crashes, verify the active revision and inspect `nginx.conf`, `previous.conf`
and the gateway process before reconciling state. Remove a stale `switch.lock` only after proving
no controller is running. Maintain deployment configuration history separately: one backup is
not a crash journal or a multi-node deployment transaction. Each node must be switched and checked.

To stop the local gateway gracefully:

```powershell
& $nginx -p $prefix -c nginx.conf -s quit
```

## Tests and evidence

```powershell
.\mvnw.cmd -pl routing-gateway verify
.\mvnw.cmd -pl routing-gateway -Prouting-integration "-Drouting.nginx=$nginx" verify
```

The second command starts real NGINX with synthetic backends on ephemeral loopback ports and
stops them after testing. It tests transport mechanics, not business behavior. The fixture accepts
synthetic cookies and does not implement real authentication, CSRF or database transactions.
Normal builds run the Java unit tests. Integration tests require the explicit profile and
fail if `routing.nginx` is absent or invalid. Reports are in `routing-gateway/target/surefire-reports`
and `routing-gateway/target/failsafe-reports`. Both suites use JUnit 4, matching the existing build.

For deployment acceptance retain this matrix with fixture results and correlated telemetry:

| State | Evidence |
| --- | --- |
| All legacy | Login/logout, browser workflow, CSRF, attachment and persisted-state checks |
| Each capability modern, individually and together | Same read fixtures/authorization; legacy owns writes |
| Replacement returns errors or is unavailable | Explicit error; no fallback/replay |
| Each capability restored to legacy | Equivalent reads; persisted status/ownership/audit/attachments preserved |
| Every state | Response revision, actual owner, UUID and matching backend completion |

Run the existing characterization and browser suites as regression checks:

```powershell
.\mvnw.cmd -Pcharacterization,functional verify
```

Those suites currently host/call legacy directly. They do not automatically traverse this gateway
or supply a replacement. For real cutover acceptance, execute the same browser scenarios at the
gateway URL and register/run the real replacement characterization adapter. Do not report a green
synthetic routing suite as proof that MOD-001/MOD-002 cross-runtime acceptance has passed.

Collect `target/runtime/logs/routing.jsonl` from every gateway node. Events exclude queries,
bodies, cookies, usernames, raw paths and client header values. Backend implementation is
allowlisted to `legacy`, `replacement` or `unknown`; missing headers on proxy errors remain
unknown. Native NGINX error logs are suppressed because they can contain query values; configuration
test diagnostics remain available. Configure protected durable collection/retention separately.
