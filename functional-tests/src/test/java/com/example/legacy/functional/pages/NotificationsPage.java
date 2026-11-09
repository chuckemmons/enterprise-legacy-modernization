package com.example.legacy.functional.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Notification viewing and acknowledgement through the rendered page.
 *
 * @author Chuck Emmons
 */
public final class NotificationsPage extends BasePage {
  /**
   * Creates a notification-page object for one browser.
   *
   * @param driver the browser driver used by this page object
   * @param baseUrl the application's base URL
   */
  public NotificationsPage(WebDriver driver, String baseUrl) {
    super(driver, baseUrl);
  }

  /**
   * Opens the current actor's notifications.
   *
   * @return this page object for further interactions
   */
  public NotificationsPage open() {
    navigateTo("/notifications.do");
    return this;
  }

  /**
   * Counts visible unread-notification acknowledgement forms.
   *
   * @return the number of visible unread-notification acknowledgement forms
   */
  public int countUnreadNotifications() {
    return driver.findElements(By.cssSelector("form[action$='/notificationRead.do']")).size();
  }

  /**
   * Acknowledges the first unread notice and waits for the refreshed page.
   *
   * @return this page object for further interactions
   */
  public NotificationsPage markFirstRead() {
    submitForm(findForm("/notificationRead.do"));
    return this;
  }
}
