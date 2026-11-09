package com.example.legacy.integration;

import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.junit.Test;

/**
 * Compares compiled Hibernate column metadata and generated DDL with the authoritative PostgreSQL
 * schema.
 *
 * @author Chuck Emmons
 */
public class MappingConstraintIntegrationTest {
  private static final String TYPES =
      "VARCHAR\\(\\d+\\)|BIGSERIAL|BIGINT|INTEGER|BOOLEAN|TIMESTAMP|TEXT|BYTEA";

  /**
   * Loads mappings with Hibernate's bundled DTD resolver and checks metadata/DDL without opening a
   * SessionFactory.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void matchesSqlConstraintsAndGeneratesDdl() throws Exception {
    Map<String, Map<String, String>> definitions = readSchemaColumns();
    Configuration configuration = new Configuration().configure("hibernate.cfg.xml");
    configuration.buildMappings();
    int tables = 0;
    int columns = 0;
    Iterator<?> mappings = configuration.getClassMappings();
    while (mappings.hasNext()) {
      Table table = ((PersistentClass) mappings.next()).getTable();
      assertTrue(
          "Unexpected mapped table " + table.getName(), definitions.containsKey(table.getName()));
      columns += checkColumns(configuration, table, definitions.get(table.getName()));
      tables++;
    }
    assertTrue(
        "All SQL tables must have a checked Hibernate mapping", tables == definitions.size());
    String ddl =
        String.join("\n", configuration.generateSchemaCreationScript(new PostgreSQLDialect()))
            .toLowerCase(Locale.ROOT);
    assertTrue(
        "Generated DDL must retain unbounded TEXT and BYTEA columns",
        ddl.contains("description text not null") && ddl.contains("payload bytea not null"));
    assertTrue(
        "Generated DDL must retain unique business identifiers",
        ddl.contains("username varchar(60) not null unique")
            && ddl.contains("case_number varchar(40) not null unique"));
    for (String table : definitions.keySet()) {
      assertTrue(
          "Existing sequence generator for " + table,
          ddl.contains("create sequence " + table + "_id_seq"));
    }
  }

  /**
   * Reads the column declarations used in this application's SQL schema without connecting to
   * PostgreSQL.
   *
   * @return the values indexed by their keys
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  private static Map<String, Map<String, String>> readSchemaColumns() throws Exception {
    String sql =
        new String(Files.readAllBytes(Paths.get("db/01_schema.sql")), StandardCharsets.UTF_8);
    Matcher tables =
        Pattern.compile("CREATE TABLE IF NOT EXISTS (\\w+)\\s*\\((.*?)\\);", Pattern.DOTALL)
            .matcher(sql);
    Map<String, Map<String, String>> result = new LinkedHashMap<>();
    while (tables.find()) {
      Map<String, String> columns = new LinkedHashMap<>();
      for (String declaration : tables.group(2).split(",\\s*(?=\\w+\\s+(?:" + TYPES + "))")) {
        Matcher column =
            Pattern.compile("\\s*(\\w+)\\s+(?:" + TYPES + ")(.*)", Pattern.DOTALL)
                .matcher(declaration);
        assertTrue("Unsupported SQL column declaration: " + declaration, column.matches());
        columns.put(column.group(1), declaration.trim());
      }
      result.put(tables.group(1), columns);
    }
    assertTrue("All six application tables must be checked", result.size() == 6);
    return result;
  }

  /**
   * Verifies nullability, varchar limits, unique values, schema defaults, and TEXT/BYTEA types
   * after Hibernate binds XML.
   *
   * @param configuration the Hibernate configuration to inspect
   * @param table the Hibernate table metadata to inspect
   * @param definitions the Tiles definitions indexed by name
   * @return the number of columns checked
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws NullPointerException if {@code configuration} or {@code table} or {@code definitions}
   *     is {@code null}
   * @throws NumberFormatException if text expected to contain an integer is not a valid integer
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  private static int checkColumns(
      Configuration configuration, Table table, Map<String, String> definitions) {
    assertTrue("Column count for " + table.getName(), table.getColumnSpan() == definitions.size());
    int count = 0;
    Iterator<?> columns = table.getColumnIterator();
    while (columns.hasNext()) {
      Column column = (Column) columns.next();
      String context = table.getName() + "." + column.getName();
      String definition = definitions.get(column.getName());
      assertTrue("Unexpected mapped column " + context, definition != null);
      boolean primaryKey = definition.contains("PRIMARY KEY");
      boolean required = primaryKey || definition.contains("NOT NULL");
      assertTrue("Nullability mismatch for " + context, column.isNullable() != required);
      if (primaryKey) {
        assertTrue(
            "Primary key mismatch for " + context,
            table.getPrimaryKey() != null
                && table.getPrimaryKey().getColumnSpan() == 1
                && "id".equals(table.getPrimaryKey().getColumn(0).getName()));
      } else {
        assertTrue(
            "Uniqueness mismatch for " + context,
            column.isUnique() == definition.contains("UNIQUE"));
      }
      Matcher length = Pattern.compile("VARCHAR\\((\\d+)\\)").matcher(definition);
      if (length.find()) {
        assertTrue(
            "Varchar length mismatch for " + context,
            column.getLength() == Integer.parseInt(length.group(1)));
      }
      Matcher defaults = Pattern.compile("\\bDEFAULT\\s+(\\S+)").matcher(definition);
      String expectedDefault = defaults.find() ? defaults.group(1) : null;
      assertTrue(
          "Schema default mismatch for " + context,
          Objects.equals(expectedDefault, column.getDefaultValue()));
      if (definition.matches("\\w+\\s+(TEXT|BYTEA).*")) {
        String expectedType = definition.contains("BYTEA") ? "bytea" : "text";
        assertTrue(
            "SQL type mismatch for " + context,
            expectedType.equals(
                column.getSqlType(new PostgreSQLDialect(), configuration.buildMapping())));
      }
      count++;
    }
    return count;
  }
}
