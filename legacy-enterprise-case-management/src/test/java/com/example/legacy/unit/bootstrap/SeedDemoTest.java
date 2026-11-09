package com.example.legacy.unit.bootstrap;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut;
import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import com.example.legacy.bootstrap.SeedDemo;
import com.example.legacy.db.DatabaseUtil;
import com.example.legacy.service.BootstrapService;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/**
 * Behavioral contracts for SeedDemo.
 *
 * @author Chuck Emmons
 */
public class SeedDemoTest {

  /**
   * Reads demo password and closes the factory.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if the observed behavior violates the tested contract
   */
  @Test
  public void readsDemoPasswordAndClosesTheFactory() throws Exception {
    try (MockedConstruction<BootstrapService> services = mockConstruction(BootstrapService.class);
        MockedStatic<DatabaseUtil> database = mockStatic(DatabaseUtil.class); ) {
      String output =
          tapSystemOut(() ->
              withEnvironmentVariable("CASE_DEMO_PASSWORD", "TestPassword!2026")
                  .execute(() -> SeedDemo.main(new String[0])));
      verify(services.constructed().get(0)).seedDemo("TestPassword!2026");
      assertTrue(output.contains("eight cases"));
      assertFalse(output.contains("TestPassword!2026"));
      database.verify(DatabaseUtil::closeSessionFactory);
    }
  }

  /**
   * Closes the factory when demo seeding fails.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void closesTheFactoryWhenDemoSeedingFails() {
    try (MockedConstruction<BootstrapService> services =
            mockConstruction(
                BootstrapService.class,
                (service, context) ->
                    doThrow(new IllegalStateException("failed"))
                        .when(service)
                        .seedDemo(org.mockito.ArgumentMatchers.nullable(String.class)));
        MockedStatic<DatabaseUtil> database = mockStatic(DatabaseUtil.class); ) {
      assertThrows(IllegalStateException.class, () -> SeedDemo.main(new String[0]));
      database.verify(DatabaseUtil::closeSessionFactory);
    }
  }
}
