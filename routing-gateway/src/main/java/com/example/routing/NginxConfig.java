package com.example.routing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Renders the reviewed NGINX template using only validated configuration values.
 *
 * @author Chuck Emmons
 */
final class NginxConfig {
  /** Prevents instantiation of this configuration-rendering utility. */
  private NginxConfig() {}

  /**
   * Expands the packaged template with capability routes, entry redirects and platform settings.
   *
   * @param config validated routing configuration
   * @return complete NGINX configuration with its configuration revision
   * @throws IOException if the packaged template is missing or cannot be read
   */
  static String render(RouteConfig config) throws IOException {
    StringBuilder maps = new StringBuilder();
    StringBuilder locations = new StringBuilder();
    String context = config.value("context");
    for (String capability : new String[] {"case-search", "case-details"}) {
      String variable = capability.replace('-', '_');
      maps.append("    map $request_method $")
          .append(variable)
          .append("_backend {\n")
          .append("        default legacy_backend;\n        ~^(GET|HEAD)$ ")
          .append(config.owner(capability))
          .append("_backend;\n    }\n");
      String[] paths =
          capability.equals("case-search")
              ? new String[] {"cases.do", "casesInput.do"}
              : new String[] {"caseView.do", "caseInput.do"};
      for (String path : paths) {
        locations
            .append("        location = ")
            .append(context)
            .append('/')
            .append(path)
            .append(" {\n            # Keep Windows case-insensitive locations exact.\n")
            .append("            if ($uri !~ \"^")
            .append(context)
            .append('/')
            .append(path.replace(".", "\\."))
            .append("$\") { return 404; }\n")
            .append("            set $capability ")
            .append(capability)
            .append(";\n")
            .append("            proxy_pass http://$")
            .append(variable)
            .append("_backend")
            .append(context)
            .append('/')
            .append(path)
            .append("$is_args$args;\n        }\n");
      }
    }
    String assets =
        config.modernEnabled()
            ? "if ($request_method !~ ^(GET|HEAD)$) { return 405; }\n"
                + "            proxy_pass http://replacement_backend;"
            : "return 404;";
    String result =
        template()
            .replace("@revision@", config.revision())
            .replace(
                "@null@",
                System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("windows")
                    ? "nul"
                    : "/dev/null")
            .replace("@maps@", maps)
            .replace("@locations@", locations)
            .replace("@entry_redirects@", entryRedirects(context))
            .replace("@assets@", assets);
    for (String key : new String[] {"context", "listen", "legacy", "replacement"}) {
      result = result.replace("@" + key + "@", config.value(key));
    }
    return result;
  }

  /**
   * Builds compatibility redirects for the hyphenated legacy application entry points.
   *
   * @param context canonical backend context path
   * @return redirect locations for {@code /legacy_case_management}, or empty text for other
   *     contexts
   */
  private static String entryRedirects(String context) {
    if (!context.equals("/legacy_case_management")) {
      return "";
    }
    // Accept the legacy WAR's hyphenated name at the IDE entry point. Keep all application
    // paths, cookies and capability routing on the deployed underscore context.
    StringBuilder redirects = new StringBuilder();
    for (String alias : new String[] {"/legacy-case-management", "/legacy-case-management/"}) {
      redirects
          .append("        location = ")
          .append(alias)
          .append(" {\n")
          .append("            absolute_redirect off;\n")
          .append("            return 308 /legacy_case_management/$is_args$args;\n        }\n");
    }
    return redirects.toString();
  }

  /**
   * Loads the UTF-8 NGINX template from the application's classpath.
   *
   * @return unexpanded template text
   * @throws IOException if the template is missing or cannot be read
   */
  private static String template() throws IOException {
    try (InputStream input = NginxConfig.class.getResourceAsStream("/nginx.conf.template")) {
      if (input == null) {
        throw new IOException("Missing NGINX template");
      }
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      byte[] buffer = new byte[4096];
      int count;
      while ((count = input.read(buffer)) != -1) {
        output.write(buffer, 0, count);
      }
      return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }
  }
}
