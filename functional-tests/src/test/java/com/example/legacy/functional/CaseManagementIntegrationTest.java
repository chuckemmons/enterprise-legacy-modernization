package com.example.legacy.functional;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.legacy.functional.pages.BasePage;
import com.example.legacy.functional.pages.CaseDetailsPage;
import com.example.legacy.functional.pages.CasesPage;
import com.example.legacy.functional.pages.LoginPage;
import com.example.legacy.functional.pages.NewCasePage;
import com.example.legacy.functional.pages.NotificationsPage;
import com.example.legacy.functional.pages.ReportsPage;
import com.example.legacy.functional.pages.UsersPage;
import com.example.legacy.functional.support.BrowserSession;
import com.example.legacy.functional.support.EmbeddedApplication;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Exercises real JSP submissions through Struts, services, Hibernate and a private seeded H2
 * database.
 *
 * @author Chuck Emmons
 */
public class CaseManagementIntegrationTest {
  private static final String PASSWORD = "FunctionalPassword!2026";

  /** Application lifecycle shared by this test class. */
  @ClassRule public static final EmbeddedApplication application = new EmbeddedApplication();

  /** Browser lifecycle for each test. */
  @Rule public final BrowserSession browser = new BrowserSession(application);

  /**
   * Verifies protected-page redirects, failed authentication, successful login and session
   * invalidation.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void showsLoginErrorsAndProtectsPagesAfterLogout() {
    LoginPage login =
        new LoginPage(browser.getWebDriver(), application.getBaseUrl()).openProtectedPage();
    assertTrue(browser.getWebDriver().getCurrentUrl().endsWith("/login.do"));
    BasePage result = login.signIn("worker", "incorrect-password");
    assertTrue(result.getValidationErrors().contains("Invalid credentials"));
    login.open().signIn("worker", PASSWORD);
    assertTrue(browser.getWebDriver().getCurrentUrl().contains("/dashboard.do"));
    result.logout().openProtectedPage();
    assertTrue(browser.getWebDriver().getCurrentUrl().endsWith("/login.do"));
  }

  /**
   * Checks translated accessible names and explicit label associations on every editable page.
   *
   * @throws Exception if creating or submitting the rejected upload fixture fails
   * @throws AssertionError if a control has no translated accessible label or a unique identifier
   */
  @Test
  public void associatesTranslatedLabelsWithFormControls() throws Exception {
    WebDriver driver = browser.getWebDriver();
    final LoginPage login = new LoginPage(driver, application.getBaseUrl()).open();
    assertAccessibleFormControls();
    assertEquals("Username", driver.findElement(By.id("login-username")).getAccessibleName());
    assertEquals("Password", driver.findElement(By.id("login-password")).getAccessibleName());
    login.signIn("admin", PASSWORD);

    new CasesPage(driver, application.getBaseUrl()).open();
    assertAccessibleFormControls();
    new NewCasePage(driver, application.getBaseUrl()).open();
    assertAccessibleFormControls();
    CaseDetailsPage details = new CaseDetailsPage(driver, application.getBaseUrl()).open(1);
    assertAccessibleFormControls();

    Path unsupported =
        Files.createTempDirectory(browser.getDownloadDirectory().getParent(), "upload-label-")
            .resolve("unsupported.html");
    Files.write(unsupported, "<p>Unsupported attachment</p>".getBytes(StandardCharsets.UTF_8));
    details.uploadDocument(unsupported);
    assertTrue(driver.findElement(By.id("upload-file")).isDisplayed());
    assertAccessibleFormControls();

    new UsersPage(driver, application.getBaseUrl()).open();
    assertAccessibleFormControls();
  }

  /**
   * Confirms seeded disabled accounts cannot authenticate.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void rejectsLoginForDisabledAccounts() {
    BasePage result =
        new LoginPage(browser.getWebDriver(), application.getBaseUrl())
            .open()
            .signIn("disabled", PASSWORD);
    assertTrue(result.getValidationErrors().contains("Invalid credentials"));
    assertTrue(result.hasForm("/authenticate.do"));
  }

  /** Verifies the supervisor can follow the new-case link and render the JSP form. */
  @Test
  public void opensNewCaseAsSupervisor() {
    signIn("supervisor");
    NewCasePage page =
        new CasesPage(browser.getWebDriver(), application.getBaseUrl()).open().openNewCase();
    assertTrue(page.hasForm("/caseCreate.do"));
    assertTrue(browser.getWebDriver().findElement(By.id("new-case-title")).isDisplayed());
    assertTrue(browser.getWebDriver().findElement(By.id("new-case-priority")).isDisplayed());
    assertFalse(page.getVisibleText().contains("Request could not be completed"));
  }

