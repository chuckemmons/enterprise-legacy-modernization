package com.example.legacy.integration.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.example.legacy.dao.AccountDao;
import com.example.legacy.dao.AuditDao;
import com.example.legacy.dao.CaseDao;
import com.example.legacy.dao.CaseDocumentDao;
import com.example.legacy.dao.CaseNoteDao;
import com.example.legacy.db.DatabaseUtil;
import com.example.legacy.model.Account;
import com.example.legacy.model.AuditEvent;
import com.example.legacy.model.ServiceCase;
import com.example.legacy.service.AccountAccessService;
import com.example.legacy.service.AuditService;
import com.example.legacy.service.AuthenticationService;
import com.example.legacy.service.BootstrapService;
import com.example.legacy.service.CasePolicyService;
import com.example.legacy.service.CaseService;
import com.example.legacy.service.DefaultCaseService;
import com.example.legacy.service.DocumentService;
import com.example.legacy.service.NotificationService;
import com.example.legacy.service.dto.AddCaseCommentDto;
import com.example.legacy.service.dto.AssignCaseDto;
import com.example.legacy.service.dto.AuthDto;
import com.example.legacy.service.dto.CaseDetails;
import com.example.legacy.service.dto.CaseDto;
import com.example.legacy.service.dto.CreateCaseDto;
import com.example.legacy.service.dto.DocumentDto;
import com.example.legacy.service.dto.NoticeDto;
import com.example.legacy.service.dto.RecordReferenceDto;
import com.example.legacy.service.dto.UpdateCaseDto;
import com.example.legacy.service.dto.UploadDocumentDto;
import com.example.legacy.service.exception.ConcurrentUpdateException;
import com.example.legacy.service.mapper.AccountMapper;
import com.example.legacy.service.mapper.AuditEventMapper;
import com.example.legacy.service.mapper.CaseMapper;
import com.example.legacy.service.mapper.CaseNoteMapper;
import com.example.legacy.service.mapper.DocumentMapper;
import com.example.legacy.service.transaction.HibernateTransactionTemplate;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Exercises production mappings and service transactions in an isolated PostgreSQL schema.
 *
 * @author Chuck Emmons
 */
public class PostgreSqlServiceIntegrationTest {
  private static final String ADMIN = "integration_admin";
  private static final String PASSWORD = "IntegrationPassword!2026";
  private SessionFactory factory;
  private Connection adminConnection;
  private String url;
  private String user;
  private String password;
  private String schema;
  private boolean created;

