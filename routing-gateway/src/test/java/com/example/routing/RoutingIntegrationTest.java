package com.example.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Real NGINX transport tests; backend fixtures do not establish business or security parity.
 *
 * @author Chuck Emmons
 */
public class RoutingIntegrationTest {
  /** Keeps each test's runtime files under the module's build directory and cleans them up. */
  @Rule public final TemporaryFolder temporary = new TemporaryFolder(Paths.get("target").toFile());

  private Path binary;
  private Path prefix;
  private Fixture legacy;
  private Fixture replacement;
  private JsonObject config;
  private Process process;
  private int port;

  /**
   * Starts real NGINX and isolated synthetic backends for one transport scenario.
   *
   * @throws Exception if executable validation, fixture setup or gateway activation fails
   */
  @Before
  public void start() throws Exception {
    String executable = System.getProperty("routing.nginx", "");
    if (executable.isEmpty() || !Files.isRegularFile(Paths.get(executable))) {
      throw new IllegalArgumentException("Supply -Drouting.nginx=/absolute/path/to/nginx");
    }
    binary = Paths.get(executable).toAbsolutePath();
    try {
      legacy = new Fixture("legacy");
      replacement = new Fixture("replacement");
      try (ServerSocket socket =
          new ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
        port = socket.getLocalPort();
      }
      prefix = temporary.newFolder("runtime with spaces").toPath().toAbsolutePath();
      config = GatewayTest.baseline();
      config.addProperty("listen", "127.0.0.1:" + port);
      config.addProperty("legacy", legacy.endpoint());
      config.addProperty("replacement", replacement.endpoint());
      Gateway.prepare(prefix);
      Gateway.render(prefix, NginxConfig.render(RouteConfig.parse(config.toString())));
      Gateway.nginx(binary, prefix, "-t", "-c", "nginx.conf");
      process =
          new ProcessBuilder(
                  binary.toString(),
                  "-p",
                  prefix.toString().replace('\\', '/') + "/",
                  "-c",
                  "nginx.conf",
                  "-g",
                  "daemon off;")
              .redirectErrorStream(true)
              .redirectOutput(prefix.resolve("startup.log").toFile())
              .start();
      waitForOwner("legacy", RouteConfig.parse(config.toString()).revision());
    } catch (Exception failure) {
      stop();
      throw failure;
    }
  }

  /**
   * Stops the proxy and both fixtures, including after a failed assertion.
   *
   * @throws Exception if NGINX cannot be signaled or waiting for termination is interrupted
   */
  @After
  public void stop() throws Exception {
    try {
      if (process != null && process.isAlive()) {
        try {
          Gateway.nginx(binary, prefix, "-c", "nginx.conf", "-s", "quit");
          assertTrue("NGINX must exit", process.waitFor(15, TimeUnit.SECONDS));
        } finally {
          if (process.isAlive()) {
            process.destroyForcibly().waitFor(5, TimeUnit.SECONDS);
          }
        }
      }
    } finally {
      if (legacy != null) {
        legacy.server.stop(0);
      }
      if (replacement != null) {
        replacement.server.stop(0);
      }
    }
  }

  /**
   * Verifies all ownership combinations and preservation of query strings and HEAD semantics.
   *
   * @throws Exception if route activation or fixture requests fail
   */
  @Test
  public void independentSwitchesPreserveQueriesAndHeadRequests() throws Exception {
    for (String search : new String[] {"legacy", "replacement"}) {
      for (String details : new String[] {"legacy", "replacement"}) {
        activate(search, details);
        String[][] paths = {
          {"cases.do?q=a%20b&page=2", search},
          {"casesInput.do", search},
          {"caseView.do?id=42", details},
          {"caseInput.do", details}
        };
        for (String[] entry : paths) {
          Response response = request("/cases/" + entry[0]);
          assertEquals(200, response.status);
          assertEquals(entry[1], response.owner);
          assertEquals("/cases/" + entry[0], response.event().get("path").getAsString());
        }
        Response response = request("/cases/cases.do", "HEAD", null, Collections.emptyMap());
        assertEquals(search, response.owner);
        assertEquals("", response.body);
      }
    }
  }

