package com.example.legacy.unit.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.example.legacy.util.MessageKeys;
import com.example.legacy.util.MessagesUtil;
import java.util.Locale;
import org.junit.Test;

/**
 * Behavioral contracts for MessagesUtil.
 *
 * @author Chuck Emmons
 */
public class MessagesUtilTest {

  /**
   * Formats known messages and reports unknown keys.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void formatsKnownMessagesAndReportsUnknownKeys() {
    assertEquals("Title", MessagesUtil.formatMessage("ui.title"));
    assertTrue(
        MessagesUtil.formatMessage(Locale.ENGLISH, MessageKeys.NOTICE_CASE_OVERDUE, "CASE-7")
            .contains("CASE-7"));
    assertThrows(
        java.util.MissingResourceException.class,
        () -> MessagesUtil.formatMessage("nonexistent.test.key"));
  }
}
