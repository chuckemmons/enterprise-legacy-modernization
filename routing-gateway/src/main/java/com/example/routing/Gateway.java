package com.example.routing;

import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Command-line entry point for generating, applying and rolling back NGINX routing.
 *
 * @author Chuck Emmons
 */
public final class Gateway {
  /** Prevents instantiation of this command-line utility. */
  private Gateway() {}

  /**
   * Runs the routing tool and exits with its status code.
   *
   * @param args action and optional configuration, runtime-directory and NGINX arguments
   */
  public static void main(String[] args) {
    System.exit(run(args, System.out, System.err));
  }

  /**
   * Executes one routing command and reports expected failures without a stack trace.
   *
   * @param args command-line action and option pairs
   * @param output destination for successful command messages
   * @param errors destination for validation, filesystem and process failure messages
   * @return zero on success or one on a handled failure; interruption restores the interrupt flag
   */
  static int run(String[] args, PrintStream output, PrintStream errors) {
    try {
      if (args.length == 0 || !Arrays.asList("render", "apply", "rollback").contains(args[0])) {
        throw new IllegalArgumentException(
            "Usage: java -jar routing-gateway.jar render|apply|rollback "
                + "[--config path] [--prefix directory] [--nginx executable]");
      }
      Map<String, String> options = new HashMap<>();
      for (int index = 1; index < args.length; index += 2) {
        if (index + 1 >= args.length
            || !Arrays.asList("--config", "--prefix", "--nginx").contains(args[index])
            || options.put(args[index], args[index + 1]) != null) {
          throw new IllegalArgumentException("Unknown, repeated or incomplete option");
        }
      }
      Path prefix =
          Paths.get(options.getOrDefault("--prefix", "routing-gateway/target/runtime"))
              .toAbsolutePath()
              .normalize();
      String candidate = null;
      if (!args[0].equals("rollback")) {
        Path config = Paths.get(options.getOrDefault("--config", "routing-gateway/routes.json"));
        candidate = NginxConfig.render(RouteConfig.parse(read(config)));
      }
      if (args[0].equals("render")) {
        render(prefix, candidate);
      } else {
        if (!options.containsKey("--nginx")) {
          throw new IllegalArgumentException("--nginx is required for apply/rollback");
        }
        Path binary = Paths.get(options.get("--nginx")).toAbsolutePath().normalize();
        switchConfig(prefix, candidate, arguments -> nginx(binary, prefix, arguments));
      }
      output.println(
          args[0] + " complete: " + prefix + "; verify X-Route-Revision and backend telemetry");
      return 0;
    } catch (JsonParseException failure) {
      errors.println("Invalid JSON configuration");
    } catch (IllegalArgumentException | IOException failure) {
      errors.println(failure.getClass().getSimpleName() + ": " + failure.getMessage());
    } catch (InterruptedException failure) {
      Thread.currentThread().interrupt();
      errors.println("Routing command interrupted");
    }
    return 1;
  }

  /**
   * Creates the runtime directories required by NGINX logging and temporary request storage.
   *
   * @param prefix runtime directory to prepare
   * @throws IOException if a required directory cannot be created
   */
  static void prepare(Path prefix) throws IOException {
    for (String directory : Arrays.asList("logs", "temp/client", "temp/proxy")) {
      Files.createDirectories(prefix.resolve(directory));
    }
  }

  /**
   * Installs a rendered configuration for a stopped gateway while holding the switch lock.
   *
   * @param prefix gateway runtime directory
   * @param candidate complete NGINX configuration text
   * @throws IOException if the lock cannot be acquired, a PID file exists, or file operations fail
   */
  static void render(Path prefix, String candidate) throws IOException {
    prepare(prefix);
    Path lock = prefix.resolve("switch.lock");
    Files.createFile(lock);
    try {
      if (Files.exists(prefix.resolve("nginx.pid"))) {
        throw new IOException("Use apply for a running gateway");
      }
      atomicWrite(prefix.resolve("nginx.conf"), candidate);
    } finally {
      Files.delete(lock);
    }
  }