  /**
   * Verifies legacy ownership for mutations and unmigrated routes while both read switches are on.
   *
   * @throws Exception if route activation or fixture requests fail
   */
  @Test
  public void writesAndUnmigratedRoutesStayLegacy() throws Exception {
    activate("replacement", "replacement");
    String[] mutations = {
      "authenticate.do",
      "logout.do",
      "caseCreate.do",
      "caseUpdate.do",
      "caseAssign.do",
      "caseTransition.do",
      "caseComment.do",
      "caseArchive.do",
      "caseUpload.do",
      "userCreate.do",
      "notificationRead.do",
      "cases.do"
    };
    for (String path : mutations) {
      final int before = replacement.events.size();
      Response response =
          request("/cases/" + path, "POST", "csrf=fixture&value=test", Collections.emptyMap());
      assertEquals(200, response.status);
      assertEquals("legacy", response.owner);
      assertEquals("csrf=fixture&value=test", response.event().get("body").getAsString());
      assertEquals(before, replacement.events.size());
    }
    for (String method : new String[] {"PUT", "DELETE", "OPTIONS"}) {
      assertEquals(
          "legacy", request("/cases/cases.do", method, null, Collections.emptyMap()).owner);
    }
    for (String path :
        new String[] {
          "caseDownload.do",
          "operations.do",
          "health/live",
          "assets/site.css",
          "cases.do/extra",
          "caseAssign.do;extra"
        }) {
      assertEquals("legacy", request("/cases/" + path).owner);
    }
    assertNotEquals("replacement", request("/cases/CASEs.do").owner);
  }

  /**
   * Verifies spoofed-header replacement, backend correlation and exclusion of secrets from logs.
   *
   * @throws Exception if activation, fixture requests or access-log reads fail
   */
  @Test
  public void idsAreOverwrittenAndCorrelatedWithSafeLogs() throws Exception {
    final String revision = activate("replacement", "legacy");
    String incomingId = UUID.randomUUID().toString();
    Map<String, String> headers = new LinkedHashMap<>();
    headers.put("X-Request-ID", incomingId);
    headers.put("X-Implementation", "client-spoof");
    headers.put("Cookie", "JSESSIONID=COOKIE_CANARY");
    headers.put("X-Forwarded-For", "CLIENT_CANARY");
    Response response = request("/cases/cases.do?password=SECRET_CANARY", "GET", null, headers);
    JsonObject event = response.event();
    assertEquals(response.requestId, UUID.fromString(response.requestId).toString());
    assertNotEquals(incomingId, response.requestId);
    assertEquals(response.requestId, event.get("requestId").getAsString());
    assertNull(event.get("claimedOwner"));
    assertEquals("127.0.0.1", event.get("forwardedFor").getAsString());
    assertEquals("JSESSIONID=COOKIE_CANARY", event.get("cookie").getAsString());
    assertEquals(revision, response.revision);
    String log = Gateway.read(prefix.resolve("logs/routing.jsonl"));
    for (String secret : new String[] {"SECRET_CANARY", "COOKIE_CANARY", "CLIENT_CANARY"}) {
      assertFalse(log.contains(secret));
    }
    boolean found = false;
    for (String line : log.split("\\R")) {
      JsonObject record = JsonParser.parseString(line).getAsJsonObject();
      if (record.get("requestId").getAsString().equals(response.requestId)) {
        assertEquals("replacement", record.get("implementation").getAsString());
        assertEquals(revision, record.get("revision").getAsString());
        found = true;
      }
    }
    assertTrue("Correlated access event must exist", found);
  }

  /**
   * Verifies that backend errors and connection failures never invoke legacy fallback or retries.
   *
   * @throws Exception if route activation, socket setup or fixture requests fail
   */
  @Test
  public void backendErrorsNeverFallbackOrRetry() throws Exception {
    activate("replacement", "legacy");
    replacement.status = 503;
    int oldCount = legacy.events.size();
    final int newCount = replacement.events.size();
    Response response = request("/cases/cases.do");
    assertEquals(503, response.status);
    assertEquals("replacement", response.owner);
    assertEquals(oldCount, legacy.events.size());
    assertEquals(newCount + 1, replacement.events.size());
    // Stop the backend and point at an unused port to exercise connection refusal.
    try (ServerSocket unused = new ServerSocket()) {
      unused.bind(new InetSocketAddress("127.0.0.1", 0));
      int unavailablePort = unused.getLocalPort();
      unused.close();
      replacement.server.stop(0);
      config.addProperty("replacement", "127.0.0.1:" + unavailablePort);
      String candidate = NginxConfig.render(RouteConfig.parse(config.toString()));
      Gateway.switchConfig(prefix, candidate, this::nginx);
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
      do {
        response = request("/cases/cases.do");
        if (response.status == 502) {
          break;
        }
        Thread.sleep(50);
      } while (System.nanoTime() < deadline);
      assertEquals(502, response.status);
      assertNull(response.owner);
      assertEquals(oldCount, legacy.events.size());
    }
  }

