package com.example.characterization;

import java.nio.file.*;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Runs the same fixtures and baselines for every registered implementation, each in isolation.
 *
 * @author Chuck Emmons
 */
@RunWith(Parameterized.class)
public class CharacterizationIntegrationTest {
  /**
   * Builds scenario cases for the legacy implementation and each service-loaded adapter.
   *
   * @return adapter classes paired with the names of the scenarios to run
   * @throws IllegalStateException if replacement comparison is required but no replacement adapter
   *     is registered
   */
  @Parameterized.Parameters(name = "{0}/{1}")
  public static Collection<Object[]> scenarios() {
    List<Object[]> cases = new ArrayList<>();
    List<Class<? extends ImplementationAdapter>> adapters = new ArrayList<>();
    adapters.add(LegacyServiceAdapter.class);
    for (ImplementationAdapter adapter : ServiceLoader.load(ImplementationAdapter.class)) {
      adapters.add(adapter.getClass());
    }
    if (Boolean.getBoolean("characterization.requireReplacement") && adapters.size() == 1) {
      throw new IllegalStateException("Replacement comparison required, but no adapter is registered");
    }
    for (Class<? extends ImplementationAdapter> adapter : adapters) {
      for (String name : Arrays.asList("login", "lifecycle", "rework", "permissions", "self-approval")) {
        cases.add(new Object[] {adapter, name});
      }
    }
    return cases;
  }

  private final Class<? extends ImplementationAdapter> adapter;
  private final String scenario;

  /**
   * Creates a test case for an implementation and scenario.
   *
   * @param adapter the adapter class to instantiate for the test
   * @param scenario the scenario name used to locate fixtures and baselines
   */
  public CharacterizationIntegrationTest(Class<? extends ImplementationAdapter> adapter, String scenario) {
    this.adapter = adapter;
    this.scenario = scenario;
  }

  /**
   * Runs the selected scenario with account and scenario fixtures against its recorded baseline.
   *
   * @throws Exception if fixture or baseline loading, adapter instantiation, or scenario execution
   *     fails
   */
  @Test
  public void preservesBusinessBehavior() throws Exception {
    Properties fixture = ScenarioRunner.load("/fixtures/accounts.properties");
    fixture.putAll(ScenarioRunner.load("/fixtures/" + scenario + ".properties"));
    Path reports = Paths.get(System.getProperty("characterization.reports", "target/characterization"))
        .resolve(adapter.getSimpleName());
    ScenarioRunner.run(scenario, fixture, ScenarioRunner.load("/baselines/" + scenario + ".properties"),
        adapter.getDeclaredConstructor().newInstance(), reports);
    org.junit.Assert.assertEquals("Scenario report must contain no differences", "",
        new String(Files.readAllBytes(reports.resolve(scenario + "-diff.txt")),
            java.nio.charset.StandardCharsets.UTF_8));
  }
}
