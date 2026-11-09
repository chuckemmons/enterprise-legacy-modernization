package com.example.characterization;

import com.example.legacy.db.DatabaseUtil;
import com.example.legacy.model.Account;
import com.example.legacy.model.AuditEvent;
import com.example.legacy.service.*;
import com.example.legacy.service.dto.*;
import com.example.legacy.service.exception.ApplicationException;
import com.example.legacy.service.transaction.HibernateTransactionTemplate;
import com.example.legacy.util.PasswordsUtil;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import org.h2.tools.RunScript;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Exercises real legacy services and Hibernate transactions with a private database or schema
 * per scenario.
 *
 * @author Chuck Emmons
 */
public final class LegacyServiceAdapter implements ImplementationAdapter {
  private SessionFactory factory;
  private Connection connection;
  private String schema;
  private boolean schemaCreated;
  private boolean h2;
  private Properties fixture;
  private Long caseId;
  private Integer initialVersion;
  private long started;

  /**
   * Creates an isolated H2 database or PostgreSQL schema and seeds fixture accounts.
   *
   * @param fixture the account definitions, shared password, and scenario data
   * @throws Exception if database configuration, schema creation, or account seeding fails
   */
  @Override
  public void initialize(Properties fixture) throws Exception {
    this.fixture = fixture;
    started = System.currentTimeMillis();
    String database = System.getProperty("characterization.database", "h2");
    if (!database.equals("h2") && !database.equals("postgresql")) {
      throw new IllegalArgumentException("Unknown characterization.database: " + database);
    }
    h2 = database.equals("h2");
    String url = h2 ? "jdbc:h2:mem:characterization_" + UUID.randomUUID().toString().replace("-", "")
        + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1"
        : requiredEnvironment("IT_DB_URL");
    if (!h2 && !url.startsWith("jdbc:postgresql:")) {
      throw new IllegalArgumentException("IT_DB_URL must select PostgreSQL");
    }
    String user = h2 ? "sa" : requiredEnvironment("IT_DB_USER");
    String password = h2 ? "" : Optional.ofNullable(System.getenv("IT_DB_PASSWORD")).orElse("");
    connection = DriverManager.getConnection(url, user, password);
    Configuration configuration = DatabaseUtil.configureStandaloneConnectionPool(
        new Configuration().configure(), url, user, password);
    if (h2) {
      configuration.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
      configuration.setProperty("hibernate.dialect", H2LegacyDialect.class.getName());
      try (Reader sql = resource("/h2/schema.sql")) {
        RunScript.execute(connection, sql);
      }
    } else {
      schema = "characterization_" + UUID.randomUUID().toString().replace("-", "");
      try (Statement sql = connection.createStatement()) { sql.execute("create schema " + schema); }
      schemaCreated = true;
      connection.setSchema(schema);
      // Production DDL; this script contains ordinary statements and line comments only.
      StringBuilder ddl = new StringBuilder();
      try (BufferedReader reader = new BufferedReader(resource("/postgresql/01_schema.sql"))) {
        String line;
        while ((line = reader.readLine()) != null) ddl.append(line.replaceAll("--.*$", "")).append('\n');
      }
      try (Statement sql = connection.createStatement()) {
        for (String command : ddl.toString().split(";")) {
          if (!command.trim().isEmpty()) sql.execute(command);
        }
      }
      configuration.setProperty("hibernate.default_schema", schema);
    }
    factory = configuration.buildSessionFactory();
    String hash = PasswordsUtil.hashPassword(fixture.getProperty("password"));
    new HibernateTransactionTemplate(factory).executeWithoutResult(session -> {
      for (String name : fixture.getProperty("accounts").split(",")) {
        String[] spec = fixture.getProperty("account." + name).split("\\|");
        Account account = new Account();
        account.setUsername(name);
        account.setDisplayName(name);
        account.setPasswordHash(hash);
        account.setRole(spec[0]);
        account.setDepartment(spec[1]);
        account.setEnabled(Boolean.parseBoolean(spec[2]));
        account.setCreatedAt(new java.util.Date());
        session.save(account);
      }
    });
  }

