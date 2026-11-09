package com.example.legacy.unit.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.example.legacy.support.ValidatorTestSupport;
import com.example.legacy.unit.support.WebTestSupport;
import com.example.legacy.util.ValidationLimits;
import com.example.legacy.web.CaseForm;
import com.example.legacy.web.ValidationInputAction;
import com.example.legacy.web.WebKeys;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.junit.Before;
import org.junit.Test;

/**
 * Behavioral contracts for BasePageForm.
 *
 * @author Chuck Emmons
 */
public class BasePageFormTest extends WebTestSupport {

  private CaseForm form;

  /** Creates isolated collaborators and input state for each test. */
  @Before
  public void createForm() {
    form = ValidatorTestSupport.prepareForm(new CaseForm());
    attributes.put(Globals.MODULE_KEY, ValidatorTestSupport.getModuleConfiguration());
  }

  /**
   * Normalizes invalid pagination and records malformed identifiers.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void normalizesInvalidPaginationAndRecordsMalformedIdentifiers() {
    form.setId("not-an-id");
    form.setPageNumber("-1");
    ActionErrors errors = form.validate(mapping("list"), request);
    assertEquals("invalid", attributes.get(WebKeys.RECORD_ID));
    assertTrue(errors.get("id").hasNext());
    assertTrue(errors.get("page").hasNext());
    assertEquals("0", form.getPageNumber());
    form.setId("");
    form.setPageNumber(Integer.toString(ValidationLimits.MAX_PAGE + 1));
    assertTrue(form.validate(mapping("list"), request).get("page").hasNext());
    assertNull(attributes.get(WebKeys.RECORD_ID));
    form.setPageNumber("");
    assertTrue(form.validate(mapping("list"), request).isEmpty());
  }

  /**
   * Skips validation on input forwards and resets only bound pagination.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void skipsValidationOnInputForwardsAndResetsOnlyBoundPagination() {
    ActionMapping input = mapping("case");
    input.setType(ValidationInputAction.class.getName());
    form.setPageNumber("invalid");
    assertTrue(form.validate(input, request).isEmpty());
    assertEquals("0", form.getPageNumber());
    parameters.put("page", "3");
    form.reset(mapping("list"), request);
    assertEquals("3", form.getPageNumber());
    when(request.getContentType()).thenReturn("multipart/form-data; boundary=test");
    form.reset(mapping("list"), request);
    assertEquals("0", form.getPageNumber());
  }
}
