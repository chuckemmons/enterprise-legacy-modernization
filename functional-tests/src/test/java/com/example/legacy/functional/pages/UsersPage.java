package com.example.legacy.functional.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

/**
 * Administrator account-creation and account-management interactions.
 *
 * @author Chuck Emmons
 */
public final class UsersPage extends BasePage {
  /**
   * Creates an account-administration page for one browser.
   *
   * @param driver the browser driver used by this page object
   * @param baseUrl the application's base URL
   */
  public UsersPage(WebDriver driver, String baseUrl) {
    super(driver, baseUrl);
  }

  /**
   * Opens account administration.
   *
   * @return this page object for further interactions
   */
  public UsersPage open() {
    navigateTo("/users.do");
    return this;
  }

  /**
   * Creates a worker account through the complete validated form.
   *
   * @param username the username identifying the account
   * @param password the plaintext password
   * @return this page object for further interactions
   */
  public UsersPage createWorker(String username, String password) {
    WebElement target = findForm("/userCreate.do");
    enterFieldText(target, "username", username);
    enterFieldText(target, "password", password);
    enterFieldText(target, "name", "Functional Test Worker");
    enterFieldText(target, "email", username + "@example.test");
    enterFieldText(target, "department", "Operations");
    new Select(target.findElement(By.name("role"))).selectByVisibleText("WORKER");
    submitForm(target);
    return this;
  }

  /**
   * Changes a user's role using the management form in that user's table row.
   *
   * @param username the username identifying the account
   * @param role the account role code
   * @return this page object for further interactions
   * @throws IllegalArgumentException if no account row matches the requested username
   * @throws NullPointerException if {@code username} is {@code null}
   */
  public UsersPage changeRole(String username, String role) {
    WebElement target =
        findAccountRow(username).findElement(By.cssSelector("form[action$='/userRole.do']"));
    new Select(target.findElement(By.name("role"))).selectByVisibleText(role);
    submitForm(target);
    return this;
  }

  /**
   * Toggles an account's enabled flag using its own management form.
   *
   * @param username the username identifying the account
   * @return this page object for further interactions
   * @throws IllegalArgumentException if no account row matches the requested username
   * @throws NullPointerException if {@code username} is {@code null}
   */
  public UsersPage toggleEnabled(String username) {
    submitForm(
        findAccountRow(username).findElement(By.cssSelector("form[action$='/userDisable.do']")));
    return this;
  }

  /**
   * Locates an account row by its exact username cell.
   *
   * @param username the username identifying the account
   * @return the account table row with the exact username
   * @throws IllegalArgumentException if no account row matches the requested username
   * @throws NullPointerException if {@code username} is {@code null}
   */
  private WebElement findAccountRow(String username) {
    for (WebElement row : driver.findElements(By.cssSelector("table tr"))) {
      java.util.List<WebElement> cells = row.findElements(By.tagName("td"));
      if (!cells.isEmpty() && username.equals(cells.get(0).getText())) {
        return row;
      }
    }
    throw new IllegalArgumentException("No account row for " + username);
  }
}