  /**
   * Fails an audit write after it is scheduled and verifies neither audit nor case commits
   * independently.
   *
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   * @throws ConcurrentUpdateException if submitted data is stale or Hibernate detects an
   *     optimistic-lock conflict
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void verifyAtomicRollback() {
    long beforeCases = countRecords("select count(id) from ServiceCase");
    long beforeAudits = countRecords("select count(id) from AuditEvent");
    IllegalStateException expected =
        new IllegalStateException("Injected audit persistence failure");
    AuditDao failing =
        new AuditDao() {
          /**
           * Schedules the actual Hibernate audit insert and then fails the enclosing use case.
           *
           * @param session the Hibernate session used for persistence operations
           * @param event the audit event to persist
           * @throws IllegalStateException if the injected test failure is triggered
           * @throws NullPointerException if {@code session} is {@code null}
           * @throws org.hibernate.HibernateException if a database query, persistence operation, or
           *     Hibernate session or transaction operation fails
           */
          @Override
          public void save(Session session, AuditEvent event) {
            super.save(session, event);
            throw expected;
          }
        };
    AuditService audit =
        new AuditService(
            new HibernateTransactionTemplate(factory),
            new AccountAccessService(factory),
            failing,
            new AuditEventMapper());
    assertTrue(
        "original injected failure propagates",
        captureFailure(() ->
            createCaseService(new CaseDao(), audit)
                .createCase(ADMIN, createCaseRequest("Must roll back")))
            == expected);
    assertTrue(
        "case and audit writes roll back together in PostgreSQL",
        countRecords("select count(id) from ServiceCase") == beforeCases
            && countRecords("select count(id) from AuditEvent") == beforeAudits);
  }

  /**
   * Builds a generated schema, production mappings, and a seed account before each PostgreSQL test.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Before
  public void setUp() throws Exception {
    url = System.getenv("IT_DB_URL");
    user = System.getenv("IT_DB_USER");
    password = System.getenv("IT_DB_PASSWORD");
    assertNotNull("Set IT_DB_URL to run the optional PostgreSQL integration suite", url);
    assertTrue("IT_DB_URL must select PostgreSQL", url.startsWith("jdbc:postgresql:"));
    assertNotNull("Set IT_DB_USER for the optional PostgreSQL suite", user);
    if (password == null) {
      password = "";
    }
    schema = "enterprise_it_" + UUID.randomUUID().toString().replace("-", "");
    adminConnection = DriverManager.getConnection(url, user, password);
    try (Statement sql = adminConnection.createStatement()) {
      sql.execute("create schema " + schema);
    }
    created = true;
    adminConnection.setSchema(schema);
    String ddl =
        new String(Files.readAllBytes(Paths.get("db/01_schema.sql")), StandardCharsets.UTF_8)
            .replaceAll("(?m)--.*$", "");
    for (String command : ddl.split(";")) {
      if (!command.trim().isEmpty()) {
        try (Statement sql = adminConnection.createStatement()) {
          sql.execute(command);
        }
      }
    }
    Configuration configuration =
        DatabaseUtil.configureStandaloneConnectionPool(
            new Configuration().configure(), url, user, password);
    configuration.setProperty("hibernate.default_schema", schema);
    factory = configuration.buildSessionFactory();
    new BootstrapService(factory).seedAdmin(ADMIN, PASSWORD);
  }

  /**
   * Closes the owned pool and drops only the generated schema, including after failed setup or
   * assertions.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   */
  @After
  public void tearDown() throws Exception {
    try {
      if (factory != null) {
        factory.close();
      }
    } finally {
      if (adminConnection != null) {
        try {
          adminConnection.setSchema("public");
          if (created && schema.matches("enterprise_it_[0-9a-f]{32}")) {
            try (Statement sql = adminConnection.createStatement()) {
              sql.execute("drop schema " + schema + " cascade");
            }
          }
        } finally {
          adminConnection.close();
        }
      }
    }
  }

  /**
   * Reloads case aggregate fields and native PostgreSQL attachment bytes.
   *
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   * @throws ConcurrentUpdateException if submitted data is stale or Hibernate detects an
   *     optimistic-lock conflict
   * @throws com.example.legacy.service.exception.RecordNotFoundException if the requested case does
   *     not exist or is archived
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void persistsMappedAggregate() {
    persistAndReloadAggregate();
  }

  /**
   * Exercises native database constraints separately from display/service checks.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void enforcesPostgreSqlConstraints() throws Exception {
    new DefaultCaseService(factory).createCase(ADMIN, createCaseRequest("Constraint fixture"));
    verifyDatabaseConstraints(url, user, password, schema);
  }

  /**
   * Verifies real SQL version races and independent stale-form rejection.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void detectsConcurrentAndStaleUpdates() throws Exception {
    verifyConcurrentUpdates(
        new DefaultCaseService(factory).createCase(ADMIN, createCaseRequest("Concurrent fixture")));
  }

  /**
   * Captures a runtime failure while preserving its concrete application/Hibernate cause.
   *
   * @param work the operation expected to fail
   * @return the runtime exception raised by the operation
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  private static RuntimeException captureFailure(Runnable work) {
    return assertThrows(RuntimeException.class, work::run);
  }

  /**
   * Creates realistic case input independently for each operation.
   *
   * @param title the case title
   * @return the create case DTO
   */
  private static CreateCaseDto createCaseRequest(String title) {
    return CreateCaseDto.builder()
        .title(title)
        .description("Integration description")
        .category("GENERAL")
        .priority("MEDIUM")
        .department("Operations")
        .dueAt(new java.util.Date())
        .build();
  }

  /**
   * Builds a case service with an explicitly injected DAO and audit collaborator.
   *
   * @param cases the case repository
   * @param audit the audit service collaborator
   * @return the case service
   * @throws NullPointerException if {@code cases} or {@code audit} is {@code null}
   */
  private CaseService createCaseService(CaseDao cases, AuditService audit) {
    return new DefaultCaseService(
        new HibernateTransactionTemplate(factory),
        cases,
        new CaseNoteDao(),
        new CaseDocumentDao(),
        new CaseMapper(),
        new CaseNoteMapper(),
        new DocumentMapper(),
        new AccountMapper(),
        new AccountAccessService(factory),
        new CasePolicyService(),
        audit,
        new NotificationService(factory));
  }

  /**
   * Reads durable counts using a new transaction/session after the tested transaction has
   * completed.
   *
   * @param hql the HQL query to execute
   * @return the number of matching records
   * @throws ConcurrentUpdateException if submitted data is stale or Hibernate detects an
   *     optimistic-lock conflict
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  private long countRecords(String hql) {
    return new HibernateTransactionTemplate(factory)
        .execute(session -> (Long) session.createQuery(hql).uniqueResult());
  }

  /**
   * Persists and reloads a case, note, attachment bytes, and audit events through the real
   * services.
   *
   * @return the case DTO
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   * @throws ConcurrentUpdateException if submitted data is stale or Hibernate detects an
   *     optimistic-lock conflict
   * @throws com.example.legacy.service.exception.RecordNotFoundException if the requested case does
   *     not exist or is archived
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  private CaseDto persistAndReloadAggregate() {
    AuthDto login =
        new AuthenticationService(factory)
            .authenticate(AuthDto.builder().username(ADMIN).password(PASSWORD).build());
    assertTrue(
        "authentication returns a credential-free account",
        login.getAccount() != null && login.getPassword() == null);
    CaseService cases = new DefaultCaseService(factory);
    CaseDto created = cases.createCase(ADMIN, createCaseRequest("Round trip"));
    assertTrue(
        "generated identity/version/number round trip",
        created.getId() != null
            && created.getVersion() != null
            && created.getCaseNumber().startsWith("CASE-"));
    cases.addCaseComment(
        ADMIN, AddCaseCommentDto.builder().id(created.getId()).body("Persistent note").build());
    DocumentDto document =
        DocumentDto.builder()
            .originalName("sample.txt")
            .contentType("text/plain")
            .payload(new byte[] {1, 2, 3})
            .build();
    new DocumentService(factory)
        .uploadDocument(
            ADMIN,
            UploadDocumentDto.builder()
                .caseId(created.getId())
                .document(document)
                .declaredSize(3)
                .build());
    cases.assignCase(ADMIN, AssignCaseDto.builder().id(created.getId()).assignee(ADMIN).build());
    List<NoticeDto> notices = new NotificationService(factory).listNotifications(ADMIN);
    assertTrue(
        "assignment commits a recipient notification with its case/audit update",
        notices.size() == 1);
    new NotificationService(factory)
        .markNotificationRead(
            ADMIN, RecordReferenceDto.builder().id(notices.get(0).getId()).build());
    assertTrue(
        "recipient-scoped notice updates round trip",
        new NotificationService(factory).listNotifications(ADMIN).get(0).isRead());
    CaseDetails loaded =
        cases.getCaseDetails(ADMIN, RecordReferenceDto.builder().id(created.getId()).build());
    assertTrue(
        "mapped case aggregate reloads related rows",
        loaded.getNotes().size() == 1 && loaded.getDocuments().size() == 1);
    DocumentDto downloaded =
        new DocumentService(factory)
            .downloadDocument(
                ADMIN,
                RecordReferenceDto.builder().id(loaded.getDocuments().get(0).getId()).build());
    assertTrue(
        "PostgreSQL BYTEA round trip",
        Arrays.equals(new byte[] {1, 2, 3}, downloaded.getPayload()));
    assertTrue(
        "create/comment/upload/assignment audit events commit with data",
        countRecords("select count(id) from AuditEvent") == 4);
    return loaded.getItem();
  }

  /**
   * Checks PostgreSQL uniqueness, NOT NULL, and foreign-key enforcement independently of Java
   * validation.
   *
   * @param url the JDBC connection URL
   * @param user the user's username
   * @param password the plaintext password
   * @param schema the isolated test schema name
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  private void verifyDatabaseConstraints(String url, String user, String password, String schema)
      throws Exception {
    try (Connection connection = DriverManager.getConnection(url, user, password)) {
      connection.setSchema(schema);
      expectSqlState(
          connection,
          "insert into app_users(username,password_hash,display_name,role,department) select"
              + " username,password_hash,display_name,role,department from app_users limit 1",
          "23505");
      expectSqlState(
          connection,
          "insert into case_notes(case_id,author,body) values(-1,'integration_admin','orphan')",
          "23503");
      expectSqlState(
          connection,
          "insert into case_notes(case_id,author,body) select id,'integration_admin',null from"
              + " service_cases limit 1",
          "23502");
    }
    Account source =
        new HibernateTransactionTemplate(factory)
            .execute(session -> {
              Account loaded =
                  (Account)
                      session
                          .createQuery("from Account where username=:u")
                          .setString("u", ADMIN)
                          .uniqueResult();
              return loaded;
            });
    {
      Account duplicate = new Account();
      duplicate.setUsername(source.getUsername());
      duplicate.setPasswordHash(source.getPasswordHash());
      duplicate.setDisplayName(source.getDisplayName());
      duplicate.setRole(source.getRole());
      duplicate.setDepartment(source.getDepartment());
      duplicate.setEnabled(true);
      duplicate.setCreatedAt(new java.util.Date());
      RuntimeException rejected =
          captureFailure(() -> {
            // A separate template verifies commit/flush errors trigger the same rollback lifecycle.
            new HibernateTransactionTemplate(factory)
                .executeWithoutResult(other -> new AccountDao().save(other, duplicate));
          });
      assertTrue(
          "Hibernate reports the database uniqueness constraint",
          rejected instanceof org.hibernate.exception.ConstraintViolationException);
    }
  }

  /**
   * Executes a direct SQL constraint violation and checks its PostgreSQL SQLSTATE.
   *
   * @param connection the JDBC connection used by the operation
   * @param sql the SQL statement to execute
   * @param state the expected SQLSTATE code
   * @throws SQLException if a database operation fails
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws NullPointerException if {@code connection} or {@code state} is {@code null}
   */
  private static void expectSqlState(Connection connection, String sql, String state)
      throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.executeUpdate(sql);
    } catch (SQLException failure) {
      assertTrue(
          "expected PostgreSQL constraint " + state + ", got " + failure.getSQLState(),
          state.equals(failure.getSQLState()));
      return;
    }
    throw new AssertionError("Database accepted invalid data for SQLSTATE " + state);
  }

  /**
   * Forces two service transactions to read one version before either updates, exercising
   * Hibernate's SQL version check.
   *
   * @param original the case snapshot used by the concurrent update checks
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  private void verifyConcurrentUpdates(CaseDto original) throws Exception {
    CyclicBarrier bothRead = new CyclicBarrier(2);
    CaseDao synchronizedReads =
        new CaseDao() {
          /**
           * Coordinates the test threads after each has loaded its own managed entity snapshot.
           *
           * @param session the Hibernate session used for persistence operations
           * @param id the persistent record identifier
           * @return the service case
           * @throws IllegalStateException if the injected test failure is triggered
           * @throws NullPointerException if {@code session} is {@code null}
           * @throws org.hibernate.HibernateException if a database query, persistence operation, or
           *     Hibernate session or transaction operation fails
           */
          @Override
          public ServiceCase findCaseById(Session session, long id) {
            ServiceCase item = super.findCaseById(session, id);
            try {
              bothRead.await(10, TimeUnit.SECONDS);
            } catch (Exception failure) {
              throw new IllegalStateException(failure);
            }
            return item;
          }
        };
    CaseService cases = createCaseService(synchronizedReads, new AuditService(factory));
    ExecutorService executor = Executors.newFixedThreadPool(2);
    List<Future<Object>> results = new ArrayList<>();
    try {
      for (String title : new String[] {"Concurrent first", "Concurrent second"}) {
        UpdateCaseDto request =
            UpdateCaseDto.builder()
                .id(original.getId())
                .version(original.getVersion())
                .title(title)
                .description(original.getDescription())
                .priority(original.getPriority())
                .build();
        results.add(
            executor.submit(() -> {
              try {
                return cases.updateCase(ADMIN, request);
              } catch (RuntimeException failure) {
                return failure;
              }
            }));
      }
      int successes = 0;
      int conflicts = 0;
      for (Future<Object> result : results) {
        Object value = result.get(20, TimeUnit.SECONDS);
        if (value instanceof CaseDto) {
          successes++;
        } else if (value instanceof ConcurrentUpdateException) {
          assertTrue(
              "database version conflict retains its Hibernate cause",
              ((ConcurrentUpdateException) value).getCause()
                  instanceof org.hibernate.StaleStateException);
          conflicts++;
        } else {
          throw new AssertionError("Unexpected concurrency result", (Throwable) value);
        }
      }
      assertTrue("exactly one concurrent update commits", successes == 1 && conflicts == 1);
    } finally {
      executor.shutdownNow();
      executor.awaitTermination(10, TimeUnit.SECONDS);
    }
    CaseDto winner =
        new DefaultCaseService(factory)
            .getCaseDetails(ADMIN, RecordReferenceDto.builder().id(original.getId()).build())
            .getItem();
    assertTrue(
        "committed version increments exactly once",
        winner.getVersion() == original.getVersion() + 1);
    assertTrue(
        "losing update cannot leave a committed audit event",
        countRecords("select count(id) from AuditEvent where actionName='UPDATE'") == 1);
    RuntimeException stale =
        captureFailure(() ->
            new DefaultCaseService(factory)
                .updateCase(
                    ADMIN,
                    UpdateCaseDto.builder()
                        .id(original.getId())
                        .version(original.getVersion())
                        .title("Old form")
                        .description("Old form")
                        .priority("MEDIUM")
                        .build()));
    assertTrue(
        "submitted stale forms fail before mutation independently of commit-time version checking",
        stale instanceof ConcurrentUpdateException && stale.getCause() == null);
  }
}
