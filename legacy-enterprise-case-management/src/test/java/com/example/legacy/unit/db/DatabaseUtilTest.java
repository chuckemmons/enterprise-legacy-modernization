package com.example.legacy.unit.db;

import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.legacy.db.DatabaseUtil;
import java.util.Properties;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.Test;
import org.mockito.MockedConstruction;

/**
 * Behavioral contracts for DatabaseUtil.
 *
 * @author Chuck Emmons
 */
public class DatabaseUtilTest {

  /**
   * Configures standalone defaults and explicit credentials without opening connections.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void configuresStandaloneDefaultsAndExplicitCredentialsWithoutOpeningConnections() {
    Configuration configuration = new Configuration();
    configuration.setProperty("hibernate.connection.datasource", "java:CaseDb");
    assertSame(
        configuration,
        DatabaseUtil.configureStandaloneConnectionPool(configuration, "jdbc:test", null, null));
    assertNull(configuration.getProperty("hibernate.connection.datasource"));
    assertEquals("legacy_app", configuration.getProperty("hibernate.connection.username"));
    assertEquals("", configuration.getProperty("hibernate.connection.password"));
    DatabaseUtil.configureStandaloneConnectionPool(configuration, "jdbc:other", "user", "password");
    assertEquals("user", configuration.getProperty("hibernate.connection.username"));
    assertEquals("password", configuration.getProperty("hibernate.connection.password"));
    assertThrows(
        NullPointerException.class,
        () -> DatabaseUtil.configureStandaloneConnectionPool(configuration, null, null, null));
  }

  /**
   * Loads configured environment and caches until the factory is closed.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if the observed behavior violates the tested contract
   */
  @Test
  public void loadsConfiguredEnvironmentAndCachesUntilTheFactoryIsClosed() throws Exception {
    SessionFactory factory = mock(SessionFactory.class);
    Properties values = new Properties();
    try (MockedConstruction<Configuration> configurations =
        mockConstruction(
            Configuration.class,
            (value, context) -> {
              when(value.configure(anyString())).thenReturn(value);
              when(value.getProperties()).thenReturn(values);
              when(value.buildSessionFactory()).thenReturn(factory);
            }); ) {
      withEnvironmentVariable("DB_URL", "jdbc:test")
          .and("DB_USER", "tester")
          .and("DB_PASSWORD", "test-password")
          .execute(() -> {
            assertSame(factory, DatabaseUtil.getSessionFactory());
            assertSame(factory, DatabaseUtil.getSessionFactory());
            assertEquals(1, configurations.constructed().size());
            verify(configurations.constructed().get(0))
                .setProperty("hibernate.connection.url", "jdbc:test");
            DatabaseUtil.closeSessionFactory();
            assertSame(factory, DatabaseUtil.getSessionFactory());
            assertEquals(2, configurations.constructed().size());
            DatabaseUtil.closeSessionFactory();
            DatabaseUtil.closeSessionFactory();
          });
      verify(factory, times(2)).close();
    }
  }

  /**
   * Keeps jndi configuration when standalone url is missing or empty.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if the observed behavior violates the tested contract
   */
  @Test
  public void keepsJndiConfigurationWhenStandaloneUrlIsMissingOrEmpty() throws Exception {
    try (MockedConstruction<Configuration> configurations =
        mockConstruction(
            Configuration.class,
            (value, context) -> when(value.configure(anyString())).thenReturn(value)); ) {
      withEnvironmentVariable("DB_URL", null)
          .execute(() -> {
            Configuration result = DatabaseUtil.createConfiguration();
            assertSame(configurations.constructed().get(0), result);
          });
      withEnvironmentVariable("DB_URL", "").execute(DatabaseUtil::createConfiguration);
      for (Configuration configuration : configurations.constructed()) {
        verify(configuration, never()).setProperty(eq("hibernate.connection.url"), anyString());
      }
    }
  }
}