  /**
   * Verifies preservation of redirects and cookie paths and isolation of modern resources.
   *
   * @throws Exception if route activation or fixture requests fail
   */
  @Test
  public void redirectsCookiesAndModernResourcesKeepTheirContract() throws Exception {
    Response login = request("/cases/login.do");
    assertEquals(302, login.status);
    assertEquals("/cases/cases.do", login.location);
    assertTrue(login.cookie.contains("Path=/cases"));
    assertEquals(404, request("/cases/modern/main.js").status);
    activate("replacement", "legacy");
    assertEquals("replacement", request("/cases/modern/main.js").owner);
    assertEquals(405, request("/cases/modern/api", "POST", "data", Collections.emptyMap()).status);
    assertEquals(404, request("/unrelated").status);
  }

  /**
   * Verifies context-specific entry redirects preserve methods and queries without backend calls.
   *
   * @throws Exception if context activation or fixture requests fail
   */
  @Test
  public void hyphenatedIdeEntryRedirectsWithoutCallingBackends() throws Exception {
    assertEquals(404, request("/legacy-case-management").status);
    config.addProperty("context", "/legacy_case_management");
    RouteConfig validated = RouteConfig.parse(config.toString());
    Gateway.switchConfig(prefix, NginxConfig.render(validated), this::nginx);
    waitForOwner("legacy", validated.revision(), "/legacy_case_management/login.do");
    final int legacyCount = legacy.events.size();
    final int replacementCount = replacement.events.size();
    for (String alias : new String[] {"/legacy-case-management", "/legacy-case-management/"}) {
      for (String method : new String[] {"GET", "HEAD", "POST"}) {
        Response response =
            request(alias + "?page=2&q=a%20b", method, null, Collections.emptyMap());
        assertEquals(308, response.status);
        assertEquals("/legacy_case_management/?page=2&q=a%20b", response.location);
        assertNull(response.owner);
      }
    }
    assertEquals(legacyCount, legacy.events.size());
    assertEquals(replacementCount, replacement.events.size());
    assertEquals(404, request("/legacy-case-management-extra").status);
    assertEquals(404, request("/unrelated").status);
  }

  /**
   * Verifies that rollback restores the previous route owner and configuration revision.
   *
   * @throws Exception if activation, rollback or activation probing fails
   */
  @Test
  public void rollbackRestoresPreviousRevision() throws Exception {
    final String revision = RouteConfig.parse(config.toString()).revision();
    activate("replacement", "legacy");
    Gateway.switchConfig(prefix, null, this::nginx);
    waitForOwner("legacy", revision);
  }

  /**
   * Verifies real NGINX syntax rejection preserves the active file, backup and running routes.
   *
   * @throws Exception if fixture activation or configuration-file reads fail
   */
  @Test
  public void invalidCandidatePreservesRunningConfigAndBackup() throws Exception {
    final String revision = activate("replacement", "legacy");
    String original = Gateway.read(prefix.resolve("nginx.conf"));
    String backup = Gateway.read(prefix.resolve("previous.conf"));
    assertThrows(
        IOException.class,
        () -> Gateway.switchConfig(prefix, "invalid nginx syntax;", this::nginx));
    assertEquals(original, Gateway.read(prefix.resolve("nginx.conf")));
    assertEquals(backup, Gateway.read(prefix.resolve("previous.conf")));
    waitForOwner("replacement", revision);
  }

  /**
   * Verifies that applying identical configuration does not replace the previous rollback target.
   *
   * @throws Exception if activation, rollback or activation probing fails
   */
  @Test
  public void identicalApplyPreservesRollbackTarget() throws Exception {
    final String revision = RouteConfig.parse(config.toString()).revision();
    activate("replacement", "legacy");
    activate("replacement", "legacy");
    Gateway.switchConfig(prefix, null, this::nginx);
    waitForOwner("legacy", revision);
  }

