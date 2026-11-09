package com.example.legacy.web;

import com.example.legacy.service.dto.AccountDto;
import com.example.legacy.service.dto.AuthDto;
import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;

/**
 * Request-scoped Struts adapter for AuthDto input and output.
 *
 * @author Chuck Emmons
 */
public final class AuthForm extends BasePageForm {
  private static final long serialVersionUID = 1L;
  private AuthDto data = AuthDto.builder().build();
  private String username;
  private String password;

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
   * Returns account loaded from a service DTO.
   *
   * @return account loaded from a service DTO
   */
  public AccountDto getAccount() {
    return data.getAccount();
  }

  /**
   * Creates a service DTO from bound fields; never passes the ActionForm to a service.
   *
   * @return the auth DTO
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   */
  public AuthDto getData() {
    return getPageData(AuthDto.builder())
        .account(data.getAccount())
        .username(username)
        .password(password)
        .build();
  }

  /**
   * Populates this form from service response data for view rendering.
   *
   * @param source the source value to copy or map
   * @throws NullPointerException if {@code source} is {@code null}
   */
  public void setData(AuthDto source) {
    data = AuthDto.builder(source).password(null).build();
    setPageData(source);
    username = source.getUsername();
    password = null;
  }

  /**
   * Validates only authentication submissions, allowing the initial login page to render empty.
   *
   * @param mapping the Struts mapping for the current operation
   * @param request the current HTTP request
   * @return the validation errors, or an empty collection when the submitted values are valid
   * @throws NullPointerException if {@code mapping} or {@code request} is {@code null}
   */
  @Override
  public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
    if (username != null) {
      username = username.trim();
    }
    ActionErrors errors = super.validate(mapping, request);
    if (!errors.isEmpty()) {
      password = null;
    }
    return errors;
  }
}
