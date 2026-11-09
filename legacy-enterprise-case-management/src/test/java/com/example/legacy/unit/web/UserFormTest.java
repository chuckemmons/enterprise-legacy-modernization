package com.example.legacy.unit.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.legacy.support.ValidatorTestSupport;
import com.example.legacy.unit.support.WebTestSupport;
import com.example.legacy.web.UserForm;
import com.example.legacy.web.ValidationInputAction;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionMapping;
import org.junit.Before;
import org.junit.Test;

/**
 * Behavioral contracts for UserForm.
 *
 * @author Chuck Emmons
 */
public class UserFormTest extends WebTestSupport {

  private UserForm form;

  /** Creates isolated collaborators and input state for each test. */
  @Before
  public void createForm() {
    form = ValidatorTestSupport.prepareForm(new UserForm());
    attributes.put(Globals.MODULE_KEY, ValidatorTestSupport.getModuleConfiguration());
  }

  /**
   * Trims new usernames and rejects invalid roles and secrets.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void trimsNewUsernamesAndRejectsInvalidRolesAndSecrets() {
    form.setUsername(" Worker ");
    form.setName("Worker");
    form.setPassword("ValidPassword!2026");
    form.setRole("WORKER");
    form.setDepartment("Operations");
    assertTrue(form.validate(mapping("create"), request).isEmpty());
    assertEquals("Worker", form.getUsername());
    form.setRole("OWNER");
    assertTrue(form.validate(mapping("create"), request).get("role").hasNext());
    assertNull(form.getPassword());
    form.setId("7");
    form.setRole("ADMIN");
    assertTrue(form.validate(mapping("role"), request).isEmpty());
  }

  /**
   * Leaves read only and input routes independent of creation fields.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void leavesReadOnlyAndInputRoutesIndependentOfCreationFields() {
    assertTrue(form.validate(mapping("list"), request).isEmpty());
    ActionMapping input = mapping("users");
    input.setType(ValidationInputAction.class.getName());
    assertTrue(form.validate(input, request).isEmpty());
    assertFalse(form.validate(mapping("create"), request).isEmpty());
    assertNull(form.getPassword());
  }
}
