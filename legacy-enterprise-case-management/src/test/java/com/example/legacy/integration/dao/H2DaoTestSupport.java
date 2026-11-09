package com.example.legacy.integration.dao;

import com.example.legacy.db.DatabaseUtil;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;
import org.h2.tools.RunScript;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 * Loads real Hibernate mappings into a private H2 database and seeds it before every DAO test.
 *
 * @author Chuck Emmons
 */
public abstract class H2DaoTestSupport {
  private static String url;
  private static SessionFactory factory;

  /** Test fixture for session. */
  protected Session session;

  private Transaction transaction;

  /**
   * Creates an in-memory schema compatible with production PostgreSQL column/sequence mappings.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @BeforeClass
  public static void createDatabase() throws Exception {
    Class.forName("org.h2.Driver");
    url =
        "jdbc:h2:mem:dao_"
            + UUID.randomUUID().toString().replace("-", "")
            + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
            + "DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";
    try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
      executeSqlResource(connection, "/h2/schema.sql");
    }
    Configuration configuration =
        DatabaseUtil.configureStandaloneConnectionPool(
            new Configuration().configure(), url, "sa", "");
    configuration.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
    configuration.setProperty("hibernate.dialect", H2LegacyDialect.class.getName());
    configuration.setProperty("hibernate.hbm2ddl.auto", "validate");
    factory = configuration.buildSessionFactory();
  }

  /**
   * Restores known test data before opening the current test's independent transaction/session.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Before
  public void seedDatabase() throws Exception {
    try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
      executeSqlResource(connection, "/h2/test-data.sql");
    }
    session = factory.openSession();
    transaction = session.beginTransaction();
  }

  /**
   * Rolls back writes and closes the test-owned session even when a DAO assertion fails.
   *
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @After
  public void rollbackTest() {
    try {
      if (transaction != null && transaction.isActive()) {
        transaction.rollback();
      }
    } finally {
      if (session != null && session.isOpen()) {
        session.close();
      }
    }
  }

  /**
   * Closes the pool and disposes the private in-memory database after each DAO test class.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   */
  @AfterClass
  public static void closeDatabase() throws Exception {
    try {
      if (factory != null) {
        factory.close();
      }
    } finally {
      if (url != null) {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
            Statement sql = connection.createStatement()) {
          sql.execute("SHUTDOWN");
        }
      }
      factory = null;
      url = null;
    }
  }

  /**
   * Flushes actual SQL and clears Hibernate's first-level cache before reload assertions.
   *
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  protected void flushAndClear() {
    session.flush();
    session.clear();
  }

  /**
   * Executes checked-in UTF-8 fixture SQL through H2's script reader.
   *
   * @param connection the JDBC connection used by the operation
   * @param resource the classpath resource name
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  private static void executeSqlResource(Connection connection, String resource) throws Exception {
    try (InputStream stream = H2DaoTestSupport.class.getResourceAsStream(resource)) {
      Assert.assertNotNull("Missing H2 fixture " + resource, stream);
      try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
        RunScript.execute(connection, reader);
      }
    }
  }
}
