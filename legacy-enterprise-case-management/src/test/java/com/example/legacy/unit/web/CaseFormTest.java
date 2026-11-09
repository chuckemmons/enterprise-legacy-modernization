package com.example.legacy.unit.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.legacy.service.dto.CaseDto;
import com.example.legacy.service.dto.CasePageDto;
import com.example.legacy.support.ValidatorTestSupport;
import com.example.legacy.unit.support.WebTestSupport;
import com.example.legacy.web.CaseForm;
import com.example.legacy.web.ValidationInputAction;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionMapping;
import org.junit.Before;
import org.junit.Test;

/**
 * Behavioral contracts for CaseForm.
 *
 * @author Chuck Emmons
 */
public class CaseFormTest extends WebTestSupport {

  /** Creates isolated collaborators and input state for each test. */
  @Before
  public void configureModule() {
    attributes.put(Globals.MODULE_KEY, ValidatorTestSupport.getModuleConfiguration());
  }

  /**
   * Validates only choices required by the mapped operation.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void validatesOnlyChoicesRequiredByTheMappedOperation() {
    CaseForm form = ValidatorTestSupport.prepareForm(new CaseForm());
    form.setId("7");
    form.setTitle("Title");
    form.setDescription("Description");
    form.setVersion("3");
    form.setPriority("INVALID");
    assertTrue(form.validate(mapping("update"), request).get("priority").hasNext());
    form.setDestination("UNKNOWN");
    assertTrue(form.validate(mapping("transition"), request).get("destination").hasNext());
    form.setStatus("UNKNOWN");
    assertTrue(form.validate(mapping("list"), request).get("status").hasNext());
    form.setStatus("");
    form.setBody("Note");
    assertTrue(form.validate(mapping("comment"), request).isEmpty());
    ActionMapping input = mapping("case");
    input.setType(ValidationInputAction.class.getName());
    assertTrue(form.validate(input, request).isEmpty());
  }

  /**
   * Reloads display fields from persisted case data.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void reloadsDisplayFieldsFromPersistedCaseData() {
    CaseForm form = new CaseForm();
    form.setData(
        CasePageDto.builder()
            .item(
                CaseDto.builder()
                    .id(7L)
                    .title("Title")
                    .description("Description")
                    .category("ACCESS")
                    .priority("HIGH")
                    .department("Operations")
                    .build())
            .build());
    assertEquals("Description", form.getDescription());
    assertEquals("ACCESS", form.getCategory());
    assertEquals("HIGH", form.getPriority());
    assertEquals("Operations", form.getDepartment());
  }
}
