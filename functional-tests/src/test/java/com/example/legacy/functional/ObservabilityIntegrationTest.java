package com.example.legacy.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.legacy.functional.pages.LoginPage;
import com.example.legacy.functional.support.BrowserSession;
import com.example.legacy.functional.support.EmbeddedApplication;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Exercises real servlet wiring, correlation, database probes and administrator-only telemetry.
 *
 * @author Chuck Emmons
 */
public class ObservabilityIntegrationTest {
  /** Embedded application and isolated database shared by the tests in this class. */
  @ClassRule public static final EmbeddedApplication application = new EmbeddedApplication();

  /** Fresh browser session and fixture reset for each test. */
  @Rule public final BrowserSession browser = new BrowserSession(application);

  /**
   * Checks that deployed probes and anonymous redirects identify the actual handling runtime.
   *
   * @throws Exception if opening the HTTP connection or reading a response fails
   * @throws AssertionError if HTTP status, implementation identity or correlation headers differ
   */
  @Test
  public void probesAndRedirectsIdentifyTheHandlingImplementation() throws Exception {
    for (String path :
        new String[] {"/health/live", "/health/ready", "/operations.do?view=events"}) {
      HttpURLConnection connection =
          (HttpURLConnection) new URL(application.getBaseUrl() + path).openConnection();
      connection.setInstanceFollowRedirects(false);
      connection.setConnectTimeout(5000);
      connection.setReadTimeout(10000);
      try {
        assertEquals(path.startsWith("/health") ? 200 : 302, connection.getResponseCode());
        assertEquals("legacy", connection.getHeaderField("X-Implementation"));
        UUID.fromString(connection.getHeaderField("X-Request-ID"));
      } finally {
        connection.disconnect();
      }
    }
  }

  /**
   * Exercises administrator access, dashboard lookup and correlated transaction exports.
   *
   * <p>Submitted password canaries must remain absent from exported events.
   *
   * @throws AssertionError if access, correlation, metrics or credential exclusion checks fail
   * @throws org.openqa.selenium.WebDriverException if browser interaction or dashboard loading
   *     fails
   */
  @Test
  public void dashboardCorrelatesRequestsAndKeepsCredentialsOutOfExports() {
    WebDriver driver = browser.getWebDriver();
    LoginPage login = new LoginPage(driver, application.getBaseUrl());
    login.open().signIn("worker", "NeverLogThisPassword!2026");
    login.open().signIn("worker", "FunctionalPassword!2026");
    driver.get(application.getBaseUrl() + "/operations.do?view=events");
    assertFalse(driver.findElement(By.tagName("body")).getText().contains("\"events\""));
    login.open().signIn("admin", "FunctionalPassword!2026");
    driver.get(application.getBaseUrl() + "/operations.do");
    new WebDriverWait(driver, Duration.ofSeconds(10))
        .until(ignored -> driver.findElements(By.cssSelector("#events tr")).size() > 0);
    assertTrue(driver.findElement(By.id("events")).getText().contains("legacy / 1.0.0-SNAPSHOT"));
    @SuppressWarnings("unchecked")
    Map<String, Object> snapshot =
        (Map<String, Object>)
            ((JavascriptExecutor) driver)
                .executeAsyncScript(
                    "const done = arguments[arguments.length - 1];"
                        + " fetch('operations.do?view=events').then(r =>"
                        + " r.json()).then(done).catch(() => done({events: []}));");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> entries = (List<Map<String, Object>>) snapshot.get("events");
    String authenticationId =
        entries.stream()
            .filter(
                e ->
                    "request".equals(e.get("event"))
                        && "/authenticate.do".equals(e.get("operation")))
            .map(e -> (String) e.get("requestId"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing authentication completion"));
    assertTrue(
        entries.stream()
            .anyMatch(
                e ->
                    "transaction".equals(e.get("event"))
                        && authenticationId.equals(e.get("requestId"))
                        && "legacy".equals(e.get("implementation"))));
    driver.findElement(By.id("request-id")).sendKeys(authenticationId);
    assertTrue(driver.findElement(By.id("events")).getText().contains("transaction"));
    assertTrue(driver.findElement(By.id("events")).getText().contains(authenticationId));
    driver.get(application.getBaseUrl() + "/operations.do?view=events");
    String events = driver.findElement(By.tagName("body")).getText();
    assertFalse(events.contains("NeverLogThisPassword"));
    assertFalse(events.contains("FunctionalPassword"));
    assertTrue(events.contains("\"operation\":\"/authenticate.do\""));
    assertTrue(events.contains("\"event\":\"transaction\""));
    driver.get(application.getBaseUrl() + "/operations.do?view=metrics");
    assertTrue(
        driver.findElement(By.tagName("body")).getText().contains("legacy_duration_seconds_count"));
  }
}
