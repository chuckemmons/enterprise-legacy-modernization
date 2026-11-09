package com.example.legacy.functional.pages;

import java.util.List;
import java.util.stream.Collectors;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

/**
 * Case search and pagination interactions using the rendered results table.
 *
 * @author Chuck Emmons
 */
public final class CasesPage extends BasePage {

  /**
   * Creates a case-list page for one browser.
   *
   * @param driver the browser driver used by this page object
   * @param baseUrl the application's base URL
   */
  public CasesPage(WebDriver driver, String baseUrl) {
    super(driver, baseUrl);
  }

  /**
   * Follows the new-case link and waits for the form page to load.
   *
   * @return the case creation page
   */
  public NewCasePage openNewCase() {
    WebElement document = driver.findElement(By.tagName("html"));
    driver.findElement(By.cssSelector("a[href$='/caseNew.do']")).click();
    awaitNavigation(document);
    return new NewCasePage(driver, baseUrl);
  }

  /**
   * Opens the first result page.
   *
   * @return this page object for further interactions
   */
  public CasesPage open() {
    navigateTo("/cases.do");
    return this;
  }

  /**
   * Searches by title/number with the same filters a user can submit.
   *
   * @param query the case title or number to search for
   * @param status the case status code
   * @param assignee the assigned account's username
   * @return this page object for further interactions
   */
  public CasesPage searchCases(String query, String status, String assignee) {
    WebElement target = findForm("/cases.do");
    enterFieldText(target, "q", query);
    enterFieldText(target, "assignee", assignee);
    new Select(target.findElement(By.name("status"))).selectByValue(status);
    submitForm(target);
    return this;
  }

  /**
   * Returns result identifiers rather than including the table's empty-result row.
   *
   * @return result identifiers rather than including the table's empty-result row
   */
  public List<String> getCaseNumbers() {
    return driver.findElements(By.cssSelector("tbody a[href*='/caseView.do?id=']")).stream()
        .map(WebElement::getText)
        .collect(Collectors.toList());
  }

  /**
   * Follows the next-page link and waits for server-side page navigation.
   *
   * @return this page object for further interactions
   */
  public CasesPage goToNextPage() {
    WebElement document = driver.findElement(By.tagName("html"));
    driver.findElement(By.linkText("Next")).click();
    awaitNavigation(document);
    return this;
  }
}
