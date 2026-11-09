package com.example.legacy.integration;

import static org.junit.Assert.assertTrue;

import com.example.legacy.domain.Priority;
import com.example.legacy.domain.Role;
import com.example.legacy.util.MessageKeys;
import com.example.legacy.util.MessagesUtil;
import com.example.legacy.util.WorkflowUtil;
import com.example.legacy.web.ActionOperation;
import com.example.legacy.web.PageView;
import com.example.legacy.web.Routes;
import com.example.legacy.web.ViewOptionsUtil;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Checks shared symbols against external framework configuration and the application's message
 * bundle.
 *
 * @author Chuck Emmons
 */
public class ApplicationContractIntegrationTest {

  /**
   * Ensures every supported route/operation and typed page remains declared to Struts/Tiles.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void declaresSupportedRoutesAndTilesViews() throws Exception {
    Document struts = parseWebappXml("struts-config.xml");
    Set<String> routes = new HashSet<>();
    Set<String> operations = new HashSet<>();
    for (ActionOperation operation : ActionOperation.values()) {
      operations.add(operation.getParameter());
    }
    NodeList actions = struts.getElementsByTagName("action");
    for (int i = 0; i < actions.getLength(); i++) {
      Element action = (Element) actions.item(i);
      routes.add(action.getAttribute("path") + ".do");
      if (action.hasAttribute("parameter")) {
        assertTrue(
            "Struts operation has a shared definition",
            operations.contains(action.getAttribute("parameter")));
      }
    }
    assertTrue(
        "route constants match all configured Struts actions",
        routes.equals(new HashSet<>(Routes.getPaths().values())));
    NodeList definitions = parseWebappXml("tiles-defs.xml").getElementsByTagName("definition");
    Set<String> pages = new HashSet<>();
    for (int i = 0; i < definitions.getLength(); i++) {
      Element definition = (Element) definitions.item(i);
      if (definition.getAttribute("name").startsWith("page.")) {
        pages.add(definition.getAttribute("name"));
      }
      NodeList attributes = definition.getElementsByTagName("put");
      for (int j = 0; j < attributes.getLength(); j++) {
        Element attribute = (Element) attributes.item(j);
        assertTrue(
            "Tiles titles use message keys", !"title".equals(attribute.getAttribute("name")));
        if ("titleKey".equals(attribute.getAttribute("name"))) {
          assertTrue(
              "Tiles title key resolves",
              !MessagesUtil.formatMessage(attribute.getAttribute("value")).isEmpty());
        }
      }
    }
    Set<String> typedPages = new HashSet<>();
    for (PageView page : PageView.values()) {
      typedPages.add(page.getDefinition());
    }
    assertTrue("PageView exactly matches configured Tiles pages", pages.equals(typedPages));
  }

  /**
   * Resolves every Java/JSP message key and verifies parameterized feedback retains its values.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void resolvesApplicationMessageKeys() throws Exception {
    ResourceBundle bundle = ResourceBundle.getBundle(MessagesUtil.BUNDLE, Locale.ENGLISH);
    for (Field field : MessageKeys.class.getFields()) {
      assertTrue(
          "Java message key resolves: " + field.getName(),
          bundle.containsKey((String) field.get(null)));
    }
    Pattern literalKey = Pattern.compile("<bean:message\\s+key=[\"']([^\"']+)[\"']");
    try (Stream<Path> paths = Files.walk(Paths.get("src/main/webapp/WEB-INF/views"))) {
      Iterator<Path> iterator = paths.filter(path -> path.toString().endsWith(".jsp")).iterator();
      while (iterator.hasNext()) {
        Path path = iterator.next();
        String source = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        Matcher matcher = literalKey.matcher(source);
        while (matcher.find()) {
          String key = matcher.group(1);
          if (!key.startsWith("${")) {
            assertTrue("JSP key resolves: " + path + " / " + key, bundle.containsKey(key));
          }
        }
        assertTrue(
            "domain choices come from enum values: " + path, !source.contains("<c:forTokens"));
      }
    }
    assertTrue(
        "parameterized business message preserves its limit",
        "Field exceeds 180 characters"
            .equals(MessagesUtil.formatMessage(MessageKeys.BUSINESS_FIELD_TOO_LONG, 180)));
    assertTrue(
        "notification template preserves generated identity",
        "Case assigned: CASE-000001"
            .equals(MessagesUtil.formatMessage(MessageKeys.NOTICE_CASE_ASSIGNED, "CASE-000001")));
    assertTrue(
        "redirect confirmation resolves to display text",
        "Case created"
            .equals(
                MessagesUtil.formatMessage(Locale.ENGLISH, MessageKeys.CONFIRMATION_CASE_CREATED)));
  }

  /**
   * Confirms dropdown snapshots cannot change the domain choices used by later requests.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void protectsDomainChoicesFromMutation() {
    ViewOptionsUtil options = new ViewOptionsUtil();
    String[] priorities = options.getPriorities();
    priorities[0] = "MUTATED";
    assertTrue(
        "priority options are defensive snapshots",
        Priority.LOW.name().equals(options.getPriorities()[0]));
    assertTrue(
        "workflow destinations are offered statuses",
        new HashSet<>(Arrays.asList(options.getStatuses()))
            .containsAll(WorkflowUtil.getNextStatuses("NEW", "ADMIN")));
    assertTrue(
        "unknown roles remain unsupported",
        !Role.ADMIN.matchesCode(null) && !Role.WORKER.matchesCode("UNKNOWN"));
    assertTrue(
        "creation defaults retain existing external codes",
        "GENERAL".equals(options.getDefaultCategory())
            && "MEDIUM".equals(options.getDefaultPriority()));
  }

  /**
   * Parses application configuration without requesting remote DTDs.
   *
   * @param name the configuration filename under {@code WEB-INF}
   * @return the parsed XML document
   * @throws Exception if test setup, execution, or cleanup fails
   */
  private static Document parseWebappXml(String name) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    return factory.newDocumentBuilder().parse(new File("src/main/webapp/WEB-INF/" + name));
  }
}
