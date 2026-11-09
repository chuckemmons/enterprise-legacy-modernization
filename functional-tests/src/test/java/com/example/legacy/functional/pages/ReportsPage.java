package com.example.legacy.functional.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Report and audit navigation, including browser-initiated CSV export.
 *
 * @author Chuck Emmons
 */
public final class ReportsPage extends BasePage {
  /**
   * Creates a reporting-page object for one browser.
   *
   * @param driver the browser driver used by this page object
   * @param baseUrl the application's base URL
   */
  public ReportsPage(WebDriver driver, String baseUrl) {
    super(driver, baseUrl);
  }

  /**
   * Opens operational reports.
   *
   * @return this page object for further interactions
   */
  public ReportsPage open() {
    navigateTo("/reports.do");
    return this;
  }

  /** Downloads the export using its public link. */
  public void exportCsv() {
    driver.findElement(By.cssSelector("a[href$='/reportsExport.do']")).click();
  }

  /**
   * Opens the audit history available to administrators and auditors.
   *
   * @return this page object for further interactions
   */
  public ReportsPage openAudit() {
    navigateTo("/audit.do");
    return this;
  }
}
