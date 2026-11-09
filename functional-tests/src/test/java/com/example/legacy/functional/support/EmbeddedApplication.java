package com.example.legacy.functional.support;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.ContextResource;
import org.h2.tools.RunScript;

/**
 * Hosts the staged production WAR on a random loopback port with its own seeded in-memory database.
 *
 * @author Chuck Emmons
 */
public final class EmbeddedApplication extends org.junit.rules.ExternalResource {
  private final String databaseUrl =
      "jdbc:h2:mem:functional_"
          + UUID.randomUUID().toString().replace("-", "")
          + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";
  private Tomcat tomcat;
  private String baseUrl;

  /**
   * Replaces all fixture data before each browser test, including sequence values and account
   * passwords.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   */
  public void resetData() throws Exception {
    runScript("h2/test-data.sql");
  }

  /**
   * Returns the random-port application URL after successful deployment.
   *
   * @return the random-port application URL after successful deployment
   */
  public String getBaseUrl() {
    return baseUrl;
  }

  /**
   * Builds the production schema, binds the H2 datasource and starts Struts, Tiles and JSP
   * processing.
   *
   * @throws Throwable if application setup fails
   */
  @Override
  protected void before() throws Throwable {
    if (System.getenv("DB_URL") != null && !System.getenv("DB_URL").trim().isEmpty()) {
      throw new IllegalStateException(
          "Unset DB_URL: functional tests must use their private H2 datasource");
    }
    try {
      runScript("h2/schema.sql");
      resetData();
      Path build = Paths.get(System.getProperty("functional.build", "build")).toAbsolutePath();
      Files.createDirectories(build);
      Path base = Files.createTempDirectory(build, "tomcat-");
      final Path apps = Files.createDirectories(base.resolve("webapps"));
      tomcat = new Tomcat();
      tomcat.setBaseDir(base.toString());
      tomcat.setHostname("127.0.0.1");
      tomcat.setPort(0);
      tomcat.enableNaming();
      tomcat.getHost().setAppBase(apps.toString());
      tomcat.getConnector().setProperty("address", "127.0.0.1");
      String webapp =
          Paths.get(System.getProperty("functional.webapp", "build/webapp"))
              .toAbsolutePath()
              .toString();
      Context context = tomcat.addWebapp("/cases", webapp);
      context.setParentClassLoader(EmbeddedApplication.class.getClassLoader());
      ContextResource datasource = new ContextResource();
      datasource.setName("jdbc/LegacyCases");
      datasource.setAuth("Container");
      datasource.setType("javax.sql.DataSource");
      datasource.setProperty("factory", "org.apache.tomcat.dbcp.dbcp2.BasicDataSourceFactory");
      datasource.setProperty("driverClassName", "org.h2.Driver");
      datasource.setProperty("url", databaseUrl);
      datasource.setProperty("username", "sa");
      datasource.setProperty("password", "");
      datasource.setProperty("maxTotal", "4");
      context.getNamingResources().addResource(datasource);
      tomcat.start();
      if (!context.getState().isAvailable()) {
        throw new IllegalStateException(
            "Embedded application failed to deploy; inspect Tomcat test output");
      }
      baseUrl = "http://127.0.0.1:" + tomcat.getConnector().getLocalPort() + "/cases";
    } catch (Throwable failure) {
      try {
        stopApplicationAndDatabase();
      } catch (Exception cleanup) {
        failure.addSuppressed(cleanup);
      }
      throw failure;
    }
  }

  /**
   * Stops the application and its Hibernate factory before shutting down the private database.
   *
   * @throws IllegalStateException if the embedded application or its private database cannot be
   *     stopped
   */
  @Override
  protected void after() {
    try {
      stopApplicationAndDatabase();
    } catch (Exception failure) {
      throw new IllegalStateException("Cannot stop functional test fixture", failure);
    }
  }

  /**
   * Executes the checked-in UTF-8 fixture scripts through the same H2 driver used by JNDI.
   *
   * @param resource the classpath resource name
   * @throws Exception if the SQL resource cannot be read or executed
   */
  private void runScript(String resource) throws Exception {
    try (Connection connection = DriverManager.getConnection(databaseUrl, "sa", "");
        InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
      if (input == null) {
        throw new IllegalStateException("Missing fixture " + resource);
      }
      RunScript.execute(connection, new InputStreamReader(input, StandardCharsets.UTF_8));
    }
  }

  /**
   * Cleans up both resources even when deployment or shutdown fails.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   */
  private void stopApplicationAndDatabase() throws Exception {
    try {
      if (tomcat != null) {
        try {
          tomcat.stop();
        } finally {
          tomcat.destroy();
        }
      }
    } finally {
      try (Connection connection = DriverManager.getConnection(databaseUrl, "sa", "")) {
        connection.createStatement().execute("SHUTDOWN");
      } catch (SQLException failure) {
        throw new IllegalStateException("Cannot shut down private H2 database", failure);
      }
    }
  }
}
