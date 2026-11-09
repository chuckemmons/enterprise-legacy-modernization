package com.example.legacy.functional.pages;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Common browser interactions for the server-rendered Struts pages, using explicit navigation
 * waits.
 *
 * @author Chuck Emmons
 */
public class BasePage {
  /** Test fixture for driver. */
  protected final WebDriver driver;

  /** Test fixture for base url. */
  protected final String baseUrl;

  /** Test fixture for wait. */
  protected final WebDriverWait wait;

  /**
   * Retains a browser and application URL; each page object belongs to one browser session.
   *
   * @param driver the browser driver used by this page object
   * @param baseUrl the application's base URL
   */
  public BasePage(WebDriver driver, String baseUrl) {
    this.driver = driver;
    this.baseUrl = baseUrl;
    this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
  }

  /**
   * Returns the currently rendered text for user-visible assertions.
   *
   * @return the currently rendered text for user-visible assertions
   */
  public String getVisibleText() {
    return driver.findElement(By.tagName("body")).getText();
  }

  /**
   * Reads the last page response status through Chrome's Navigation Timing API without a second
   * HTTP request.
   *
   * @return the HTTP status of the most recent page navigation
   */
  public int getResponseStatus() {
    Number status =
        (Number)
            ((JavascriptExecutor) driver)
                .executeScript(
                    "return performance.getEntriesByType('navigation')[0].responseStatus;");
    return status.intValue();
  }

  /**
   * Returns Struts validation or business-rule errors from the accessible alert region.
   *
   * @return Struts validation or business-rule errors from the accessible alert region
   */
  public String getValidationErrors() {
    return driver.findElement(By.cssSelector("[role='alert']")).getText();
  }

  /**
   * Reports whether the current page exposes a given write operation.
   *
   * @param endpoint the application endpoint used to identify the form
   * @return {@code true} if the requested form is present; {@code false} otherwise
   */
  public boolean hasForm(String endpoint) {
    return !driver.findElements(By.cssSelector("form[action$='" + endpoint + "']")).isEmpty();
  }

  /**
   * Logs out through the application's POST form and waits for the login page.
   *
   * @return the login page displayed after logout
   */
  public LoginPage logout() {
    submitForm(findForm("/logout.do"));
    return new LoginPage(driver, baseUrl);
  }

  /**
   * Removes a CSRF token to exercise the real security filter through a browser submission.
   *
   * @param endpoint the application endpoint used to identify the form
   */
  public void submitWithoutCsrf(String endpoint) {
    WebElement target = findForm(endpoint);
    ((JavascriptExecutor) driver)
        .executeScript("arguments[0].remove();", target.findElement(By.name("csrf")));
    submitForm(target);
  }

  /**
   * Navigates to an application-relative path and waits until the new document body is available.
   *
   * @param path the application-relative path
   */
  protected void navigateTo(String path) {
    driver.get(baseUrl + path);
    wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
  }

  /**
   * Locates a form by its public Struts endpoint, independent of visual styling.
   *
   * @param endpoint the application endpoint used to identify the form
   * @return the HTML form for the specified endpoint
   */
  protected WebElement findForm(String endpoint) {
    return driver.findElement(By.cssSelector("form[action$='" + endpoint + "']"));
  }

  /**
   * Replaces a named field in a particular form.
   *
   * @param form the HTML form containing the control
   * @param name the form control's name
   * @param value the text to enter into the control
   * @throws NullPointerException if {@code form} is {@code null}
   */
  protected void enterFieldText(WebElement form, String name, String value) {
    WebElement field = form.findElement(By.name(name));
    field.clear();
    field.sendKeys(value);
  }

  /**
   * Submits through the visible button and waits for the previous document to unload.
   *
   * @param form the HTML form containing the controls
   * @throws NullPointerException if {@code form} is {@code null}
   */
  protected void submitForm(WebElement form) {
    WebElement document = driver.findElement(By.tagName("html"));
    form.findElement(By.tagName("button")).click();
    awaitNavigation(document);
  }

  /**
   * Waits for a new root element without asking ChromeDriver to inspect an already detached DOM
   * node.
   *
   * @param previousDocument the previous document root whose replacement signals navigation
   * @throws NullPointerException if {@code previousDocument} is {@code null}
   */
  protected void awaitNavigation(WebElement previousDocument) {
    wait.until(current -> !previousDocument.equals(current.findElement(By.tagName("html"))));
    wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
  }
}