  /**
   * Persists a case, note and multipart document, then verifies bytes and visibility after a fresh
   * login.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void createsCaseWithNoteAndDocumentAsWorker() throws Exception {
    signIn("worker");
    CaseDetailsPage details =
        new NewCasePage(browser.getWebDriver(), application.getBaseUrl())
            .open()
            .createCase("Functional access request", "Please enable the reporting account.");
    final long id = details.getCaseId();
    assertEquals("NEW", details.getCaseStatus());
    details.addNote("Investigated by the functional test.");
    assertTrue(details.getVisibleText().contains("Investigated by the functional test."));
    byte[] content = "Portable attachment: UTF-8 — complete.\n".getBytes(StandardCharsets.UTF_8);
    Path upload =
        Files.createTempDirectory(browser.getDownloadDirectory().getParent(), "upload-")
            .resolve("functional-note.txt");
    Files.write(upload, content);
    details.uploadDocument(upload).downloadDocument(upload.getFileName().toString());
    Path download = waitForDownload("functional-note.txt");
    assertArrayEquals(content, Files.readAllBytes(download));
    details.logout();
    signIn("worker");
    details.open(id);
    assertTrue(details.getVisibleText().contains("Functional access request"));
    assertTrue(details.getVisibleText().contains("Investigated by the functional test."));
    assertTrue(details.getVisibleText().contains("functional-note.txt"));
  }

  /**
   * Exercises the server ValidatorForm rather than depending on the browser's required-field check.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void showsValidationErrorsForBlankTitle() {
    signIn("worker");
    CaseDetailsPage result =
        new NewCasePage(browser.getWebDriver(), application.getBaseUrl())
            .open()
            .createCase("   ", "A description retained after validation.");
    assertTrue(result.getValidationErrors().contains("Title"));
    assertEquals(
        "A description retained after validation.",
        browser.getWebDriver().findElement(By.name("description")).getAttribute("value"));
    assertFalse(browser.getWebDriver().getCurrentUrl().contains("/caseView.do"));
  }

  /**
   * Checks the servlet security filter by submitting a real edit form without its token.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void blocksMutationWithoutCsrfToken() {
    signIn("worker");
    CaseDetailsPage details =
        new CaseDetailsPage(browser.getWebDriver(), application.getBaseUrl()).open(1);
    details.submitWithoutCsrf("/caseUpdate.do");
    assertEquals(403, details.getResponseStatus());
    assertTrue(details.getVisibleText().contains("Request could not be completed"));
    assertTrue(details.open(1).getVisibleText().contains("Access request"));
  }

  /**
   * Verifies assignment produces an actor-specific in-app notice that can be marked read.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void notifiesWorkerAfterSupervisorAssignment() {
    signIn("supervisor");
    CaseDetailsPage details =
        new CaseDetailsPage(browser.getWebDriver(), application.getBaseUrl())
            .open(5)
            .assignCase("worker");
    assertTrue(details.getVisibleText().contains("worker"));
    details.logout();
    signIn("worker");
    NotificationsPage notices =
        new NotificationsPage(browser.getWebDriver(), application.getBaseUrl()).open();
    assertTrue(notices.getVisibleText().contains("CASE-000005"));
    int unread = notices.countUnreadNotifications();
    assertTrue(unread >= 2);
    notices.markFirstRead();
    assertEquals(unread - 1, notices.countUnreadNotifications());
  }

  /**
   * Covers the happy path and rejection/rework using the supervisor and assigned worker accounts.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void supportsRejectionReworkAndClosure() {
    signIn("supervisor");
    CaseDetailsPage details =
        new CaseDetailsPage(browser.getWebDriver(), application.getBaseUrl()).open(1);
    assertEquals("TRIAGED", details.transitionCase("TRIAGED").getCaseStatus());
    details.logout();
    signIn("worker");
    assertEquals("IN_PROGRESS", details.open(1).transitionCase("IN_PROGRESS").getCaseStatus());
    assertEquals("PENDING_APPROVAL", details.transitionCase("PENDING_APPROVAL").getCaseStatus());
    details.logout();
    signIn("supervisor");
    assertEquals("REJECTED", details.open(1).transitionCase("REJECTED").getCaseStatus());
    details.logout();
    signIn("worker");
    assertEquals("IN_PROGRESS", details.open(1).transitionCase("IN_PROGRESS").getCaseStatus());
    assertEquals("PENDING_APPROVAL", details.transitionCase("PENDING_APPROVAL").getCaseStatus());
    assertFalse(details.hasForm("/caseTransition.do"));
    details.logout();
    signIn("supervisor");
    assertEquals("APPROVED", details.open(1).transitionCase("APPROVED").getCaseStatus());
    assertEquals("CLOSED", details.transitionCase("CLOSED").getCaseStatus());
    assertFalse(details.hasForm("/caseUpdate.do"));
  }

  /**
   * Submits tampered workflow choices to verify business authorization beyond the rendered
   * controls.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void rejectsWorkerApprovalAndCreatorSelfApproval() {
    signIn("worker");
    CaseDetailsPage details =
        new CaseDetailsPage(browser.getWebDriver(), application.getBaseUrl())
            .open(2)
            .submitTamperedTransition("APPROVED");
    assertTrue(details.getValidationErrors().contains("Transition not permitted"));
    assertEquals("IN_PROGRESS", details.open(2).getCaseStatus());
    details.open(5);
    assertFalse(details.hasForm("/caseUpdate.do"));
    assertFalse(details.hasForm("/caseTransition.do"));
    details.logout();
    signIn("supervisor");
    details
        .open(5)
        .transitionCase("IN_PROGRESS")
        .transitionCase("PENDING_APPROVAL")
        .submitTamperedTransition("APPROVED");
    assertTrue(details.getValidationErrors().contains("Creator cannot self-approve"));
    assertEquals("PENDING_APPROVAL", details.open(5).getCaseStatus());
  }

  /**
   * Loads two browser tabs before saving, then confirms a stale edit cannot overwrite the first
   * update.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void rejectsStaleEditWithoutOverwritingCase() {
    signIn("worker");
    WebDriver driver = browser.getWebDriver();
    CaseDetailsPage details = new CaseDetailsPage(driver, application.getBaseUrl()).open(1);
    String first = driver.getWindowHandle();
    driver.switchTo().newWindow(WindowType.TAB);
    final String second = driver.getWindowHandle();
    details.open(1);
    driver.switchTo().window(first);
    details.updateTitle("First browser tab saved");
    driver.switchTo().window(second);
    details.updateTitle("Stale browser tab must not save");
    assertTrue(details.getValidationErrors().contains("Another user updated this case"));
    details.open(1);
    assertTrue(details.getVisibleText().contains("First browser tab saved"));
    assertFalse(details.getVisibleText().contains("Stale browser tab must not save"));
  }

  /**
   * Checks two distinct result pages and combined search, status and assignee filtering.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void filtersAndPaginatesSeededCases() {
    signIn("worker");
    CasesPage cases = new CasesPage(browser.getWebDriver(), application.getBaseUrl()).open();
    List<String> first = cases.getCaseNumbers();
    assertEquals(25, first.size());
    List<String> second = cases.goToNextPage().getCaseNumbers();
    assertEquals(9, second.size());
    Set<String> caseNumbers = new HashSet<String>(first);
    caseNumbers.addAll(second);
    assertEquals(34, caseNumbers.size());
    assertEquals(
        java.util.Collections.singletonList("CASE-000001"),
        cases.open().searchCases("Access request", "NEW", "worker").getCaseNumbers());
    assertTrue(cases.searchCases("does-not-exist", "", "").getCaseNumbers().isEmpty());
  }

  /**
   * Confirms the auditor's read-only view and verifies a real CSV download escapes formula
   * prefixes.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void allowsAuditorToReadReportsAndExportSafeCsv() throws Exception {
    signIn("auditor");
    new CasesPage(browser.getWebDriver(), application.getBaseUrl()).open();
    assertTrue(
        browser.getWebDriver().findElements(By.cssSelector("a[href$='/caseNew.do']")).isEmpty());
    CaseDetailsPage details =
        new CaseDetailsPage(browser.getWebDriver(), application.getBaseUrl()).open(1);
    assertFalse(details.hasForm("/caseUpdate.do"));
    assertFalse(details.hasForm("/caseComment.do"));
    assertFalse(details.hasForm("/caseUpload.do"));
    ReportsPage reports = new ReportsPage(browser.getWebDriver(), application.getBaseUrl()).open();
    assertTrue(reports.getVisibleText().contains("NEW"));
    reports.exportCsv();
    String csv =
        new String(Files.readAllBytes(waitForDownload("cases.csv")), StandardCharsets.UTF_8);
    assertTrue(csv.contains("CASE-000035"));
    assertTrue(csv.contains("'=1+1"));
    assertTrue(reports.openAudit().getVisibleText().contains("Case created"));
  }

  /**
   * Creates an account via Struts and confirms it can authenticate against the persisted password
   * hash.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void createsUsableAccountAsAdministrator() {
    signIn("admin");
    UsersPage users =
        new UsersPage(browser.getWebDriver(), application.getBaseUrl())
            .open()
            .createWorker("functionalworker", PASSWORD);
    assertTrue(users.getVisibleText().contains("functionalworker"));
    users.logout();
    signIn("functionalworker");
    assertTrue(browser.getWebDriver().getCurrentUrl().contains("/dashboard.do"));
  }

  /**
   * Uses independent browsers to verify role and enabled-flag changes affect an already logged-in
   * worker.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void appliesAccountChangesToExistingSessions() throws Exception {
    signIn("worker");
    WebDriver adminBrowser = browser.openAnotherBrowser();
    new LoginPage(adminBrowser, application.getBaseUrl()).open().signIn("admin", PASSWORD);
    UsersPage users =
        new UsersPage(adminBrowser, application.getBaseUrl())
            .open()
            .changeRole("worker", "AUDITOR");
    CaseDetailsPage details =
        new CaseDetailsPage(browser.getWebDriver(), application.getBaseUrl()).open(1);
    assertFalse(details.hasForm("/caseUpdate.do"));
    users.toggleEnabled("worker");
    new LoginPage(browser.getWebDriver(), application.getBaseUrl()).openProtectedPage();
    assertTrue(browser.getWebDriver().getCurrentUrl().endsWith("/login.do"));
  }

  /**
   * Signs in using the shared fixture password while leaving role-specific operations in the page
   * objects.
   *
   * @param username the username identifying the account
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  private void signIn(String username) {
    new LoginPage(browser.getWebDriver(), application.getBaseUrl())
        .open()
        .signIn(username, PASSWORD);
    assertTrue(
        "Expected successful login for " + username + ": " + browser.getWebDriver().getCurrentUrl(),
        browser.getWebDriver().getCurrentUrl().contains("/dashboard.do"));
  }

  /**
   * Checks rendered controls rather than assuming JSP translation tags produce readable labels.
   *
   * @throws AssertionError if a form control lacks a unique ID, an associated label, or an
   *     accessible name
   * @throws org.openqa.selenium.WebDriverException if the browser cannot inspect a rendered control
   */
  private void assertAccessibleFormControls() {
    WebDriver driver = browser.getWebDriver();
    List<WebElement> controls =
        driver.findElements(By.cssSelector("input:not([type='hidden']), select, textarea"));
    assertFalse("Expected editable form controls on " + driver.getCurrentUrl(), controls.isEmpty());
    for (WebElement control : controls) {
      String id = control.getAttribute("id");
      assertFalse("Control must have an ID: " + control.getAttribute("name"), id.isEmpty());
      assertEquals("Control IDs must be unique: " + id, 1, driver.findElements(By.id(id)).size());
      assertEquals(
          "Control must have an explicit label: " + id,
          1,
          driver.findElements(By.cssSelector("label[for='" + id + "']")).size());
      assertFalse(
          "Control must have a translated accessible name: " + id,
          control.getAccessibleName().trim().isEmpty());
    }
  }

  /**
   * Waits for Chrome's completed attachment file rather than sleeping or bypassing the browser with
   * HTTP.
   *
   * @param filename the filename to locate
   * @return the completed download path
   */
  private Path waitForDownload(String filename) {
    Path file = browser.getDownloadDirectory().resolve(filename);
    new WebDriverWait(browser.getWebDriver(), Duration.ofSeconds(20))
        .until(ignored ->
            Files.isRegularFile(file)
                && !Files.exists(browser.getDownloadDirectory().resolve(filename + ".crdownload")));
    return file;
  }
}
