package com.example.legacy.unit.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mockStatic;

import com.example.legacy.util.PasswordsUtil;
import org.junit.Test;

/**
 * Behavioral contracts for PasswordsUtil.
 *
 * @author Chuck Emmons
 */
public class PasswordsUtilTest {

  /**
   * Rejects malformed hashes without exposing parsing failures.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void rejectsMalformedHashesWithoutExposingParsingFailures() {
    for (String encoded :
        new String[] {
          null,
          "",
          "OTHER$150000$AA==$AA==",
          "PBKDF2_SHA256$9999$AA==$AA==",
          "PBKDF2_SHA256$1000001$AA==$AA==",
          "PBKDF2_SHA256$invalid$AA==$AA==",
          "PBKDF2_SHA256$150000$#$AA=="
        }) {
      assertFalse(PasswordsUtil.matchesPassword("password", encoded));
    }
    assertFalse(PasswordsUtil.matchesPassword(null, "PBKDF2_SHA256$150000$AA==$AA=="));
  }

  /**
   * Uses independent salts and reports unavailable cryptography.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if the observed behavior violates the tested contract
   */
  @Test
  public void usesIndependentSaltsAndReportsUnavailableCryptography() throws Exception {
    String first = PasswordsUtil.hashPassword("ValidPassword!2026");
    String second = PasswordsUtil.hashPassword("ValidPassword!2026");
    assertNotEquals(first, second);
    assertTrue(PasswordsUtil.matchesPassword("ValidPassword!2026", first));
    try (org.mockito.MockedStatic<javax.crypto.SecretKeyFactory> crypto =
        mockStatic(javax.crypto.SecretKeyFactory.class); ) {
      crypto
          .when(() -> javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256"))
          .thenThrow(new java.security.NoSuchAlgorithmException("unavailable"));
      IllegalStateException failure =
          assertThrows(
              IllegalStateException.class, () -> PasswordsUtil.hashPassword("ValidPassword!2026"));
      assertTrue(failure.getCause() instanceof java.security.NoSuchAlgorithmException);
    }
  }
}
