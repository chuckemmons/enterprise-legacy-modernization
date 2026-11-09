package com.example.legacy.unit.web;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.example.legacy.domain.CaseStatus;
import com.example.legacy.domain.Priority;
import com.example.legacy.domain.Role;
import com.example.legacy.util.CaseDefaults;
import com.example.legacy.util.ValidationLimits;
import com.example.legacy.web.Routes;
import com.example.legacy.web.ViewOptionsUtil;
import com.example.legacy.web.WebKeys;
import org.junit.Test;

/**
 * Behavioral contracts for ViewOptionsUtil.
 *
 * @author Chuck Emmons
 */
public class ViewOptionsUtilTest {

  /**
   * Exposes immutable server selected view metadata.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void exposesImmutableServerSelectedViewMetadata() {
    ViewOptionsUtil options = new ViewOptionsUtil();
    assertArrayEquals(Role.getCodes(), options.getRoles());
    assertArrayEquals(CaseStatus.getCodes(), options.getStatuses());
    assertArrayEquals(Priority.getCodes(), options.getPriorities());
    assertEquals("WORKER", options.getDefaultRole());
    assertEquals("MEDIUM", options.getDefaultPriority());
    assertEquals(CaseDefaults.CATEGORY, options.getDefaultCategory());
    assertEquals(CaseDefaults.DEPARTMENT, options.getDefaultDepartment());
    assertEquals("ADMIN", options.getRoleCodes().get("ADMIN"));
    assertEquals(WebKeys.CSRF, options.getKeys().get("CSRF"));
    assertEquals(Routes.CASE_CREATE, options.getRoutes().get("CASE_CREATE"));
    assertEquals(Integer.toString(ValidationLimits.TITLE), options.getLimits().get("TITLE"));
    assertThrows(
        UnsupportedOperationException.class, () -> options.getRoleCodes().put("FORGED", "FORGED"));
    assertThrows(
        UnsupportedOperationException.class, () -> options.getKeys().put("CSRF", "forged"));
    assertThrows(UnsupportedOperationException.class, () -> options.getRoutes().clear());
    assertThrows(UnsupportedOperationException.class, () -> options.getLimits().clear());
  }
}
