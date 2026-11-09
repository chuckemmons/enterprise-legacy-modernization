package com.example.legacy.unit.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.legacy.domain.AuditAction;
import com.example.legacy.domain.AuditEntity;
import com.example.legacy.domain.CaseStatus;
import com.example.legacy.domain.Priority;
import com.example.legacy.domain.ReportGrouping;
import com.example.legacy.domain.Role;
import java.util.Arrays;
import org.junit.Test;

/**
 * Behavioral contracts for DomainOptions.
 *
 * @author Chuck Emmons
 */
public class DomainOptionsTest {

  /**
   * Publishes stable defensive option arrays.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void publishesStableDefensiveOptionArrays() {
    String[] roles = Role.getCodes();
    roles[0] = "TAMPERED";
    assertFalse(Arrays.asList(Role.getCodes()).contains("TAMPERED"));
    assertTrue(Arrays.asList(CaseStatus.getCodes()).contains("CLOSED"));
    assertTrue(Arrays.asList(Priority.getCodes()).contains("HIGH"));
    assertTrue(Arrays.asList(AuditAction.getCodes()).contains("UPLOAD"));
    assertTrue(Arrays.asList(AuditEntity.getCodes()).contains("CASE"));
    assertEquals("status", ReportGrouping.STATUS.getProperty());
    assertEquals("priority", ReportGrouping.PRIORITY.getProperty());
  }

  /**
   * Matches persisted codes while rejecting different or absent values.
   *
   * @throws AssertionError if enum codes accept an incorrect value
   */
  @Test
  public void matchesCodesWithoutAcceptingNullOrUnknownValues() {
    assertTrue(Priority.HIGH.matchesCode("HIGH"));
    assertFalse(Priority.HIGH.matchesCode(null));
    assertTrue(CaseStatus.NEW.matchesCode("NEW"));
    assertFalse(CaseStatus.NEW.matchesCode("other"));
    assertTrue(Role.ADMIN.matchesCode("ADMIN"));
    assertFalse(Role.ADMIN.matchesCode(null));
    assertTrue(AuditAction.UPLOAD.matchesCode("UPLOAD"));
    assertFalse(AuditAction.UPLOAD.matchesCode("other"));
    assertTrue(AuditEntity.CASE.matchesCode("CASE"));
    assertFalse(AuditEntity.CASE.matchesCode(null));
  }
}
