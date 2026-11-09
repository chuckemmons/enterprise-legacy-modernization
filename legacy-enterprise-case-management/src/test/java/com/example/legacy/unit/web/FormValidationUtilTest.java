package com.example.legacy.unit.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.legacy.web.FormValidationUtil;
import org.apache.struts.action.ActionErrors;
import org.junit.Test;

/**
 * Behavioral contracts for FormValidationUtil.
 *
 * @author Chuck Emmons
 */
public class FormValidationUtilTest {

  /**
   * Checks optional choices without duplicating existing identifier errors.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void checksOptionalChoicesWithoutDuplicatingExistingIdentifierErrors() {
    ActionErrors errors = new ActionErrors();
    FormValidationUtil.validateChoice(errors, "role", "Role", null, "ADMIN");
    FormValidationUtil.validateChoice(errors, "role", "Role", "ADMIN", "ADMIN");
    assertTrue(errors.isEmpty());
    FormValidationUtil.validateChoice(errors, "role", "Role", "UNKNOWN", "ADMIN");
    assertEquals(1, errors.size());
    FormValidationUtil.validateRecordId(errors, "");
    FormValidationUtil.validateRecordId(errors, "7");
    assertEquals(1, errors.size());
    FormValidationUtil.validateRecordId(errors, "0");
    assertTrue(errors.get("id").hasNext());
    int count = errors.size();
    FormValidationUtil.validateRecordId(errors, "invalid");
    assertEquals(count, errors.size());
    ActionErrors malformed = new ActionErrors();
    FormValidationUtil.validateRecordId(malformed, "invalid");
    assertEquals(1, malformed.size());
  }
}
