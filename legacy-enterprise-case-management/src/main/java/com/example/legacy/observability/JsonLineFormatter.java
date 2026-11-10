package com.example.legacy.observability;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Formats records as JSON lines for a handler attached exclusively to the telemetry logger.
 *
 * @author Chuck Emmons
 */
public final class JsonLineFormatter extends Formatter {
  /**
   * Emits a telemetry message and newline while excluding records from other logger categories.
   *
   * @param record the log record to format
   * @return the JSON line, or an empty string for another logger category
   * @throws NullPointerException if {@code record} is {@code null}
   */
  @Override
  public String format(LogRecord record) {
    if (!Telemetry.class.getName().equals(record.getLoggerName())) {
      return "";
    }
    return record.getMessage() + "\n";
  }
}
