package com.example.legacy.observability;

import com.example.legacy.web.Routes;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Retains bounded, credential-free operational events and cumulative duration histograms.
 *
 * @author Chuck Emmons
 */
public final class Telemetry {
  private static final Logger LOG = Logger.getLogger(Telemetry.class.getName());
  private static final ThreadLocal<String[]> CONTEXT = new ThreadLocal<>();
  private static final Set<String> ROUTES = routes();
  private static final ArrayDeque<String> EVENTS = new ArrayDeque<>();
  private static final Map<String, double[]> METRICS = new LinkedHashMap<>();
  private static final double[] BUCKETS = {0.005, 0.025, 0.1, 0.5, 1, 5, 30};
  private static final long STARTED = System.currentTimeMillis();

  /** Prevents instantiation of this shared operational telemetry utility. */
  private Telemetry() {}

  /**
   * Maps paths to a finite allowlist because arbitrary URLs can contain credentials.
   *
   * @param path the servlet path to classify, or {@code null} when unavailable
   * @return the known route, or {@code other} for an unrecognized path
   */
  public static String operation(String path) {
    return ROUTES.contains(path) ? path : "other";
  }

  /**
   * Establishes request context; only validated identifiers and allowlisted routes are retained.
   *
   * @param requestId the proposed canonical UUID, or an invalid value to use background correlation
   * @param path the servlet path used to select a bounded operation label
   */
  public static void begin(String requestId, String path) {
    CONTEXT.set(new String[] {validId(requestId) ? requestId : "background", operation(path)});
  }

  /** Releases pooled-thread context unconditionally at the request boundary. */
  public static void end() {
    CONTEXT.remove();
  }

