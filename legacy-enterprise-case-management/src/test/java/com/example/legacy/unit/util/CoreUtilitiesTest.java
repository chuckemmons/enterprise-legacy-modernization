package com.example.legacy.unit.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.legacy.util.PasswordsUtil;
import com.example.legacy.util.StringsUtil;
import com.example.legacy.util.WorkflowUtil;
import org.junit.Test;

/**
 * Exercises workflow, password hashes, and output safeguards as independent unit tests.
 *
 * @author Chuck Emmons
 */
public class CoreUtilitiesTest {
  /**
   * Preserves supported workflow edges and role restrictions.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void enforcesWorkflowPermissions() {
    assertTrue(WorkflowUtil.isTransitionAllowed("NEW", "TRIAGED"));
    assertFalse(WorkflowUtil.isTransitionAllowed("NEW", "CLOSED"));
    assertTrue(WorkflowUtil.canTransitionCase("SUPERVISOR", "PENDING_APPROVAL", "APPROVED"));
    assertFalse(WorkflowUtil.canTransitionCase("WORKER", "PENDING_APPROVAL", "APPROVED"));
    assertTrue(WorkflowUtil.canTransitionCase("WORKER", "IN_PROGRESS", "PENDING_APPROVAL"));
  }

  /**
   * Verifies password hashing and rejection of a different password.
   *
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws IllegalStateException if a required runtime service cannot complete the operation
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   */
  @Test
  public void hashesAndVerifiesPasswords() {
    String hash = PasswordsUtil.hashPassword("SomeUniquePassword!2026");
    assertTrue(PasswordsUtil.matchesPassword("SomeUniquePassword!2026", hash));
    assertFalse(PasswordsUtil.matchesPassword("wrong", hash));
  }

  /**
   * Keeps CSV cells and filenames safe for their output formats.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void escapesOutputAndSanitizesFilenames() {
    assertFalse(StringsUtil.escapeCsvCell("=SUM(1,1)").startsWith("\"="));
    assertFalse(StringsUtil.sanitizeFilename("../../etc/passwd").contains("/"));
  }
}
