package com.example.legacy.unit.web;

import static org.junit.Assert.assertTrue;

import com.example.legacy.support.ValidatorTestSupport;
import com.example.legacy.unit.support.WebTestSupport;
import com.example.legacy.web.AuditForm;
import com.example.legacy.web.NotificationForm;
import com.example.legacy.web.ReportForm;
import org.apache.struts.Globals;
import org.junit.Test;

/**
 * Behavioral contracts for ReadOnlyForms.
 *
 * @author Chuck Emmons
 */
public class ReadOnlyFormsTest extends WebTestSupport {

  /**
   * Validates each read only form using shared boundaries.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void validatesEachReadOnlyFormUsingSharedBoundaries() {
    attributes.put(Globals.MODULE_KEY, ValidatorTestSupport.getModuleConfiguration());
    assertTrue(
        ValidatorTestSupport.prepareForm(new AuditForm())
            .validate(mapping("list"), request)
            .isEmpty());
    assertTrue(
        ValidatorTestSupport.prepareForm(new ReportForm())
            .validate(mapping("page"), request)
            .isEmpty());
    NotificationForm form = ValidatorTestSupport.prepareForm(new NotificationForm());
    form.setId("7");
    assertTrue(form.validate(mapping("read"), request).isEmpty());
  }
}
