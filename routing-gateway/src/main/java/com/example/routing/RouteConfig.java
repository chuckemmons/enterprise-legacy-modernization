package com.example.routing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Validated, immutable configuration for read-only capability routing.
 *
 * @author Chuck Emmons
 */
final class RouteConfig {
  private static final Gson JSON = new GsonBuilder().setStrictness(Strictness.STRICT).create();
  private final JsonObject values;

  /**
   * Copies configuration values so later changes to the caller's JSON cannot affect routing.
   *
   * @param values configuration object to copy
   */
  private RouteConfig(JsonObject values) {
    this.values = values.deepCopy();
  }

  /**
   * Parses strict JSON and validates endpoints, capability ownership and replacement readiness.
   *
   * @param source JSON configuration text
   * @return validated configuration with a private copy of the input values
   * @throws com.google.gson.JsonParseException if the JSON is malformed or has an incompatible
   *     shape
   * @throws IllegalArgumentException if configuration keys, values or readiness constraints are
   *     invalid
   */
  static RouteConfig parse(String source) {
    JsonObject values = JSON.fromJson(source, JsonObject.class);
    requireKeys(
        values, "listen", "context", "legacy", "replacement", "replacement_ready", "capabilities");
    RouteConfig config = new RouteConfig(values);
    for (String key : Arrays.asList("listen", "legacy", "replacement")) {
      validateAddress(config.value(key));
    }
    require(
        config.value("context").matches("/[A-Za-z0-9_-]+"),
        "Context must be one path segment, e.g. /cases");
    JsonElement capabilities = values.get("capabilities");
    require(capabilities.isJsonObject(), "Capabilities must be an object");
    requireKeys(capabilities.getAsJsonObject(), "case-search", "case-details");
    for (String capability : Arrays.asList("case-search", "case-details")) {
      String owner = config.owner(capability);
      require(
          owner.equals("legacy") || owner.equals("replacement"),
          "Route owner must be legacy or replacement");
    }
    JsonElement ready = values.get("replacement_ready");
    require(
        ready.isJsonPrimitive() && ready.getAsJsonPrimitive().isBoolean(),
        "replacement_ready must be boolean");
    require(
        !config.modernEnabled() || ready.getAsBoolean(),
        "Verify replacement authentication, read-only DB access and parity first");
    Set<String> addresses = new HashSet<>();
    for (String key : Arrays.asList("listen", "legacy", "replacement")) {
      require(addresses.add(config.value(key)), "Listen and backend endpoints must be distinct");
    }
    return config;
  }

  /**
   * Returns a string-valued top-level configuration entry.
   *
   * @param key name of a string configuration entry
   * @return configuration value
   * @throws IllegalArgumentException if the entry is missing or is not a JSON string
   */
  String value(String key) {
    return string(values.get(key));
  }

  /**
   * Returns the configured implementation for a supported capability.
   *
   * @param capability capability name, such as {@code case-search}
   * @return {@code legacy} or {@code replacement}
   * @throws IllegalArgumentException if the capability is absent or its value is not a string
   */
  String owner(String capability) {
    return string(values.getAsJsonObject("capabilities").get(capability));
  }

  /**
   * Reports whether any read-only capability is assigned to the replacement.
   *
   * @return {@code true} if search or details uses the replacement implementation
   */
  boolean modernEnabled() {
    return owner("case-search").equals("replacement")
        || owner("case-details").equals("replacement");
  }

  /**
   * Computes a stable revision from sorted JSON, independent of input formatting and key order.
   *
   * @return the first sixteen hexadecimal characters of the SHA-256 configuration digest
   * @throws IllegalStateException if the runtime does not provide SHA-256
   */
  String revision() {
    try {
      byte[] hash =
          MessageDigest.getInstance("SHA-256")
              .digest(canonical(values).getBytes(StandardCharsets.UTF_8));
      StringBuilder result = new StringBuilder();
      for (int index = 0; index < 8; index++) {
        result.append(String.format("%02x", hash[index] & 0xff));
      }
      return result.toString();
    } catch (NoSuchAlgorithmException failure) {
      throw new IllegalStateException("SHA-256 is required", failure);
    }
  }

  /**
   * Serializes a JSON value with object keys sorted recursively and consistent separators.
   *
   * @param element value to serialize
   * @return deterministic JSON representation used to compute configuration revisions
   */
  private static String canonical(JsonElement element) {
    if (!element.isJsonObject()) {
      return element.toString();
    }
    Map<String, JsonElement> sorted = new TreeMap<>();
    for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
      sorted.put(entry.getKey(), entry.getValue());
    }
    StringBuilder result = new StringBuilder("{");
    for (Map.Entry<String, JsonElement> entry : sorted.entrySet()) {
      if (result.length() > 1) {
        result.append(", ");
      }
      result.append(JSON.toJson(entry.getKey())).append(": ").append(canonical(entry.getValue()));
    }
    return result.append('}').toString();
  }

  /**
   * Extracts a JSON string without coercing booleans or numbers to text.
   *
   * @param value proposed string value, possibly {@code null}
   * @return the JSON string's contents
   * @throws IllegalArgumentException if the value is absent or is not a JSON string
   */
  private static String string(JsonElement value) {
    require(
        value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString(),
        "Expected a string configuration value");
    return value.getAsString();
  }

  /**
   * Requires an IPv4 address and a port between one and 65535, without configuration interpolation.
   *
   * @param value endpoint in {@code IPv4:port} form
   * @throws IllegalArgumentException if the address or port is invalid
   */
  private static void validateAddress(String value) {
    require(value.matches("[0-9.]+:[0-9]+"), "Endpoints must be IPv4:port");
    String[] endpoint = value.split(":");
    String[] parts = endpoint[0].split("\\.", -1);
    require(parts.length == 4, "Endpoints must be IPv4:port");
    for (String part : parts) {
      require(
          part.matches("0|[1-9][0-9]{0,2}") && Integer.parseInt(part) <= 255,
          "Invalid IPv4 address");
    }
    require(endpoint[1].matches("[1-9][0-9]{0,4}"), "Invalid port");
    require(Integer.parseInt(endpoint[1]) <= 65535, "Port must be between 1 and 65535");
  }

  /**
   * Requires an object to contain exactly the permitted configuration keys.
   *
   * @param value object to inspect, possibly {@code null}
   * @param keys complete set of permitted and required keys
   * @throws IllegalArgumentException if the object is absent or its key set differs
   */
  private static void requireKeys(JsonObject value, String... keys) {
    require(
        value != null && value.keySet().equals(new HashSet<>(Arrays.asList(keys))),
        "Unexpected or missing configuration keys");
  }

  /**
   * Rejects a configuration that does not satisfy a validation condition.
   *
   * @param condition required validation result
   * @param message explanation reported if validation fails
   * @throws IllegalArgumentException if {@code condition} is false
   */
  private static void require(boolean condition, String message) {
    if (!condition) {
      throw new IllegalArgumentException(message);
    }
  }
}
