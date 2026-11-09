package com.example.legacy.unit.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.util.StringsUtil;
import java.util.Collections;
import org.junit.Test;

/**
 * Behavioral contracts for StringsUtil.
 *
 * @author Chuck Emmons
 */
public class StringsUtilTest {

  /**
   * Normalizes controls without discarding allowed whitespace.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void normalizesControlsWithoutDiscardingAllowedWhitespace() {
    assertEquals("a\nb\tc", StringsUtil.normalizeText("  a\nb\tc\u0001  ", 20));
    assertEquals("", StringsUtil.normalizeText(null, 2));
    assertThrows(BusinessValidationException.class, () -> StringsUtil.normalizeText("abc", 2));
    assertEquals("HIGH", StringsUtil.requireAllowedChoice("HIGH", "LOW", "HIGH"));
    assertThrows(
        BusinessValidationException.class,
        () -> StringsUtil.requireAllowedChoice("OTHER", "LOW", "HIGH"));
  }

  /**
   * Sanitizes path separators empty names and length limits.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void sanitizesPathSeparatorsEmptyNamesAndLengthLimits() {
    assertEquals("document", StringsUtil.sanitizeFilename(null));
    assertEquals("document", StringsUtil.sanitizeFilename(""));
    assertEquals("file.txt", StringsUtil.sanitizeFilename("C:\\folder\\file.txt"));
    assertEquals(
        120, StringsUtil.sanitizeFilename(String.join("", Collections.nCopies(130, "x"))).length());
    assertEquals("a_b.txt", StringsUtil.sanitizeFilename("a b.txt"));
  }

  /**
   * Quotes csv and neutralizes formula prefixes.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void quotesCsvAndNeutralizesFormulaPrefixes() {
    for (String prefix : new String[] {"=", "+", "-", "@", "\t"}) {
      assertTrue(StringsUtil.escapeCsvCell(prefix + "value").startsWith("\"'"));
    }
    assertEquals("\"a\"\"b c d\"", StringsUtil.escapeCsvCell("a\"b\rc\nd"));
    assertEquals("\"\"", StringsUtil.escapeCsvCell(null));
    assertEquals("", StringsUtil.convertToLowerCase(null));
    assertEquals("indigo", StringsUtil.convertToLowerCase("INDIGO"));
  }
}