  /**
   * Applies read-route ownership and waits for the search route to expose the expected revision.
   *
   * @param search implementation that should own case search
   * @param details implementation that should own case details
   * @return revision of the applied fixture configuration
   * @throws Exception if rendering, switching or activation probing fails
   */
  private String activate(String search, String details) throws Exception {
    config.addProperty("replacement_ready", true);
    config.getAsJsonObject("capabilities").addProperty("case-search", search);
    config.getAsJsonObject("capabilities").addProperty("case-details", details);
    RouteConfig validated = RouteConfig.parse(config.toString());
    Gateway.switchConfig(prefix, NginxConfig.render(validated), this::nginx);
    waitForOwner(search, validated.revision());
    return validated.revision();
  }

  /**
   * Invokes the installed NGINX executable against this test's isolated runtime directory.
   *
   * @param arguments NGINX validation or control arguments
   * @throws IOException if NGINX cannot run or its command fails
   * @throws InterruptedException if waiting for NGINX is interrupted
   */
  private void nginx(String... arguments) throws IOException, InterruptedException {
    Gateway.nginx(binary, prefix, arguments);
  }

  /**
   * Waits for the case-search endpoint to report the expected implementation and revision.
   *
   * @param owner expected backend implementation
   * @param revision expected routing configuration revision
   * @throws Exception if probing is interrupted
   * @throws AssertionError if the expected configuration is not observed within ten seconds
   */
  private void waitForOwner(String owner, String revision) throws Exception {
    waitForOwner(owner, revision, "/cases/cases.do");
  }

  /**
   * Polls a read-only endpoint until the expected implementation and revision become active.
   *
   * @param owner expected backend implementation
   * @param revision expected routing configuration revision
   * @param path endpoint path used for the activation probe
   * @throws Exception if probing is interrupted
   * @throws AssertionError if the expected configuration is not observed within ten seconds
   */
  private void waitForOwner(String owner, String revision, String path) throws Exception {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
    do {
      try {
        Response response = request(path);
        if (owner.equals(response.owner) && revision.equals(response.revision)) {
          return;
        }
      } catch (IOException pendingStartup) {
        // Retry only the test's read-only activation probe, never production requests.
      }
      Thread.sleep(50);
    } while (System.nanoTime() < deadline);
    throw new AssertionError("NGINX did not activate expected routing");
  }

  /**
   * Sends an unauthenticated GET request to the test gateway without following redirects.
   *
   * @param path request path including any query string
   * @return captured HTTP status, selected headers and body
   * @throws IOException if the request or response cannot be transferred
   */
  private Response request(String path) throws IOException {
    return request(path, "GET", null, Collections.emptyMap());
  }

  /**
   * Exchanges a fixture request with the gateway, retaining redirects as observable responses.
   *
   * @param path request path including any query string
   * @param method HTTP method to send
   * @param body UTF-8 request body, or {@code null} for no body
   * @param headers additional request headers
   * @return captured HTTP status, selected headers and body
   * @throws IOException if the connection, body transfer or response read fails
   */
  private Response request(String path, String method, String body, Map<String, String> headers)
      throws IOException {
    HttpURLConnection connection =
        (HttpURLConnection) new URL("http://127.0.0.1:" + port + path).openConnection();
    connection.setConnectTimeout(3000);
    connection.setReadTimeout(3000);
    connection.setInstanceFollowRedirects(false);
    connection.setRequestMethod(method);
    headers.forEach(connection::setRequestProperty);
    try {
      if (body != null) {
        connection.setDoOutput(true);
        try (java.io.OutputStream output = connection.getOutputStream()) {
          output.write(body.getBytes(StandardCharsets.UTF_8));
        }
      }
      int status = connection.getResponseCode();
      InputStream input = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
      String content;
      try (InputStream stream = input) {
        content = stream == null ? "" : readStream(stream);
      }
      return new Response(connection, status, content);
    } finally {
      connection.disconnect();
    }
  }

