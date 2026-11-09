package com.example.legacy.unit.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.legacy.support.ValidatorTestSupport;
import com.example.legacy.unit.support.WebTestSupport;
import com.example.legacy.web.AuthForm;
import org.apache.struts.Globals;
import org.junit.Before;
import org.junit.Test;

/**
 * Behavioral contracts for AuthForm.
 *
 * @author Chuck Emmons
 */
public class AuthFormTest extends WebTestSupport {

  private AuthForm form;

  /** Creates isolated collaborators and input state for each test. */
  @Before
  public void createForm() {
    form = ValidatorTestSupport.prepareForm(new AuthForm());
    attributes.put(Globals.MODULE_KEY, ValidatorTestSupport.getModuleConfiguration());
  }

  /**
   * Trims usernames and retains only valid submitted passwords.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void trimsUsernamesAndRetainsOnlyValidSubmittedPasswords() {
    form.setUsername(" worker ");
    form.setPassword("short-but-existing");
    assertTrue(form.validate(mapping("authenticate"), request).isEmpty());
    assertEquals("worker", form.getUsername());
    assertEquals("short-but-existing", form.getPassword());
    form.setUsername(null);
    assertFalse(form.validate(mapping("authenticate"), request).isEmpty());
    assertNull(form.getPassword());
    assertTrue(form.validate(mapping("login"), request).isEmpty());
  }
}