  /**
   * Dispatches a fixture command to legacy services and captures the resulting durable state.
   * Business exceptions are recorded using their message keys as the command outcome.
   *
   * @param command the operation, actor, and optional argument separated by pipes
   * @return the normalized snapshot with the command outcome stored under {@code result}
   * @throws Exception if command execution or snapshot collection fails unexpectedly
   * @throws AssertionError if authentication exposes credentials or downloaded content is invalid
   */
  @Override
  public Map<String, String> execute(String command) throws Exception {
    String[] parts = command.split("\\|", -1);
    String operation = parts[0];
    String actor = parts[1];
    String argument = parts.length > 2 ? parts[2] : "";
    String result = "OK";
    CaseService cases = new DefaultCaseService(factory);
    DocumentService documents = new DocumentService(factory);
    try {
      switch (operation) {
        case "LOGIN":
          AuthDto auth = new AuthenticationService(factory).authenticate(AuthDto.builder()
              .username(actor).password(argument.equals("valid") ? fixture.getProperty("password") : "incorrect").build());
          if (auth.getPassword() != null) throw new AssertionError("Credentials leaked by authentication");
          result = auth.getAccount() == null ? "INVALID_CREDENTIALS" : "AUTHENTICATED";
          break;
        case "CREATE":
          CaseDto created = cases.createCase(actor, CreateCaseDto.builder()
              .title(fixture.getProperty("case.title")).description(fixture.getProperty("case.description"))
              .category(fixture.getProperty("case.category")).priority(fixture.getProperty("case.priority"))
              .department(fixture.getProperty("case.department")).build());
          caseId = created.getId();
          initialVersion = created.getVersion();
          break;
        case "ASSIGN":
          cases.assignCase(actor, AssignCaseDto.builder().id(caseId).assignee(argument).build());
          break;
        case "TRANSITION":
          cases.transitionCase(actor, TransitionCaseDto.builder().id(caseId).destination(argument).build());
          break;
        case "UPLOAD":
          byte[] bytes = Base64.getDecoder().decode(fixture.getProperty("document.base64"));
          documents.uploadDocument(actor, UploadDocumentDto.builder().caseId(caseId).declaredSize(bytes.length)
              .document(DocumentDto.builder().originalName(fixture.getProperty("document.name"))
                  .contentType(argument.isEmpty() ? fixture.getProperty("document.type") : argument)
                  .payload(bytes).build()).build());
          break;
        case "UPDATE":
        case "STALE_UPDATE":
          CaseDto item = cases.getCaseDetails("auditor", RecordReferenceDto.builder().id(caseId).build()).getItem();
          cases.updateCase(actor, UpdateCaseDto.builder().id(caseId)
              .version(operation.equals("STALE_UPDATE") ? initialVersion : item.getVersion())
              .title(argument).description(item.getDescription()).priority(item.getPriority()).build());
          break;
        case "DOWNLOAD":
          List<DocumentDto> attachments = cases.getCaseDetails(actor,
              RecordReferenceDto.builder().id(caseId).build()).getDocuments();
          if (attachments.size() != 1) throw new AssertionError("Expected one attachment");
          DocumentDto downloaded = documents.downloadDocument(actor,
              RecordReferenceDto.builder().id(attachments.get(0).getId()).build());
          if (!Arrays.equals(Base64.getDecoder().decode(fixture.getProperty("document.base64")), downloaded.getPayload())) {
            throw new AssertionError("Attachment download differs from input fixture");
          }
          break;
        default: throw new IllegalArgumentException("Unknown operation " + operation);
      }
    } catch (ApplicationException expected) {
      // Stable business outcomes; infrastructure/programming failures are never normalized away.
      result = expected.getMessageKey();
    }
    Map<String, String> snapshot = snapshot();
    snapshot.put("result", result);
    return snapshot;
  }

