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

import com.example.legacy.bootstrap.SeedAdmin;
import com.example.legacy.db.DatabaseUtil;
import com.example.legacy.service.BootstrapService;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/**
 * Behavioral contracts for SeedAdmin.
 *
 * @author Chuck Emmons
 */
public class SeedAdminTest {

  /**
   * Reads administrator environment and closes the factory.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if the observed behavior violates the tested contract
   */
  @Test
  public void readsAdministratorEnvironmentAndClosesTheFactory() throws Exception {
    try (MockedConstruction<BootstrapService> services = mockConstruction(BootstrapService.class);
        MockedStatic<DatabaseUtil> database = mockStatic(DatabaseUtil.class); ) {
      String output =
          tapSystemOut(() ->
              withEnvironmentVariable("CASE_ADMIN_USER", "testadmin")
                  .and("CASE_ADMIN_PASSWORD", "TestPassword!2026")
                  .execute(() -> SeedAdmin.main(new String[0])));
      verify(services.constructed().get(0)).seedAdmin("testadmin", "TestPassword!2026");
      assertTrue(output.contains("testadmin"));
      assertFalse(output.contains("TestPassword!2026"));
      database.verify(DatabaseUtil::closeSessionFactory);
    }
  }

  /**
   * Closes the factory when seeding fails.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void closesTheFactoryWhenSeedingFails() {
    try (MockedConstruction<BootstrapService> services =
            mockConstruction(
                BootstrapService.class,
                (service, context) ->
                    doThrow(new IllegalStateException("failed"))
                        .when(service)
                        .seedAdmin(
                            org.mockito.ArgumentMatchers.nullable(String.class),
                            org.mockito.ArgumentMatchers.nullable(String.class)));
        MockedStatic<DatabaseUtil> database = mockStatic(DatabaseUtil.class); ) {
      assertThrows(IllegalStateException.class, () -> SeedAdmin.main(new String[0]));
      database.verify(DatabaseUtil::closeSessionFactory);
    }
  }
}
