package com.example.legacy.unit.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.util.DatesUtil;
import java.util.Date;
import org.junit.Test;

/**
 * Behavioral contracts for DatesUtil.
 *
 * @author Chuck Emmons
 */
public class DatesUtilTest {

  /**
   * Parses end of day and formats without changing the input date.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void parsesEndOfDayAndFormatsWithoutChangingTheInputDate() {
    Date value = DatesUtil.parseDueDate("2028-02-29");
    java.time.ZonedDateTime local = value.toInstant().atZone(java.time.ZoneId.systemDefault());
    assertEquals(23, local.getHour());
    assertEquals(59, local.getMinute());
    assertEquals("2028-02-29", DatesUtil.formatDateInput(value));
    assertNull(DatesUtil.parseDueDate(null));
    assertNull(DatesUtil.parseDueDate(" \t"));
    assertEquals("", DatesUtil.formatDateInput(null));
  }

  /**
   * Rejects impossible and malformed dates.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void rejectsImpossibleAndMalformedDates() {
    assertThrows(BusinessValidationException.class, () -> DatesUtil.parseDueDate("2026-02-30"));
    assertThrows(BusinessValidationException.class, () -> DatesUtil.parseDueDate("not-a-date"));
  }
}
