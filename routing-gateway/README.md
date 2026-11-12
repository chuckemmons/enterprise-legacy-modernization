# Routing Gateway (MOD-003)

NGINX configuration and a Java command-line tool built as a Maven module.
The tool runs on Java 8+ and ships as `target/routing-gateway.jar` with its JSON dependency included.
No business code, database migrations or replacement business endpoints are introduced.

See [operations](../docs/MOD-003-OPERATIONS.md) for installation, readiness requirements,
switching, rollback and acceptance evidence, and [ADR-0003](../docs/adr/0003-reversible-proxy-routing.md)
for ownership and safety decisions.

From the repository root:

```powershell
.\mvnw.cmd -pl routing-gateway verify
.\mvnw.cmd -pl routing-gateway -Prouting-integration '-Drouting.nginx=C:/tools/nginx/nginx.exe' verify
java -jar routing-gateway/target/routing-gateway.jar render
```

The second command requires an installed NGINX binary and runs the real proxy on random
loopback ports against **synthetic** legacy/replacement HTTP fixtures. It fails if NGINX
is missing; it never silently skips integration checks. Tested with NGINX 1.30.6 on Windows.
NGINX binaries and generated runtime files belong under ignored `target/` directories or
outside the repository. These tests do not certify real replacement authentication or parity.

Normal Maven builds run the unit tests; the `routing-integration` profile explicitly enables
the real-proxy tests. Surefire/Failsafe reports are under `routing-gateway/target/`. The CLI defaults
assume execution from the repository root; use `--config` and `--prefix` elsewhere.
Use `java -jar routing-gateway/target/routing-gateway.jar apply --nginx <executable>` or `rollback`
to switch a running instance. NGINX handles HTTP traffic; Java manages its configuration.

Validation: 9 unit tests and 9 real-NGINX integration tests passed on JDK 8.
Routing sources pass the repository's Google formatter and Checkstyle rules.
