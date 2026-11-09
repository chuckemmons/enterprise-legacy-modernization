package com.example.legacy.functional.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * Resets data and creates an isolated headless ChromeDriver session for each JUnit test.
 *
 * @author Chuck Emmons
 */
public final class BrowserSession extends TestWatcher {
  private final EmbeddedApplication application;
  private ChromeDriver driver;
  private Path downloads;
  private Path artifacts;
  private ChromeOptions options;
  private final List<ChromeDriver> drivers = new ArrayList<ChromeDriver>();

  /**
   * Associates this browser lifecycle with the class-scoped embedded application.
   *
   * @param application the embedded application that supplies the test database and base URL
   */
  public BrowserSession(EmbeddedApplication application) {
    this.application = application;
  }

  /**
   * Exposes WebDriver to page objects and assertions without sharing it across test methods.
   *
   * @return the current test's browser driver
   */
  public WebDriver getWebDriver() {
    return driver;
  }

  /**
   * Opens an independent login session for concurrent-account scenarios without resetting H2.
   *
   * @return a new browser driver with an independent login session
   * @throws IOException if an I/O operation fails
   * @throws IllegalArgumentException if the configured Chrome for Testing version is missing or
   *     blank
   * @throws IllegalStateException if the isolated browser fixture cannot be started
   * @throws java.nio.file.InvalidPathException if a configured filesystem path is invalid for the
   *     current platform
   */
  public WebDriver openAnotherBrowser() throws IOException {
    ChromeDriverService service =
        new ChromeDriverService.Builder()
            .usingDriverExecutable(new ChromeBinaries().getChromeDriverExecutable().toFile())
            .usingAnyFreePort()
            .withLogFile(artifacts.resolve("chromedriver-" + drivers.size() + ".log").toFile())
            .build();
    ChromeDriver extra = new ChromeDriver(service, options);
    drivers.add(extra);
    extra.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(45));
    return extra;
  }

  /**
   * Returns the per-test directory used to verify browser-initiated downloads.
   *
   * @return the per-test directory used to verify browser-initiated downloads
   */
  public Path getDownloadDirectory() {
    return downloads;
  }

  /**
   * Seeds H2 before opening a fresh browser profile using explicitly downloaded executables.
   *
   * @param description the JUnit test description
   * @throws IllegalStateException if the isolated browser fixture cannot be started
   */
  @Override
  protected void starting(Description description) {
    try {
      application.resetData();
      Path build = Paths.get(System.getProperty("functional.build", "build")).toAbsolutePath();
      artifacts =
          Files.createDirectories(build.resolve("artifacts").resolve(description.getMethodName()));
      Files.deleteIfExists(artifacts.resolve("failure.png"));
      Files.deleteIfExists(artifacts.resolve("failure.html"));
      Files.deleteIfExists(artifacts.resolve("failure-url.txt"));
      downloads = Files.createTempDirectory(artifacts, "downloads-");
      ChromeBinaries binaries = new ChromeBinaries();
      if (!Files.isRegularFile(binaries.getChromeExecutable())
          || !Files.isRegularFile(binaries.getChromeDriverExecutable())) {
        throw new IllegalStateException(
            "Portable browser is missing; run the root Maven build with -Pfunctional verify first");
      }
      options = new ChromeOptions();
      options.setBinary(binaries.getChromeExecutable().toFile());
      options.addArguments(
          "--headless=new",
          "--window-size=1440,1080",
          "--disable-dev-shm-usage",
          "--no-first-run",
          "--no-default-browser-check",
          "--disable-background-networking");
      if (Boolean.getBoolean("functional.noSandbox")) {
        options.addArguments("--no-sandbox");
      }
      Map<String, Object> preferences = new HashMap<String, Object>();
      preferences.put("download.default_directory", downloads.toString());
      preferences.put("download.prompt_for_download", false);
      preferences.put("safebrowsing.enabled", true);
      options.setExperimentalOption("prefs", preferences);
      driver = (ChromeDriver) openAnotherBrowser();
    } catch (Exception failure) {
      throw new IllegalStateException("Cannot start isolated browser fixture", failure);
    }
  }

  /**
   * Saves the visible failure state before the browser is closed.
   *
   * @param failure the failure to report
   * @param description the JUnit test description
   * @throws NullPointerException if {@code failure} is {@code null}
   */
  @Override
  protected void failed(Throwable failure, Description description) {
    if (driver != null) {
      try {
        Files.write(artifacts.resolve("failure.png"), driver.getScreenshotAs(OutputType.BYTES));
        Files.write(
            artifacts.resolve("failure.html"),
            driver.getPageSource().getBytes(StandardCharsets.UTF_8));
        Files.write(
            artifacts.resolve("failure-url.txt"),
            driver.getCurrentUrl().getBytes(StandardCharsets.UTF_8));
      } catch (Exception captureFailure) {
        failure.addSuppressed(captureFailure);
      }
    }
  }

  /**
   * Quits ChromeDriver after success, assertion failure, or a partially initialized fixture.
   *
   * @param description the JUnit test description
   * @throws RuntimeException if browser shutdown or fixture cleanup fails
   */
  @Override
  protected void finished(Description description) {
    RuntimeException failure = null;
    for (ChromeDriver browser : drivers) {
      try {
        browser.quit();
      } catch (RuntimeException cleanup) {
        if (failure == null) {
          failure = cleanup;
        } else {
          failure.addSuppressed(cleanup);
        }
      }
    }
    if (failure != null) {
      throw failure;
    }
  }
}