  /**
   * Collects case, audit, and document state with normalized references and validated timestamps.
   *
   * @return the durable state without generated identifiers or credentials
   * @throws Exception if persisted state cannot be read or document content cannot be hashed
   * @throws AssertionError if timestamps are invalid or audit events reference another case
   */
  private Map<String, String> snapshot() throws Exception {
    Map<String, String> values = new TreeMap<>();
    values.put("case.exists", Boolean.toString(caseId != null));
    values.put("audit.count", "0");
    values.put("document.count", "0");
    long caseCount = new HibernateTransactionTemplate(factory).execute(session ->
        (Long) session.createQuery("select count(id) from ServiceCase").uniqueResult());
    values.put("case.count", Long.toString(caseCount));
    if (caseId == null) {
      long auditCount = new HibernateTransactionTemplate(factory).execute(session ->
          (Long) session.createQuery("select count(id) from AuditEvent").uniqueResult());
      values.put("audit.count", Long.toString(auditCount));
      return values;
    }
    CaseDetails details = new DefaultCaseService(factory).getCaseDetails("auditor",
        RecordReferenceDto.builder().id(caseId).build());
    CaseDto item = details.getItem();
    values.put("case.ref", "case-1");
    values.put("case.title", item.getTitle());
    values.put("case.status", item.getStatus());
    values.put("case.creator", item.getCreatedBy());
    values.put("case.assignee", item.getAssignedTo() == null ? "<unassigned>" : item.getAssignedTo());
    values.put("case.department", item.getDepartment());
    values.put("case.closed", Boolean.toString(item.getClosedAt() != null));
    checkTime(item.getCreatedAt(), started);
    checkTime(item.getUpdatedAt(), item.getCreatedAt().getTime());
    if (item.getClosedAt() != null) checkTime(item.getClosedAt(), item.getCreatedAt().getTime());
    values.put("timestamps.valid", "true");
    List<AuditEvent> events = new HibernateTransactionTemplate(factory).execute(session -> {
      @SuppressWarnings("unchecked")
      List<AuditEvent> found = session.createQuery("from AuditEvent order by id").list();
      return found;
    });
    values.put("audit.count", Integer.toString(events.size()));
    long previous = started;
    for (int i = 0; i < events.size(); i++) {
      AuditEvent event = events.get(i);
      if (!caseId.equals(event.getEntityId())) throw new AssertionError("Audit references another entity");
      checkTime(event.getCreatedAt(), previous);
      previous = event.getCreatedAt().getTime();
      String prefix = "audit." + i + ".";
      values.put(prefix + "entity", event.getEntityType() + ":case-1");
      values.put(prefix + "action", event.getActionName());
      values.put(prefix + "actor", event.getActor());
      values.put(prefix + "details", event.getDetails().replace(item.getCaseNumber(), "case-1"));
    }
    List<DocumentDto> attachments = details.getDocuments();
    values.put("document.count", Integer.toString(attachments.size()));
    for (int i = 0; i < attachments.size(); i++) {
      DocumentDto doc = new DocumentService(factory).downloadDocument("auditor",
          RecordReferenceDto.builder().id(attachments.get(i).getId()).build());
      checkTime(doc.getCreatedAt(), item.getCreatedAt().getTime());
      String prefix = "document." + i + ".";
      values.put(prefix + "case", "case-1");
      values.put(prefix + "name", doc.getOriginalName());
      values.put(prefix + "type", doc.getContentType());
      values.put(prefix + "size", Integer.toString(doc.getContentSize()));
      values.put(prefix + "uploader", doc.getUploadedBy());
      StringBuilder hash = new StringBuilder();
      for (byte b : MessageDigest.getInstance("SHA-256").digest(doc.getPayload())) hash.append(String.format("%02x", b & 0xff));
      values.put(prefix + "sha256", hash.toString());
    }
    return values;
  }

  /**
   * Checks that a persisted timestamp falls between its lower bound and the current time,
   * allowing one second of clock tolerance.
   *
   * @param timestamp the persisted timestamp to validate
   * @param minimum the earliest allowed time in milliseconds since the epoch
   * @throws AssertionError if the timestamp is missing or outside the allowed interval
   */
  private static void checkTime(java.util.Date timestamp, long minimum) {
    if (timestamp == null || timestamp.getTime() < minimum || timestamp.getTime() > System.currentTimeMillis() + 1000) {
      throw new AssertionError("Missing or out-of-order persisted timestamp");
    }
  }

  /**
   * Opens a classpath resource as a UTF-8 reader.
   *
   * @param name the resource path
   * @return a reader that the caller must close
   * @throws IOException if the resource cannot be found
   */
  private static Reader resource(String name) throws IOException {
    InputStream stream = LegacyServiceAdapter.class.getResourceAsStream(name);
    if (stream == null) throw new FileNotFoundException(name);
    return new InputStreamReader(stream, StandardCharsets.UTF_8);
  }

  /**
   * Reads a required, nonblank environment variable.
   *
   * @param name the environment variable name
   * @return the configured value without trimming
   * @throws IllegalArgumentException if the variable is absent or blank
   */
  private static String requiredEnvironment(String name) {
    String value = System.getenv(name);
    if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("Set " + name);
    return value;
  }

  /**
   * Closes Hibernate, shuts down the H2 database or drops the owned PostgreSQL schema,
   * and closes the JDBC connection, including after partial initialization.
   *
   * @throws Exception if database cleanup or resource closure fails
   */
  @Override
  public void close() throws Exception {
    try {
      if (factory != null) factory.close();
    } finally {
      if (connection != null) {
        try {
          if (h2) {
            try (Statement sql = connection.createStatement()) { sql.execute("SHUTDOWN"); }
          } else if (schemaCreated && schema.matches("characterization_[0-9a-f]{32}")) {
            connection.setSchema("public");
            try (Statement sql = connection.createStatement()) { sql.execute("drop schema " + schema + " cascade"); }
          }
        } finally { connection.close(); }
      }
    }
  }
}
