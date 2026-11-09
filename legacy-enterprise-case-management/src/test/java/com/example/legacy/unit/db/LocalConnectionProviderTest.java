package com.example.legacy.unit.db;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.legacy.db.LocalConnectionProvider;
import java.util.Properties;
import org.junit.Test;
import org.mockito.MockedConstruction;

/**
 * Behavioral contracts for LocalConnectionProvider.
 *
 * @author Chuck Emmons
 */
public class LocalConnectionProviderTest {

  /**
   * Configures bounded pool and delegates connection lifecycle.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if the observed behavior violates the tested contract
   */
  @Test
  public void configuresBoundedPoolAndDelegatesConnectionLifecycle() throws Exception {
    java.sql.Connection connection = mock(java.sql.Connection.class);
    try (MockedConstruction<org.apache.commons.dbcp2.BasicDataSource> pools =
        mockConstruction(
            org.apache.commons.dbcp2.BasicDataSource.class,
            (pool, context) -> when(pool.getConnection()).thenReturn(connection)); ) {
      LocalConnectionProvider provider = new LocalConnectionProvider();
      provider.close();
      Properties settings = new Properties();
      settings.setProperty("hibernate.connection.url", "jdbc:test");
      settings.setProperty("hibernate.connection.username", "tester");
      settings.setProperty("hibernate.connection.password", "password");
      provider.configure(settings);
      org.apache.commons.dbcp2.BasicDataSource pool = pools.constructed().get(0);
      verify(pool).setMaxTotal(8);
      verify(pool).setMaxIdle(4);
      verify(pool).setMaxWait(java.time.Duration.ofSeconds(10));
      verify(pool).setTestOnBorrow(true);
      verify(pool).setDriverClassName("org.postgresql.Driver");
      assertSame(connection, provider.getConnection());
      provider.closeConnection(connection);
      verify(connection).close();
      assertFalse(provider.supportsAggressiveRelease());
      provider.close();
      verify(pool).close();
    }
  }

  /**
   * Preserves sql cause when closing the pool fails.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if the observed behavior violates the tested contract
   */
  @Test
  public void preservesSqlCauseWhenClosingThePoolFails() throws Exception {
    java.sql.SQLException cause = new java.sql.SQLException("close failed");
    try (MockedConstruction<org.apache.commons.dbcp2.BasicDataSource> pools =
        mockConstruction(
            org.apache.commons.dbcp2.BasicDataSource.class,
            (pool, context) -> doThrow(cause).when(pool).close()); ) {
      LocalConnectionProvider provider = new LocalConnectionProvider();
      Properties settings = new Properties();
      settings.setProperty("hibernate.connection.driver_class", "test.Driver");
      provider.configure(settings);
      assertSame(
          cause, assertThrows(org.hibernate.HibernateException.class, provider::close).getCause());
      verify(pools.constructed().get(0)).setDriverClassName("test.Driver");
    }
  }
}
