package com.example.legacy.unit.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.legacy.service.BackOfficeService;
import com.example.legacy.service.dto.AccountDto;
import com.example.legacy.service.dto.ChangeAccountRoleDto;
import com.example.legacy.service.dto.CreateAccountDto;
import com.example.legacy.service.dto.RecordReferenceDto;
import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.unit.support.WebTestSupport;
import com.example.legacy.util.MessageKeys;
import com.example.legacy.web.PageView;
import com.example.legacy.web.UserAction;
import com.example.legacy.web.UserForm;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;

/**
 * Behavioral contracts for UserAction.
 *
 * @author Chuck Emmons
 */
public class UserActionTest extends WebTestSupport {

  private BackOfficeService service;
  private UserAction action;
  private UserForm form;

  /** Creates isolated collaborators and input state for each test. */
  @Before
  public void createAction() {
    service = mock(BackOfficeService.class);
    action = new UserAction(service);
    form = new UserForm();
    form.setId("7");
    form.setUsername("worker");
    form.setName("Worker");
    form.setPassword("ValidPassword!2026");
    form.setRole("WORKER");
    form.setDepartment("Operations");
  }

  /**
   * Lists users without retaining the submitted password.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void listsUsersWithoutRetainingTheSubmittedPassword() {
    when(service.listAllAccounts("admin"))
        .thenReturn(Collections.singletonList(AccountDto.builder().username("worker").build()));
    assertEquals(
        PageView.USERS.getDefinition(),
        action.execute(mapping("list"), form, request, response).getPath());
    assertEquals("worker", form.getUsers().get(0).getUsername());
    assertNull(form.getPassword());
  }

  /**
   * Delegates create role and disable commands.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void delegatesCreateRoleAndDisableCommands() {
    assertTrue(action.execute(mapping("create"), form, request, response).getRedirect());
    verify(service).createUser(eq("admin"), any(CreateAccountDto.class));
    assertNull(form.getPassword());
    assertTrue(action.execute(mapping("role"), form, request, response).getRedirect());
    verify(service).changeUserRole(eq("admin"), any(ChangeAccountRoleDto.class));
    assertTrue(action.execute(mapping("disable"), form, request, response).getRedirect());
    verify(service).toggleUserEnabled(eq("admin"), any(RecordReferenceDto.class));
  }

  /**
   * Preserves business errors and clears secrets on both failure paths.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void preservesBusinessErrorsAndClearsSecretsOnBothFailurePaths() {
    doThrow(new BusinessValidationException(MessageKeys.BUSINESS_INVALID_RECORD_ID))
        .when(service)
        .toggleUserEnabled(eq("admin"), any(RecordReferenceDto.class));
    assertEquals(
        "page.input", action.execute(mapping("disable"), form, request, response).getPath());
    assertNull(form.getPassword());
    form.setPassword("secret");
    assertEquals(
        PageView.ERROR.getDefinition(),
        action.execute(mapping("unknown"), form, request, response).getPath());
    assertNull(form.getPassword());
  }
}
