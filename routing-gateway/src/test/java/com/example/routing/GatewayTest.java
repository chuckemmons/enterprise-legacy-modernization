package com.example.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Configuration, CLI and switch failure tests without an NGINX installation.
 *
 * @author Chuck Emmons
 */
public class GatewayTest {
  /** Supplies isolated runtime directories and removes them after each test. */
  @Rule public final TemporaryFolder temporary = new TemporaryFolder();

  /**
   * Verifies that the checked-in configuration keeps both read capabilities on legacy.
   *
   * @throws Exception if the baseline configuration cannot be loaded or parsed
   */
  @Test
  public void defaultsToLegacy() throws Exception {
    RouteConfig config = RouteConfig.parse(Gateway.read(Paths.get("routes.json")));
    assertFalse(config.modernEnabled());
    assertEquals("legacy", config.owner("case-search"));
    assertEquals("legacy", config.owner("case-details"));
  }

  /**
   * Verifies that replacement ownership requires an explicit readiness acknowledgment.
   *
   * @throws Exception if the baseline configuration cannot be loaded or parsed
   */
  @Test
  public void replacementRequiresExplicitReadiness() throws Exception {
    JsonObject config = baseline();
    config.getAsJsonObject("capabilities").addProperty("case-search", "replacement");
    assertThrows(IllegalArgumentException.class, () -> RouteConfig.parse(config.toString()));
    config.addProperty("replacement_ready", true);
    assertTrue(RouteConfig.parse(config.toString()).modernEnabled());
  }

  /**
   * Verifies rejection of malformed JSON, unsupported capabilities and unsafe endpoint values.
   *
   * @throws Exception if the baseline configuration cannot be loaded
   */
  @Test
  public void rejectsInvalidOrInjectableConfiguration() throws Exception {
    String[][] invalid = {
      {"listen", "127.0.0.1:80; include secret;"},
      {"legacy", "http://user:password@localhost:8080"},
      {"replacement", "256.0.0.1:8081"},
      {"listen", "127.0.0.1:65536"},
      {"listen", "127.0.0.1:0"},
      {"listen", "127.00.0.1:8088"},
      {"context", "/cases/../admin"},
      {"context", "/cases$host"},
      {"replacement_ready", "false"},
      {"extra", "value"},
      {"replacement", "127.0.0.1:8080"}
    };
    for (String[] change : invalid) {
      JsonObject config = baseline();
      config.addProperty(change[0], change[1]);
      assertThrows(
          change[0], IllegalArgumentException.class, () -> RouteConfig.parse(config.toString()));
    }
    for (String input : new String[] {"null", "[]", "{}", "{unquoted:true}", "{} {}"}) {
      assertThrows(RuntimeException.class, () -> RouteConfig.parse(input));
    }
    JsonObject config = baseline();
    config.getAsJsonObject("capabilities").addProperty("case-assignment", "replacement");
    assertThrows(IllegalArgumentException.class, () -> RouteConfig.parse(config.toString()));
    config.getAsJsonObject("capabilities").remove("case-assignment");
    config.getAsJsonObject("capabilities").addProperty("case-search", "unknown");
    assertThrows(IllegalArgumentException.class, () -> RouteConfig.parse(config.toString()));
  }

  /**
   * Verifies stable revisions for reordered keys and changed revisions for changed settings.
   *
   * @throws Exception if the baseline configuration cannot be loaded or parsed
   */
  @Test
  public void revisionIsIndependentOfJsonFormattingAndKeyOrder() throws Exception {
    JsonObject config = baseline();
    String revision = RouteConfig.parse(config.toString()).revision();
    JsonObject reordered = new JsonObject();
    config.entrySet().stream()
        .sorted((left, right) -> right.getKey().compareTo(left.getKey()))
        .forEach(entry -> reordered.add(entry.getKey(), entry.getValue()));
    assertEquals(revision, RouteConfig.parse(reordered.toString()).revision());
    config.addProperty("listen", "127.0.0.1:9999");
    assertNotEquals(revision, RouteConfig.parse(config.toString()).revision());
  }

