package com.example.legacy.db;

import com.example.legacy.util.EnvironmentKeys;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Owns shared Hibernate session-factory configuration and shutdown; services own sessions and
 * transactions.
 *
 * @author Chuck Emmons
 */
public final class DatabaseUtil {
  private static final String CONFIGURATION = "hibernate.cfg.xml";
  private static final String CONNECTION_URL = "hibernate.connection.url";
  private static final String CONNECTION_USERNAME = "hibernate.connection.username";
  private static final String CONNECTION_PASSWORD = "hibernate.connection.password";
  private static volatile SessionFactory sessionFactory;

  /** Prevents instantiation of this static utility class. */
  private DatabaseUtil() {}

  /**
   * Returns the shared session factory, creating it on first use from {@code hibernate.cfg.xml}.
   *
   * <p>A nonempty {@code DB_URL} selects the standalone connection pool; {@code DB_USER} and {@code
   * DB_PASSWORD} then supply its credentials.
   *
   * @return the shared session factory
   * @throws org.hibernate.HibernateException if the configuration cannot be loaded or the session
   *     factory cannot be built
   */
  public static synchronized SessionFactory getSessionFactory() {
    if (sessionFactory == null) {
      Configuration configuration = createConfiguration();
      sessionFactory = configuration.buildSessionFactory();
    }
    return sessionFactory;
  }

  /**
   * Builds a new Hibernate configuration with the shared mappings.
   *
   * <p>The configuration uses JNDI unless a nonempty {@code DB_URL} environment variable selects
   * the standalone JDBC pool.
   *
   * @return a new Hibernate configuration using JNDI or the standalone connection settings
   * @throws org.hibernate.HibernateException if the Hibernate configuration cannot be loaded
   */
  public static Configuration createConfiguration() {
    Configuration configuration = new Configuration().configure(CONFIGURATION);
    String jdbcUrl = System.getenv(EnvironmentKeys.DB_URL);
    if (jdbcUrl != null && !jdbcUrl.isEmpty()) {
      configureStandaloneConnectionPool(
          configuration,
          jdbcUrl,
          System.getenv(EnvironmentKeys.DB_USER),
          System.getenv(EnvironmentKeys.DB_PASSWORD));
    }
    return configuration;
  }

  /**
   * Configures the supplied Hibernate configuration to use a bounded standalone JDBC pool.
   *
   * <p>The configuration is updated in place and its JNDI datasource setting is removed. This
   * method does not open the pool.
   *
   * @param configuration the Hibernate configuration to update in place
   * @param url the standalone JDBC connection URL
   * @param username the database username, or {@code null} to use {@code "legacy_app"}
   * @param password the database password, or {@code null} to use an empty password
   * @return the supplied configuration, updated to use the standalone pool
   * @throws NullPointerException if {@code configuration} or {@code url} is {@code null}
   */
  public static Configuration configureStandaloneConnectionPool(
      Configuration configuration, String url, String username, String password) {
    configuration.getProperties().remove("hibernate.connection.datasource");
    configuration.setProperty(
        "hibernate.connection.provider_class", LocalConnectionProvider.class.getName());
    configuration.setProperty(CONNECTION_URL, java.util.Objects.requireNonNull(url, "url"));
    configuration.setProperty(CONNECTION_USERNAME, username == null ? "legacy_app" : username);
    configuration.setProperty(CONNECTION_PASSWORD, password == null ? "" : password);
    return configuration;
  }

  /**
   * Closes the shared session factory if it is initialized and clears its reference after a
   * successful close.
   *
   * <p>A later call to {@link #getSessionFactory()} creates a new session factory.
   *
   * @throws org.hibernate.HibernateException if the session factory cannot be closed
   */
  public static synchronized void closeSessionFactory() {
    if (sessionFactory != null) {
      sessionFactory.close();
      sessionFactory = null;
    }
  }
}
