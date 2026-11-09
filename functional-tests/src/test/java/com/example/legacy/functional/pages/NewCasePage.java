package com.example.legacy.functional.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Case creation page, including server-side validation submissions.
 *
 * @author Chuck Emmons
 */
public final class NewCasePage extends BasePage {
  /**
   * Creates a case-entry page for one browser.
   *
   * @param driver the browser driver used by this page object
   * @param baseUrl the application's base URL
   */
  public NewCasePage(WebDriver driver, String baseUrl) {
    super(driver, baseUrl);
  }

  /**
   * Opens the creation form with application defaults.
   *
   * @return this page object for further interactions
   */
  public NewCasePage open() {
    navigateTo("/caseNew.do");
    return this;
  }

  /**
   * Enters the required case details and submits the create action.
   *
   * @param title the case title
   * @param description the case description
   * @return the page object for the destination page
   */
  public CaseDetailsPage createCase(String title, String description) {
    WebElement target = findForm("/caseCreate.do");
    enterFieldText(target, "title", title);
    enterFieldText(target, "description", description);
    submitForm(target);
    return new CaseDetailsPage(driver, baseUrl);
  }
}