  /**
   * Accepts only canonical UUIDs for correlation, never arbitrary caller text.
   *
   * @param value the proposed request identifier, or {@code null}
   * @return {@code true} if the value has the canonical UUID shape
   */
  public static boolean validId(String value) {
    return value != null
        && value.matches(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
  }

  /**
   * Records a request completion, including handled unexpected failures and HTTP server errors.
   *
   * @param start the request start time obtained from {@link System#nanoTime()}
   * @param status the response's HTTP status code
   * @param failed whether an unexpected failure occurred, even if the response status is 200
   */
  public static void request(long start, int status, boolean failed) {
    record("request", start, status, failed || status >= 500 ? "error" : "complete", null);
  }

  /**
   * Measures session acquisition through cleanup; failure does not imply rollback succeeded.
   *
   * @param start the transaction lifecycle start time obtained from {@link System#nanoTime()}
   * @param completed whether acquisition, work, commit and session cleanup all completed normally
   */
  public static void transaction(long start, boolean completed) {
    record("transaction", start, 0, completed ? "complete" : "error", null);
  }

  /**
   * Logs type only because exception messages, causes and suppressed failures may contain secrets.
   *
   * @param failure the unexpected failure whose class name should be recorded
   * @throws NullPointerException if {@code failure} is {@code null}
   */
  public static void failure(Throwable failure) {
    record("failure", System.nanoTime(), 0, "error", failure.getClass().getName());
  }

  /**
   * Records a synthetic database check without exposing connection settings or error text.
   *
   * @param start the check start time obtained from {@link System#nanoTime()}
   * @param healthy whether the database query and transaction lifecycle completed successfully
   */
  public static void check(long start, boolean healthy) {
    record("check", start, healthy ? 200 : 503, healthy ? "up" : "down", null);
  }

  /**
   * Builds an event from controlled fields, updates bounded storage and histograms, and logs it.
   *
   * <p>Logger handler failures do not change the caller's business outcome. Failure events are
   * retained without adding duration histogram samples.
   *
   * @param kind the application-controlled event kind
   * @param start the monotonic start time used to calculate elapsed seconds
   * @param status the HTTP status, or zero for non-HTTP events
   * @param outcome the application-controlled completion outcome
   * @param error the failure class name, or {@code null} when no failure type is needed
   */
  private static void record(String kind, long start, int status, String outcome, String error) {
    String[] context = CONTEXT.get();
    String id = context == null ? "background" : context[0];
    String operation = context == null ? "background" : context[1];
    double seconds = Math.max(0, System.nanoTime() - start) / 1_000_000_000.0;
    String event =
        "{\"timestamp\":\""
            + Instant.now()
            + "\",\"implementation\":\"legacy\","
            + "\"version\":\"1.0.0-SNAPSHOT\",\"event\":\""
            + kind
            + "\",\"requestId\":\""
            + id
            + "\",\"operation\":\""
            + operation
            + "\",\"status\":"
            + status
            + ",\"outcome\":\""
            + outcome
            + "\",\"durationSeconds\":"
            + seconds
            + (error == null ? "" : ",\"errorType\":\"" + error + "\"")
            + "}";
    synchronized (Telemetry.class) {
      if (EVENTS.size() == 1000) {
        EVENTS.removeFirst();
      }
      EVENTS.addLast(event);
      if (!"failure".equals(kind)) {
        String labels =
            "kind=\""
                + kind
                + "\",implementation=\"legacy\",operation=\""
                + operation
                + "\",outcome=\""
                + outcome
                + "\"";
        double[] values = METRICS.computeIfAbsent(labels, key -> new double[BUCKETS.length + 2]);
        for (int i = 0; i < BUCKETS.length; i++) {
          if (seconds <= BUCKETS[i]) {
            values[i]++;
          }
        }
        values[BUCKETS.length]++;
        values[BUCKETS.length + 1] += seconds;
      }
    }
    try {
      LOG.log(
          "failure".equals(kind) ? java.util.logging.Level.SEVERE : java.util.logging.Level.INFO,
          event);
    } catch (RuntimeException loggingFailure) {
      // A broken container handler must not fail a transaction or change an HTTP response.
    }
  }

  /**
   * Returns a consistent snapshot of the latest events for request lookup and export.
   *
   * @return the JSON snapshot containing implementation, process start time, capacity and events
   */
  public static synchronized String snapshot() {
    return "{\"implementation\":\"legacy\",\"startedAtEpochMillis\":"
        + STARTED
        + ",\"capacity\":1000,\"events\":["
        + String.join(",", EVENTS)
        + "]}";
  }

  /**
   * Exports cumulative Prometheus histograms in seconds without request IDs or record ID labels.
   *
   * @return the histogram definitions, cumulative buckets, sample counts and duration sums
   */
  public static synchronized String metrics() {
    StringBuilder text =
        new StringBuilder(
            "# HELP legacy_duration_seconds HTTP, transaction and check duration in seconds.\n"
                + "# TYPE legacy_duration_seconds histogram\n");
    METRICS.forEach(
        (labels, values) -> {
          for (int i = 0; i < BUCKETS.length; i++) {
            text.append("legacy_duration_seconds_bucket{")
                .append(labels)
                .append(",le=\"")
                .append(BUCKETS[i])
                .append("\"} ")
                .append(values[i])
                .append('\n');
          }
          text.append("legacy_duration_seconds_bucket{")
              .append(labels)
              .append(",le=\"+Inf\"} ")
              .append(values[BUCKETS.length])
              .append('\n');
          text.append("legacy_duration_seconds_count{")
              .append(labels)
              .append("} ")
              .append(values[BUCKETS.length])
              .append('\n');
          text.append("legacy_duration_seconds_sum{")
              .append(labels)
              .append("} ")
              .append(values[BUCKETS.length + 1])
              .append('\n');
        });
    return text.toString();
  }

  /**
   * Collects declared business routes and operational servlet paths for bounded metric labels.
   *
   * @return the allowlisted servlet paths
   * @throws ExceptionInInitializerError if a public route constant cannot be read
   */
  private static Set<String> routes() {
    Set<String> result = new HashSet<>();
    for (Field field : Routes.class.getFields()) {
      if (field.getType() == String.class) {
        try {
          result.add((String) field.get(null));
        } catch (IllegalAccessException failure) {
          throw new ExceptionInInitializerError(failure);
        }
      }
    }
    result.add("/operations.do");
    result.add("/health");
    return result;
  }
}
