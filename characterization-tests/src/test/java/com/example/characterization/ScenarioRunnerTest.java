package com.example.characterization;

import static org.junit.Assert.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests detection of differences that would otherwise falsely certify a migration.
 *
 * @author Chuck Emmons
 */
public class ScenarioRunnerTest {
  @Rule public TemporaryFolder temporary = new TemporaryFolder();

  /**
   * Verifies that ownership changes and unexpected audit fields are reported and the adapter closes.
   *
   * @throws Exception if the temporary report directory cannot be created or reports cannot be read
   */
  @Test
  public void reportsOwnershipAndUnexpectedAuditFieldsAndClosesAdapter() throws Exception {
    StubAdapter adapter = new StubAdapter();
    adapter.actual.put("case.assignee", "another-worker");
    adapter.actual.put("audit.1.actor", "unexpected-actor");
    Path reports = temporary.newFolder().toPath();
    AssertionError failure = assertThrows(AssertionError.class, () -> ScenarioRunner.run(
        "ownership", fixture(), baseline(), adapter, reports));
    assertTrue(failure.getMessage().contains("ownership step 1 case.assignee"));
    assertTrue(failure.getMessage().contains("audit.1.actor"));
    assertTrue(adapter.closed);
    assertTrue(new String(Files.readAllBytes(reports.resolve("ownership-diff.txt")),
        StandardCharsets.UTF_8).contains("another-worker"));
  }

  /**
   * Verifies that an adapter is closed even when its initialization fails.
   *
   * @throws Exception if test setup fails
   */
  @Test
  public void initializationFailuresStillReleaseOwnedEnvironment() throws Exception {
    StubAdapter adapter = new StubAdapter() {
      /**
       * Simulates an environment initialization failure.
       *
       * @param fixture the unused fixture data
       * @throws IllegalStateException always, to exercise cleanup after failed initialization
       */
      @Override public void initialize(Properties fixture) { throw new IllegalStateException("setup failed"); }
    };
    assertThrows(IllegalStateException.class, () -> ScenarioRunner.run(
        "setup", fixture(), baseline(), adapter, temporary.newFolder().toPath()));
    assertTrue(adapter.closed);
  }

  /**
   * Verifies that a missing baseline result prevents a scenario from passing.
   *
   * @throws Exception if test setup fails
   */
  @Test
  public void incompleteBaselineCannotPass() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> ScenarioRunner.run(
        "missing", fixture(), new Properties(), new StubAdapter(), temporary.newFolder().toPath()));
  }

  /**
   * Creates a fixture containing a single assignment command.
   *
   * @return the one-step fixture
   */
  private static Properties fixture() {
    Properties fixture = new Properties();
    fixture.setProperty("steps", "1");
    fixture.setProperty("01.command", "ASSIGN|supervisor|worker");
    return fixture;
  }

  /**
   * Creates the expected successful assignment checkpoint.
   *
   * @return the baseline result and expected assignee
   */
  private static Properties baseline() {
    Properties baseline = new Properties();
    baseline.setProperty("01.result", "OK");
    baseline.setProperty("01.case.assignee", "worker");
    return baseline;
  }

  /**
   * Supplies mutable snapshot data and records whether the runner closes the adapter.
   *
   * @author Chuck Emmons
   */
  private static class StubAdapter implements ImplementationAdapter {
    final Map<String, String> actual = new TreeMap<>();
    boolean closed;
    /** Creates an adapter with a successful assignment snapshot. */
    StubAdapter() {
      actual.put("result", "OK");
      actual.put("case.assignee", "worker");
    }
    /**
     * Accepts fixture initialization without allocating resources.
     *
     * @param fixture the unused fixture data
     */
    @Override public void initialize(Properties fixture) {}

    /**
     * Returns the configured snapshot without executing the command.
     *
     * @param command the unused scenario command
     * @return the mutable snapshot configured by the test
     */
    @Override public Map<String, String> execute(String command) { return actual; }

    /** Records that the runner released this adapter. */
    @Override public void close() { closed = true; }
  }
}
