package com.example.legacy.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Properties;
import org.apache.commons.dbcp2.BasicDataSource;
import org.hibernate.HibernateException;
import org.hibernate.connection.ConnectionProvider;

/**
 * Owns a bounded DBCP pool for explicit standalone CLI/test JDBC configuration.
 *
 * @author Chuck Emmons
 */
public final class LocalConnectionProvider implements ConnectionProvider {
  private BasicDataSource dataSource;

  /**
   * Configures an unopened pool; connections are created only when Hibernate requests them.
   *
   * @param properties the Hibernate connection properties
   * @throws NullPointerException if {@code properties} is {@code null}
   */
  @Override
  public void configure(Properties properties) {
    BasicDataSource pool = new BasicDataSource();
    pool.setDriverClassName(
        properties.getProperty("hibernate.connection.driver_class", "org.postgresql.Driver"));
    pool.setUrl(properties.getProperty("hibernate.connection.url"));
    pool.setUsername(properties.getProperty("hibernate.connection.username"));
    pool.setPassword(properties.getProperty("hibernate.connection.password"));
    pool.setMaxTotal(8);
    pool.setMaxIdle(4);
    pool.setMaxWait(Duration.ofSeconds(10));
    pool.setValidationQuery("SELECT 1");
    pool.setValidationQueryTimeout(Duration.ofSeconds(3));
    pool.setTestOnBorrow(true);
    this.dataSource = pool;
  }

  /**
   * Borrows one validated connection from the standalone pool.
   *
   * @return a validated connection borrowed from the standalone pool
   * @throws SQLException if a database operation fails
   */
  @Override
  public Connection getConnection() throws SQLException {
    return dataSource.getConnection();
  }

  /**
   * Returns the connection to its pool when the service's session finishes.
   *
   * @param connection the JDBC connection used by the operation
   * @throws SQLException if a database operation fails
   * @throws NullPointerException if {@code connection} is {@code null}
   */
  @Override
  public void closeConnection(Connection connection) throws SQLException {
    connection.close();
  }

  /**
   * Closes the owned standalone pool when Hibernate closes its session factory.
   *
   * @throws HibernateException if the connection pool cannot be closed
   */
  @Override
  public void close() {
    if (dataSource != null) {
      try {
        dataSource.close();
      } catch (SQLException failure) {
        throw new HibernateException("Could not close standalone connection pool", failure);
      }
    }
  }

  /**
   * Keeps borrowed connections for the normal Hibernate transaction lifecycle.
   *
   * @return {@code false}, because connections remain associated with their sessions
   */
  @Override
  public boolean supportsAggressiveRelease() {
    return false;
  }
}
