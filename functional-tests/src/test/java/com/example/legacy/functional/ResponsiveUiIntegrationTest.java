package com.example.legacy.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.legacy.functional.pages.LoginPage;
import com.example.legacy.functional.support.BrowserSession;
import com.example.legacy.functional.support.EmbeddedApplication;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Collections;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** Browser checks for responsive layouts, focus management and progressive enhancement. */
public class ResponsiveUiIntegrationTest {
  /** Application lifecycle shared by this test class. */
  @ClassRule public static final EmbeddedApplication application = new EmbeddedApplication();

  /** Browser lifecycle for each test. */
  @Rule public final BrowserSession browser = new BrowserSession(application);

  @Test
  public void supportsMobileNavigationAndScrollableTables() throws Exception {
    WebDriver driver = browser.getWebDriver();
    new LoginPage(driver, application.getBaseUrl())
        .open()
        .signIn("admin", "FunctionalPassword!2026");
    driver.manage().window().setSize(new Dimension(390, 844));
    WebElement toggle = driver.findElement(By.cssSelector(".navbar-toggler"));
    assertTrue(toggle.isDisplayed());
    assertEquals("false", toggle.getAttribute("aria-expanded"));
    toggle.sendKeys(Keys.ENTER);
    new WebDriverWait(driver, Duration.ofSeconds(5))
        .until(ExpectedConditions.visibilityOfElementLocated(By.id("primary-navigation")));
    assertEquals("true", toggle.getAttribute("aria-expanded"));
    driver.findElement(By.cssSelector("#primary-navigation a[href$='/cases.do']")).click();
    assertEquals(
        "page",
        driver
            .findElement(By.cssSelector("a[href$='/cases.do'].nav-link"))
            .getAttribute("aria-current"));
    assertEquals("3.7.1", ((JavascriptExecutor) driver).executeScript("return jQuery.fn.jquery"));
    assertEquals(
        "function", ((JavascriptExecutor) driver).executeScript("return typeof jQuery.ajax"));
    assertEquals(
        "auto",
        ((JavascriptExecutor) driver)
            .executeScript("return getComputedStyle(document.documentElement).scrollBehavior"));
    String[] pages = {
      "/dashboard.do",
      "/cases.do",
      "/caseNew.do",
      "/caseView.do?id=1",
      "/users.do",
      "/reports.do",
      "/audit.do",
      "/notifications.do"
    };
    for (String page : pages) {
      driver.get(application.getBaseUrl() + page);
      assertEquals(
          "No page-wide horizontal overflow: " + page,
          Boolean.TRUE,
          ((JavascriptExecutor) driver)
              .executeScript("return document.documentElement.scrollWidth <= window.innerWidth"));
      for (WebElement region : driver.findElements(By.cssSelector(".table-responsive"))) {
        assertEquals("0", region.getAttribute("tabindex"));
        assertFalse(region.getAccessibleName().isEmpty());
        region.sendKeys(Keys.ARROW_RIGHT);
      }
    }
    driver.get(application.getBaseUrl() + "/cases.do");
    Files.write(
        browser.getDownloadDirectory().getParent().resolve("mobile.png"),
        ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
    driver.manage().window().setSize(new Dimension(1440, 1080));
    Files.write(
        browser.getDownloadDirectory().getParent().resolve("desktop.png"),
        ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
  }

  @Test
  public void archiveModalCancelsWithEscapeAndSubmitsOriginalForm() {
    WebDriver driver = browser.getWebDriver();
    new LoginPage(driver, application.getBaseUrl())
        .open()
        .signIn("admin", "FunctionalPassword!2026");
    driver.get(application.getBaseUrl() + "/caseView.do?id=1");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
    WebElement trigger = driver.findElement(By.cssSelector("#archive-case-form button"));
    trigger.click();
    wait.until(webDriver ->
        "archive-cancel".equals(webDriver.switchTo().activeElement().getAttribute("id")));
    driver.switchTo().activeElement().sendKeys(Keys.ESCAPE);
    wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("archive-confirmation")));
    wait.until(webDriver -> trigger.equals(webDriver.switchTo().activeElement()));
    assertTrue(driver.getCurrentUrl().contains("caseView.do"));
    trigger.click();
    wait.until(ExpectedConditions.elementToBeClickable(By.id("archive-confirm"))).click();
    wait.until(ExpectedConditions.stalenessOf(trigger));
    assertFalse(driver.findElement(By.tagName("body")).getText().contains("Invalid upload token"));
    assertTrue(driver.findElement(By.tagName("body")).getText().contains("Case archived"));
  }

  @Test
  public void focusesServerValidationSummary() {
    WebDriver driver = browser.getWebDriver();
    new LoginPage(driver, application.getBaseUrl()).open().signIn("admin", "incorrect-password");
    WebElement summary = driver.findElement(By.cssSelector("[data-error-summary]"));
    assertEquals("alert", summary.getAttribute("role"));
    assertEquals(summary, driver.switchTo().activeElement());
    assertTrue(summary.getText().contains("Invalid credentials"));
  }

  @Test
  public void keepsFormsAndMobileNavigationUsableWithoutJavaScript() {
    ChromeDriver driver = (ChromeDriver) browser.getWebDriver();
    driver.executeCdpCommand(
        "Emulation.setScriptExecutionDisabled", Collections.singletonMap("value", true));
    driver.manage().window().setSize(new Dimension(390, 844));
    new LoginPage(driver, application.getBaseUrl())
        .open()
        .signIn("admin", "FunctionalPassword!2026");
    WebElement cases =
        driver.findElement(By.cssSelector("#primary-navigation a[href$='/cases.do']"));
    assertTrue(cases.isDisplayed());
    cases.click();
    assertTrue(driver.getCurrentUrl().endsWith("/cases.do"));
    driver.findElement(By.cssSelector("a[href$='/caseNew.do'].btn")).click();
    assertTrue(driver.findElement(By.id("new-case-title")).isDisplayed());
  }
}