  /**
   * Validates and installs a configuration, signals reload and retains the previous configuration.
   *
   * <p>A null candidate selects rollback. Identical content leaves the backup unchanged. A failed
   * reload command triggers restoration of the active file; successful signaling still requires
   * deployment verification because NGINX reload is asynchronous.
   *
   * @param prefix gateway runtime directory
   * @param candidate desired configuration, or {@code null} to restore {@code previous.conf}
   * @param command collaborator that validates configuration and signals NGINX
   * @throws IOException if locking, file operations, validation or reload signaling fails
   * @throws InterruptedException if an NGINX command is interrupted
   */
  static void switchConfig(Path prefix, String candidate, NginxCommand command)
      throws IOException, InterruptedException {
    prepare(prefix);
    Path lock = prefix.resolve("switch.lock");
    Files.createFile(lock);
    try {
      Path active = prefix.resolve("nginx.conf");
      Path previous = prefix.resolve("previous.conf");
      String old = read(active);
      String next = candidate == null ? read(previous) : candidate;
      if (next.equals(old)) {
        return;
      }
      atomicWrite(prefix.resolve("candidate.conf"), next);
      command.execute("-t", "-c", "candidate.conf");
      atomicWrite(active, next);
      try {
        command.execute("-c", "nginx.conf", "-s", "reload");
      } catch (IOException | InterruptedException failure) {
        try {
          atomicWrite(active, old);
        } catch (IOException restoreFailure) {
          failure.addSuppressed(restoreFailure);
        }
        throw failure;
      }
      atomicWrite(previous, old);
    } finally {
      Files.delete(lock);
    }
  }

  /**
   * Invokes NGINX without a shell and captures diagnostics in {@code nginx-command.log}.
   *
   * @param binary NGINX executable path
   * @param prefix runtime directory supplied through the NGINX {@code -p} option
   * @param arguments additional NGINX command arguments
   * @throws IOException if the process cannot start, exceeds fifteen seconds or exits
   *     unsuccessfully
   * @throws InterruptedException if waiting for the process is interrupted
   */
  static void nginx(Path binary, Path prefix, String... arguments)
      throws IOException, InterruptedException {
    ArrayList<String> command =
        new ArrayList<>(
            Arrays.asList(binary.toString(), "-p", prefix.toString().replace('\\', '/') + "/"));
    command.addAll(Arrays.asList(arguments));
    Path diagnostic = prefix.resolve("nginx-command.log");
    Process process =
        new ProcessBuilder(command)
            .redirectErrorStream(true)
            .redirectOutput(diagnostic.toFile())
            .start();
    try {
      if (!process.waitFor(15, TimeUnit.SECONDS)) {
        throw new IOException("NGINX command timed out; inspect " + diagnostic);
      }
      if (process.exitValue() != 0) {
        throw new IOException("NGINX command failed; inspect " + diagnostic);
      }
    } finally {
      if (process.isAlive()) {
        process.destroyForcibly();
      }
    }
  }

  /**
   * Reads a complete UTF-8 configuration file.
   *
   * @param path file to read
   * @return decoded file contents
   * @throws IOException if the file cannot be read
   */
  static String read(Path path) throws IOException {
    return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
  }

  /**
   * Replaces a file using a UTF-8 temporary file and an atomic move within its directory.
   *
   * @param path destination file whose parent directory already exists
   * @param content complete replacement text
   * @throws IOException if writing, atomic replacement or temporary-file cleanup fails
   */
  static void atomicWrite(Path path, String content) throws IOException {
    Path temporary = Files.createTempFile(path.getParent(), "routing-", ".conf");
    try {
      Files.write(temporary, content.getBytes(StandardCharsets.UTF_8));
      // Fail closed if the filesystem cannot provide atomic replacement.
      Files.move(
          temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  /**
   * Supplies NGINX command execution independently of configuration-switch file operations.
   *
   * @author Chuck Emmons
   */
  @FunctionalInterface
  interface NginxCommand {
    /**
     * Executes one NGINX validation or control command.
     *
     * @param arguments NGINX command arguments
     * @throws IOException if command execution fails
     * @throws InterruptedException if execution is interrupted
     */
    void execute(String... arguments) throws IOException, InterruptedException;
  }
}
