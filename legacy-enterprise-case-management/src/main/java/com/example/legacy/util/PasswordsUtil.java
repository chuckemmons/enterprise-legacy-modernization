package com.example.legacy.util;

import com.example.legacy.service.exception.BusinessValidationException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Creates and verifies salted PBKDF2-HMAC-SHA256 password hashes using the stored iteration count.
 *
 * @author Chuck Emmons
 */
public final class PasswordsUtil {
  private static final String STORED_ALGORITHM = "PBKDF2_SHA256";
  private static final String KEY_ALGORITHM = "PBKDF2WithHmacSHA256";
  private static final String HASH_SEPARATOR = "$";
  private static final int ITERATIONS = 150000;
  private static final SecureRandom random = new SecureRandom();

  /** Prevents instantiation of this static utility class. */
  private PasswordsUtil() {}

  /**
   * Rejects passwords shorter than 12 characters and returns an encoded algorithm, iteration count,
   * random 16-byte salt, and derived digest.
   *
   * @param password the plaintext password
   * @return the encoded algorithm, iteration count, salt, and password digest
   * @throws BusinessValidationException if the password is {@code null} or shorter than the minimum
   *     length
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws IllegalStateException if PBKDF2 password key derivation is unavailable or fails
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   */
  public static String hashPassword(String password) {
    ServiceValidationUtil.requirePassword(password);
    byte[] salt = new byte[16];
    random.nextBytes(salt);
    byte[] digest = derivePasswordKey(password, salt, ITERATIONS);
    return STORED_ALGORITHM
        + HASH_SEPARATOR
        + ITERATIONS
        + HASH_SEPARATOR
        + Base64.getEncoder().encodeToString(salt)
        + HASH_SEPARATOR
        + Base64.getEncoder().encodeToString(digest);
  }

  /**
   * Verifies a stored hash using a constant-time digest comparison. Malformed hashes, unsupported
   * iteration counts, and verification errors return {@code false}.
   *
   * @param password the plaintext password
   * @param encoded the stored password hash, including its algorithm, iterations, and salt
   * @return {@code true} if the password matches the stored hash; {@code false} for mismatches or
   *     verification errors
   */
  public static boolean matchesPassword(String password, String encoded) {
    try {
      String[] hashParts = encoded.split(java.util.regex.Pattern.quote(HASH_SEPARATOR));
      if (hashParts.length != 4 || !STORED_ALGORITHM.equals(hashParts[0])) {
        return false;
      }
      int iterations = Integer.parseInt(hashParts[1]);
      if (iterations < 10000 || iterations > 1000000) {
        return false;
      }
      byte[] salt = Base64.getDecoder().decode(hashParts[2]);
      byte[] expected = Base64.getDecoder().decode(hashParts[3]);
      return MessageDigest.isEqual(expected, derivePasswordKey(password, salt, iterations));
    } catch (Exception failure) {
      return false;
    }
  }

  /**
   * Derives a 256-bit key with PBKDF2-HMAC-SHA256, clearing temporary password characters
   * afterward; unavailable crypto support raises IllegalStateException.
   *
   * @param password the plaintext password
   * @param salt the salt bytes used for key derivation
   * @param iterations the number of PBKDF2 iterations
   * @return the derived 256-bit key
   * @throws IllegalStateException if PBKDF2 key derivation is unavailable or fails
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws NullPointerException if {@code password} or {@code salt} is {@code null}
   * @throws IllegalArgumentException if {@code salt} is empty or {@code iterations} is not positive
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   */
  private static byte[] derivePasswordKey(String password, byte[] salt, int iterations) {
    char[] chars = password.toCharArray();
    PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, 256);
    try {
      return SecretKeyFactory.getInstance(KEY_ALGORITHM).generateSecret(spec).getEncoded();
    } catch (Exception failure) {
      throw new IllegalStateException(
          MessagesUtil.formatMessage(MessageKeys.BUSINESS_PBKDF2_UNAVAILABLE), failure);
    } finally {
      spec.clearPassword();
      java.util.Arrays.fill(chars, '\0');
    }
  }
}
