package com.example.legacy.functional.pages;

import java.nio.file.Path;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

/**
 * Page object for case edits, workflow, assignment, notes, and document transfers.
 *
 * @author Chuck Emmons
 */
public final class CaseDetailsPage extends BasePage {
  /**
   * Creates a details-page object without navigating away from a submitted result.
   *
   * @param driver the browser driver used by this page object
   * @param baseUrl the application's base URL
   */
  public CaseDetailsPage(WebDriver driver, String baseUrl) {
    super(driver, baseUrl);
  }

  /**
   * Opens a case through its public identifier.
   *
   * @param id the persistent record identifier
   * @return this page object for further interactions
   */
  public CaseDetailsPage open(long id) {
    navigateTo("/caseView.do?id=" + id);
    return this;
  }

  /**
   * Returns the identifier embedded in the edit form after creation.
   *
   * @return the identifier embedded in the edit form after creation
   * @throws NumberFormatException if text expected to contain an integer is not a valid integer
   */
  public long getCaseId() {
    return Long.parseLong(
        findForm("/caseUpdate.do").findElement(By.name("id")).getAttribute("value"));
  }

  /**
   * Returns the status displayed beside the case heading.
   *
   * @return the status displayed beside the case heading
   */
  public String getCaseStatus() {
    return driver.findElement(By.cssSelector(".between > .pill")).getText();
  }

  /**
   * Assigns the case to an active account through the supervisor form.
   *
   * @param username the username identifying the account
   * @return this page object for further interactions
   */
  public CaseDetailsPage assignCase(String username) {
    WebElement target = findForm("/caseAssign.do");
    new Select(target.findElement(By.name("assignee"))).selectByValue(username);
    submitForm(target);
    return this;
  }

  /**
   * Selects an offered workflow destination and submits the action.
   *
   * @param destination the requested destination status code
   * @return this page object for further interactions
   */
  public CaseDetailsPage transitionCase(String destination) {
    WebElement target = findForm("/caseTransition.do");
    new Select(target.findElement(By.name("destination"))).selectByValue(destination);
    submitForm(target);
    return this;
  }

  /**
   * Sends a tampered destination to exercise server-side authorization independently of the
   * options.
   *
   * @param destination the requested destination status code
   * @return this page object for further interactions
   */
  public CaseDetailsPage submitTamperedTransition(String destination) {
    WebElement target = findForm("/caseTransition.do");
    WebElement select = target.findElement(By.name("destination"));
    ((JavascriptExecutor) driver)
        .executeScript(
            "arguments[0].add(new Option(arguments[1], arguments[1], true, true));",
            select,
            destination);
    submitForm(target);
    return this;
  }

  /**
   * Updates the title while preserving the form's loaded optimistic-lock version.
   *
   * @param title the case title
   * @return this page object for further interactions
   */
  public CaseDetailsPage updateTitle(String title) {
    WebElement target = findForm("/caseUpdate.do");
    enterFieldText(target, "title", title);
    submitForm(target);
    return this;
  }

  /**
   * Adds a note through the discussion form.
   *
   * @param body the message body
   * @return this page object for further interactions
   */
  public CaseDetailsPage addNote(String body) {
    WebElement target = findForm("/caseComment.do");
    enterFieldText(target, "body", body);
    submitForm(target);
    return this;
  }

  /**
   * Uploads a real local file using the browser's multipart form handling.
   *
   * @param file the local file to upload
   * @return this page object for further interactions
   * @throws NullPointerException if {@code file} is {@code null}
   */
  public CaseDetailsPage uploadDocument(Path file) {
    WebElement target = findForm("/caseUpload.do");
    target.findElement(By.name("file")).sendKeys(file.toAbsolutePath().toString());
    submitForm(target);
    return this;
  }

  /**
   * Starts an attachment download through its rendered link.
   *
   * @param filename the filename to locate
   */
  public void downloadDocument(String filename) {
    driver.findElement(By.linkText(filename)).click();
  }
}
