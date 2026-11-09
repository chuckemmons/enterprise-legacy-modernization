package com.example.legacy.functional.support;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Downloads a matching, portable browser/driver pair without discovering installed browsers.
 *
 * @author Chuck Emmons
 */
public final class ChromeBinaries {
  private final Path directory;
  private final String version;
  private final String platform;

  /**
   * Selects the operating-system archive in the module-local, versioned cache.
   *
   * @throws IllegalArgumentException if the Chrome for Testing version is missing or is not four
   *     numeric components separated by dots
   * @throws IllegalStateException if no portable Chrome archive is configured for the operating
   *     system and architecture
   * @throws java.nio.file.InvalidPathException if a configured filesystem path is invalid for the
   *     current platform
   */
  public ChromeBinaries() {
    version = System.getProperty("functional.chromeVersion");
    if (version == null || !version.matches("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+")) {
      throw new IllegalArgumentException(
          "Set functional.chromeVersion to a pinned Chrome for Testing version");
    }
    platform = resolveBrowserPlatform();
    directory =
        Paths.get(System.getProperty("functional.browserCache", "build/browser"))
            .toAbsolutePath()
            .resolve(version)
            .resolve(platform);
  }

  /**
   * Bootstraps both binaries once before any test starts.
   *
   * @param args the command-line arguments
   * @throws Exception if the browser artifacts cannot be downloaded or installed
   */
  public static void main(String[] args) throws Exception {
    new ChromeBinaries().prepareBrowserBinaries();
  }

  /**
   * Returns the downloaded browser's executable, including the macOS app-bundle path.
   *
   * @return the downloaded browser's executable, including the macOS app-bundle path
   */
  public Path getChromeExecutable() {
    String executable = platform.startsWith("win") ? "chrome.exe" : "chrome";
    if (platform.startsWith("mac")) {
      executable = "Google Chrome for Testing.app/Contents/MacOS/Google Chrome for Testing";
    }
    return directory.resolve("chrome-" + platform).resolve(executable);
  }

  /**
   * Returns the downloaded ChromeDriver executable rather than consulting PATH or Selenium Manager.
   *
   * @return the downloaded ChromeDriver executable rather than consulting PATH or Selenium Manager
   */
  public Path getChromeDriverExecutable() {
    return directory
        .resolve("chromedriver-" + platform)
        .resolve(platform.startsWith("win") ? "chromedriver.exe" : "chromedriver");
  }

  /**
   * Reuses complete archives and retries incomplete downloads on the next invocation.
   *
   * @throws Exception if the browser artifacts cannot be downloaded or installed
   */
  public void prepareBrowserBinaries() throws Exception {
    Files.createDirectories(directory);
    installBrowserArtifact("chrome", getChromeExecutable());
    installBrowserArtifact("chromedriver", getChromeDriverExecutable());
    System.out.println(
        "Portable Chrome/ChromeDriver " + version + " (" + platform + "): " + directory);
  }

  /**
   * Downloads and extracts one official release archive, marking it complete only after extraction.
   *
   * @param artifact the Chrome for Testing artifact name
   * @param executable the expected executable path after extraction
   * @throws Exception if the browser artifacts cannot be downloaded or installed
   */
  private void installBrowserArtifact(String artifact, Path executable) throws Exception {
    Path marker = directory.resolve(artifact + ".complete");
    if (Files.isRegularFile(marker) && Files.isRegularFile(executable)) {
      return;
    }
    URL url =
        new URL(
            "https://storage.googleapis.com/chrome-for-testing-public/"
                + version
                + "/"
                + platform
                + "/"
                + artifact
                + "-"
                + platform
                + ".zip");
    Path archive = directory.resolve(artifact + ".zip.part");
    System.out.println("Downloading " + url);
    URLConnection connection = url.openConnection();
    connection.setConnectTimeout(30000);
    connection.setReadTimeout(60000);
    try (InputStream input = connection.getInputStream()) {
      Files.copy(input, archive, StandardCopyOption.REPLACE_EXISTING);
    }
    if (platform.startsWith("mac")) {
      // ditto preserves the framework symlinks in the official macOS app bundle.
      Process process =
          new ProcessBuilder("/usr/bin/ditto", "-x", "-k", archive.toString(), directory.toString())
              .inheritIO()
              .start();
      if (process.waitFor() != 0) {
        throw new IOException("Cannot extract " + archive);
      }
    } else {
      extractBrowserArchive(archive);
    }
    if (!Files.isRegularFile(executable)) {
      throw new IOException("Archive did not contain " + executable);
    }
    Files.write(marker, version.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    Files.deleteIfExists(archive);
  }

  /**
   * Extracts inside the cache and restores execution permission for the portable Linux binaries.
   *
   * @param archive the downloaded archive to extract
   * @throws IOException if an I/O operation fails
   */
  private void extractBrowserArchive(Path archive) throws IOException {
    try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        Path target = directory.resolve(entry.getName()).normalize();
        if (!target.startsWith(directory)) {
          throw new IOException("Archive entry escapes browser cache: " + entry.getName());
        }
        if (entry.isDirectory()) {
          Files.createDirectories(target);
        } else {
          Files.createDirectories(target.getParent());
          Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
          if (!platform.startsWith("win") && !target.toFile().setExecutable(true, true)) {
            throw new IOException("Cannot set executable permission on " + target);
          }
        }
      }
    }
  }

  /**
   * Maps supported native platforms to Google's archive names; never falls back to a local browser.
   *
   * @return the Chrome for Testing archive platform identifier
   * @throws IllegalStateException if no portable Chrome archive is configured for the operating
   *     system and architecture
   */
  private static String resolveBrowserPlatform() {
    String operatingSystem = System.getProperty("os.name").toLowerCase(Locale.ROOT);
    String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
    boolean armArchitecture = arch.equals("aarch64") || arch.equals("arm64");
    if (operatingSystem.contains("win") && (arch.equals("amd64") || arch.equals("x86_64"))) {
      return "win64";
    }
    if (operatingSystem.contains("mac")) {
      return armArchitecture ? "mac-arm64" : "mac-x64";
    }
    if (operatingSystem.contains("linux")
        && (armArchitecture || arch.equals("amd64") || arch.equals("x86_64"))) {
      return armArchitecture ? "linux-arm64" : "linux64";
    }
    throw new IllegalStateException(
        "No configured portable Chrome archive for " + operatingSystem + "/" + arch);
  }
}
