package com.example.legacy.functional.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Page object for login, failed authentication, and redirects from protected endpoints.
 *
 * @author Chuck Emmons
 */
public final class LoginPage extends BasePage {
  /**
   * Creates a login page object for an existing browser session.
   *
   * @param driver the browser driver used by this page object
   * @param baseUrl the application's base URL
   */
  public LoginPage(WebDriver driver, String baseUrl) {
    super(driver, baseUrl);
  }

  /**
   * Opens the public login endpoint.
   *
   * @return this page object for further interactions
   */
  public LoginPage open() {
    navigateTo("/login.do");
    return this;
  }

  /**
   * Attempts authentication and leaves assertions about success or errors to the test.
   *
   * @param username the username identifying the account
   * @param password the plaintext password
   * @return a page object for the page displayed after the authentication attempt
   */
  public BasePage signIn(String username, String password) {
    WebElement target = findForm("/authenticate.do");
    enterFieldText(target, "username", username);
    enterFieldText(target, "password", password);
    submitForm(target);
    return new BasePage(driver, baseUrl);
  }

  /**
   * Tests protected-resource navigation using the browser's current session.
   *
   * @return this page object for further interactions
   */
  public LoginPage openProtectedPage() {
    navigateTo("/cases.do");
    return this;
  }
}