  /**
   * Reads a stream fully as UTF-8 text without taking ownership of its lifecycle.
   *
   * @param input stream to consume
   * @return decoded stream contents
   * @throws IOException if the stream cannot be read
   */
  private static String readStream(InputStream input) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    byte[] buffer = new byte[4096];
    int count;
    while ((count = input.read(buffer)) != -1) {
      output.write(buffer, 0, count);
    }
    return new String(output.toByteArray(), StandardCharsets.UTF_8);
  }

  /**
   * Captures the response data needed to assert gateway ownership and transport behavior.
   *
   * @author Chuck Emmons
   */
  private static final class Response {
    final int status;
    final String owner;
    final String requestId;
    final String revision;
    final String body;
    final String location;
    final String cookie;

    /**
     * Captures selected response headers together with the already-read status and body.
     *
     * @param connection completed HTTP connection providing response headers
     * @param status received HTTP status code
     * @param body decoded response body
     */
    Response(HttpURLConnection connection, int status, String body) {
      this.status = status;
      this.body = body;
      owner = connection.getHeaderField("X-Implementation");
      requestId = connection.getHeaderField("X-Request-ID");
      revision = connection.getHeaderField("X-Route-Revision");
      location = connection.getHeaderField("Location");
      cookie = connection.getHeaderField("Set-Cookie");
    }

    /**
     * Parses the synthetic backend's response body as its recorded request event.
     *
     * @return decoded backend event
     * @throws com.google.gson.JsonParseException if the response body is not valid JSON
     * @throws IllegalStateException if the parsed value is not a JSON object
     */
    JsonObject event() {
      return JsonParser.parseString(body).getAsJsonObject();
    }
  }

  /**
   * Hosts a synthetic backend that records requests and returns configurable HTTP responses.
   *
   * @author Chuck Emmons
   */
  private static final class Fixture {
    final HttpServer server;
    final Queue<Map<String, String>> events = new ConcurrentLinkedQueue<>();
    volatile int status = 200;

    /**
     * Starts an HTTP fixture on an ephemeral loopback port.
     *
     * @param owner implementation label returned in response headers
     * @throws IOException if the fixture cannot bind its listening socket
     */
    Fixture(String owner) throws IOException {
      server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext("/", exchange -> respond(exchange, owner));
      server.start();
    }

    /**
     * Returns the bound endpoint for insertion into the gateway configuration.
     *
     * @return loopback IPv4 address and assigned port
     */
    String endpoint() {
      return "127.0.0.1:" + server.getAddress().getPort();
    }

    /**
     * Records a request and returns its event, with optional login redirect and cookie headers.
     *
     * @param exchange incoming HTTP exchange, closed after response processing
     * @param owner implementation label to return
     * @throws IOException if request reading or response writing fails
     */
    private void respond(HttpExchange exchange, String owner) throws IOException {
      try {
        Map<String, String> event = new LinkedHashMap<>();
        event.put("method", exchange.getRequestMethod());
        event.put("path", exchange.getRequestURI().toString());
        event.put("body", readStream(exchange.getRequestBody()));
        event.put("requestId", exchange.getRequestHeaders().getFirst("X-Request-ID"));
        event.put("cookie", exchange.getRequestHeaders().getFirst("Cookie"));
        event.put("claimedOwner", exchange.getRequestHeaders().getFirst("X-Implementation"));
        event.put("forwardedFor", exchange.getRequestHeaders().getFirst("X-Forwarded-For"));
        events.add(event);
        exchange.getResponseHeaders().set("X-Implementation", owner);
        exchange.getResponseHeaders().set("X-Request-ID", event.get("requestId"));
        exchange.getResponseHeaders().set("X-Route-Revision", "backend-spoof");
        int code = status;
        if (exchange.getRequestURI().getPath().endsWith("login.do")) {
          code = 302;
          exchange.getResponseHeaders().set("Location", "/cases/cases.do");
          exchange
              .getResponseHeaders()
              .set("Set-Cookie", "JSESSIONID=fixture; Path=/cases; HttpOnly");
        }
        byte[] body = new Gson().toJson(event).getBytes(StandardCharsets.UTF_8);
        boolean head = exchange.getRequestMethod().equals("HEAD");
        exchange.sendResponseHeaders(code, head ? -1 : body.length);
        if (!head) {
          exchange.getResponseBody().write(body);
        }
      } finally {
        exchange.close();
      }
    }
  }
}