  /**
   * Verifies that failed NGINX validation preserves active and backup files and releases the lock.
   *
   * @throws Exception if the isolated runtime files cannot be prepared or inspected
   */
  @Test
  public void validationFailureKeepsActiveAndBackup() throws Exception {
    Path prefix = activePrefix();
    Gateway.atomicWrite(prefix.resolve("previous.conf"), "previous");
    assertThrows(
        IOException.class,
        () ->
            Gateway.switchConfig(
                prefix,
                "candidate",
                arguments -> {
                  throw new IOException("invalid");
                }));
    assertEquals("active", Gateway.read(prefix.resolve("nginx.conf")));
    assertEquals("previous", Gateway.read(prefix.resolve("previous.conf")));
    assertFalse(Files.exists(prefix.resolve("switch.lock")));
  }

  /**
   * Verifies restoration of the active file when validation succeeds but reload signaling fails.
   *
   * @throws Exception if the isolated runtime files cannot be prepared or inspected
   */
  @Test
  public void reloadFailureRestoresActive() throws Exception {
    Path prefix = activePrefix();
    AtomicInteger calls = new AtomicInteger();
    assertThrows(
        IOException.class,
        () ->
            Gateway.switchConfig(
                prefix,
                "candidate",
                arguments -> {
                  if (calls.incrementAndGet() == 2) {
                    throw new IOException("not running");
                  }
                }));
    assertEquals("active", Gateway.read(prefix.resolve("nginx.conf")));
  }

  /**
   * Verifies that render and apply refuse an existing lock without removing another owner's lock.
   *
   * @throws Exception if the isolated runtime files cannot be prepared or inspected
   */
  @Test
  public void concurrentSwitchKeepsOtherControllersLock() throws Exception {
    Path prefix = activePrefix();
    Files.createFile(prefix.resolve("switch.lock"));
    assertThrows(
        IOException.class, () -> Gateway.switchConfig(prefix, "candidate", arguments -> {}));
    assertTrue(Files.exists(prefix.resolve("switch.lock")));
    assertThrows(IOException.class, () -> Gateway.render(prefix, "candidate"));
    assertTrue(Files.exists(prefix.resolve("switch.lock")));
  }

  /**
   * Verifies that a PID file prevents render from overwriting the active configuration.
   *
   * @throws Exception if the isolated runtime files cannot be prepared or inspected
   */
  @Test
  public void renderRefusesRunningInstance() throws Exception {
    Path prefix = activePrefix();
    Files.createFile(prefix.resolve("nginx.pid"));
    assertThrows(IOException.class, () -> Gateway.render(prefix, "candidate"));
    assertEquals("active", Gateway.read(prefix.resolve("nginx.conf")));
    assertFalse(Files.exists(prefix.resolve("switch.lock")));
  }

  /**
   * Verifies CLI rendering into a path with spaces and rejection of invalid actions and options.
   *
   * @throws Exception if output capture or isolated runtime-file operations fail
   */
  @Test
  public void cliRendersAndRejectsInvalidArguments() throws Exception {
    Path prefix = temporary.newFolder("runtime with spaces").toPath();
    ByteArrayOutputStream messages = new ByteArrayOutputStream();
    PrintStream stream = new PrintStream(messages, true, "UTF-8");
    assertEquals(
        0,
        Gateway.run(
            new String[] {"render", "--config", "routes.json", "--prefix", prefix.toString()},
            stream,
            stream));
    assertTrue(Gateway.read(prefix.resolve("nginx.conf")).contains("X-Route-Revision"));
    for (String[] args :
        new String[][] {
          {},
          {"unknown"},
          {"apply"},
          {"rollback"},
          {"render", "--extra", "x"},
          {"render", "--prefix"},
          {"render", "--config", "routes.json", "--config", "routes.json"}
        }) {
      assertEquals(1, Gateway.run(args, stream, stream));
    }
  }

  /**
   * Loads a fresh, mutable copy of the module's checked-in configuration for a test.
   *
   * @return parsed baseline configuration
   * @throws IOException if {@code routes.json} cannot be read
   */
  static JsonObject baseline() throws IOException {
    return JsonParser.parseString(Gateway.read(Paths.get("routes.json"))).getAsJsonObject();
  }

  /**
   * Creates an isolated runtime directory containing a known active-configuration marker.
   *
   * @return directory containing {@code nginx.conf}
   * @throws IOException if the directory or marker file cannot be created
   */
  private Path activePrefix() throws IOException {
    Path prefix = temporary.newFolder().toPath();
    Gateway.atomicWrite(prefix.resolve("nginx.conf"), "active");
    return prefix;
  }
}
