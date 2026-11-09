package com.example.characterization;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Runs a fixture against independently seeded adapters and compares every durable checkpoint.
 *
 * @author Chuck Emmons
 */
public final class ScenarioRunner {
  /** Prevents instantiation of this scenario utility. */
  private ScenarioRunner() {}

  /**
   * Loads UTF-8 properties from a classpath resource.
   *
   * @param resource the resource path, with a leading slash for an absolute classpath location
   * @return the properties read from the resource
   * @throws IOException if the resource is missing or cannot be read
   */
  public static Properties load(String resource) throws IOException {
    Properties values = new Properties();
    try (InputStream stream = ScenarioRunner.class.getResourceAsStream(resource)) {
      if (stream == null) throw new FileNotFoundException(resource);
      try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
        values.load(reader);
      }
    }
    return values;
  }

  /**
   * Executes a scenario, writes checkpoint reports, and compares snapshots against its baseline.
   * Baselines are explicit per-step patches; absent fields retain their prior expected values.
   * The adapter is closed after initialization and execution, including when either fails.
   *
   * @param scenario the scenario name used to name report files
   * @param fixture the step count, commands, and adapter initialization data
   * @param baseline the expected per-step snapshot patches
   * @param adapter the adapter whose environment is owned by this run
   * @param reports the directory in which to write snapshots and differences
   * @throws Exception if initialization, execution, cleanup, or report writing fails
   * @throws IllegalArgumentException if there are no steps or a step lacks a baseline result
   * @throws AssertionError if any actual snapshot differs from its expected snapshot
   */
  public static void run(String scenario, Properties fixture, Properties baseline,
      ImplementationAdapter adapter, Path reports) throws Exception {
    Files.createDirectories(reports);
    Map<String, String> expected = new TreeMap<>();
    List<String> differences = new ArrayList<>();
    int steps = Integer.parseInt(fixture.getProperty("steps"));
    if (steps < 1) throw new IllegalArgumentException("Scenario has no steps");
    try (ImplementationAdapter owned = adapter) {
      owned.initialize(fixture);
      for (int step = 1; step <= steps; step++) {
        String prefix = String.format(Locale.ROOT, "%02d.", step);
        if (!baseline.containsKey(prefix + "result")) {
          throw new IllegalArgumentException("Missing baseline result for " + prefix);
        }
        for (String key : baseline.stringPropertyNames()) {
          if (key.startsWith(prefix)) expected.put(key.substring(prefix.length()), baseline.getProperty(key));
        }
        Map<String, String> actual = owned.execute(
            Objects.requireNonNull(fixture.getProperty(prefix + "command"), "Missing command " + prefix));
        write(reports.resolve(scenario + "-" + prefix + "actual.properties"), actual);
        write(reports.resolve(scenario + "-" + prefix + "expected.properties"), expected);
        Set<String> keys = new TreeSet<>(expected.keySet());
        keys.addAll(actual.keySet());
        for (String key : keys) {
          if (!Objects.equals(expected.get(key), actual.get(key))) {
            differences.add(scenario + " step " + step + " " + key + ": expected <"
                + expected.get(key) + "> but was <" + actual.get(key) + ">");
          }
        }
      }
    }
    Files.write(reports.resolve(scenario + "-diff.txt"), differences, StandardCharsets.UTF_8);
    if (!differences.isEmpty()) throw new AssertionError(String.join("\n", differences));
  }

  /**
   * Writes a normalized snapshot as UTF-8 properties.
   *
   * @param path the destination report file
   * @param snapshot the snapshot entries to serialize
   * @throws IOException if the report cannot be written
   */
  private static void write(Path path, Map<String, String> snapshot) throws IOException {
    Properties properties = new Properties();
    properties.putAll(snapshot);
    try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
      properties.store(writer, "MOD-001 normalized snapshot (no credentials)");
    }
  }
}
