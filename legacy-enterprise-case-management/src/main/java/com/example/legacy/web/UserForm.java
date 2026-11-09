package com.example.legacy.web;

import com.example.legacy.service.dto.AccountDto;
import com.example.legacy.service.dto.ChangeAccountRoleDto;
import com.example.legacy.service.dto.CreateAccountDto;
import com.example.legacy.service.dto.RecordReferenceDto;
import com.example.legacy.service.dto.UserPageDto;
import com.example.legacy.util.MessageKeys;
import com.example.legacy.util.MessagesUtil;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;

/**
 * Request-scoped Struts adapter for UserPageDto input and output.
 *
 * @author Chuck Emmons
 */
public final class UserForm extends BasePageForm {
  private static final long serialVersionUID = 1L;
  private UserPageDto data = UserPageDto.builder().build();
  private String username;
  private String password;
  private String name;
  private String email;
  private String role;
  private String department;

  /**
   * Returns the bound username field.
   *
   * @return the bound username field
   */
  public String getUsername() {
    return username;
  }

  /**
   * Binds the submitted username field.
   *
   * @param value the username to set
   */
  public void setUsername(String value) {
    username = value;
  }

  /**
   * Returns the bound password field.
   *
   * @return the bound password field
   */
  public String getPassword() {
    return password;
  }

  /**
   * Binds the submitted password field.
   *
   * @param value the password to set
   */
  public void setPassword(String value) {
    password = value;
  }

  /**
   * Returns the bound name field.
   *
   * @return the bound name field
   */
  public String getName() {
    return name;
  }

  /**
   * Binds the submitted name field.
   *
   * @param value the name to set
   */
  public void setName(String value) {
    name = value;
  }

  /**
   * Returns the bound email field.
   *
   * @return the bound email field
   */
  public String getEmail() {
    return email;
  }

  /**
   * Binds the submitted email field.
   *
   * @param value the email to set
   */
  public void setEmail(String value) {
    email = value;
  }

  /**
   * Returns the bound role field.
   *
   * @return the bound role field
   */
  public String getRole() {
    return role;
  }

  /**
   * Binds the submitted role field.
   *
   * @param value the role to set
   */
  public void setRole(String value) {
    role = value;
  }

  /**
   * Returns the bound department field.
   *
   * @return the bound department field
   */
  public String getDepartment() {
    return department;
  }

  /**
   * Binds the submitted department field.
   *
   * @param value the department to set
   */
  public void setDepartment(String value) {
    department = value;
  }

  /**
   * Returns users loaded from a service DTO.
   *
   * @return users loaded from a service DTO
   */
  public List<AccountDto> getUsers() {
    return data.getUsers();
  }

  /**
   * Creates a service DTO from bound fields; never passes the ActionForm to a service.
   *
   * @return the user page DTO
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   */
  public UserPageDto getData() {
    long id = FormDataUtil.parseRecordId(getId());
    AccountDto account =
        AccountDto.builder()
            .id(id == 0 ? null : id)
            .username(username)
            .displayName(name)
            .email(email)
            .role(role)
            .department(department)
            .build();
    return getPageData(UserPageDto.builder())
        .users(data.getUsers())
        .account(account)
        .password(password)
        .build();
  }

  /**
   * Populates this form from service response data for view rendering.
   *
   * @param source the source value to copy or map
   * @throws NullPointerException if {@code source} is {@code null}
   */
  public void setData(UserPageDto source) {
    data = UserPageDto.builder(source).password(null).build();
    setPageData(source);
    password = null;
    if (source.getAccount() != null) {
      username = source.getAccount().getUsername();
      name = source.getAccount().getDisplayName();
      email = source.getAccount().getEmail();
      role = source.getAccount().getRole();
      department = source.getAccount().getDepartment();
    }
  }

  /**
   * Validates account controls for the mapped operation; role authority and uniqueness remain
   * service rules.
   *
   * @param mapping the Struts mapping for the current operation
   * @param request the current HTTP request
   * @return the validation errors, or an empty collection when the submitted values are valid
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws NullPointerException if {@code mapping} or {@code request} is {@code null}
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   */
  @Override
  public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
    if (ActionOperation.CREATE.matchesParameter(mapping.getParameter()) && username != null) {
      username = username.trim();
    }
    ActionErrors errors = super.validate(mapping, request);
    if (isInputForward(mapping)) {
      return errors;
    }
    String operation = mapping.getParameter();
    if (ActionOperation.CREATE.matchesParameter(operation)
        || ActionOperation.ROLE.matchesParameter(operation)) {
      FormValidationUtil.validateChoice(
          errors,
          FormFields.ROLE,
          MessagesUtil.formatMessage(MessageKeys.FIELD_ROLE),
          role,
          com.example.legacy.domain.Role.getCodes());
    }
    if (!errors.isEmpty()) {
      password = null;
    }
    return errors;
  }

  /**
   * Refreshes the account list while preserving submitted nonsecret controls for correction.
   *
   * @param source the source value to copy or map
   * @throws NullPointerException if {@code source} is {@code null}
   */
  public void setDisplayData(UserPageDto source) {
    data = UserPageDto.builder(source).password(null).build();
    password = null;
  }

  /**
   * Maps only the controls required by the getCreateData application operation.
   *
   * @return the create account DTO
   */
  public CreateAccountDto getCreateData() {
    return CreateAccountDto.builder()
        .username(getUsername())
        .password(getPassword())
        .displayName(getName())
        .email(getEmail())
        .role(getRole())
        .department(getDepartment())
        .build();
  }

  /**
   * Maps only the controls required by the getRoleData application operation.
   *
   * @return the change account role DTO
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   */
  public ChangeAccountRoleDto getRoleData() {
    return ChangeAccountRoleDto.builder()
        .id(FormDataUtil.parseRecordId(getId()))
        .role(getRole())
        .build();
  }

  /**
   * Maps only the controls required by the getRecordData application operation.
   *
   * @return the record reference DTO
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   */
  public RecordReferenceDto getRecordData() {
    return RecordReferenceDto.builder().id(FormDataUtil.parseRecordId(getId())).build();
  }
}
