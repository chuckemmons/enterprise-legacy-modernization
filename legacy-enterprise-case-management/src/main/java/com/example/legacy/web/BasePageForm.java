package com.example.legacy.web;

import com.example.legacy.service.dto.PageDto;
import com.example.legacy.util.ContentTypes;
import com.example.legacy.util.MessageKeys;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.validator.ValidatorForm;

import javax.servlet.http.HttpServletRequest;

/**
 * Common request-scoped Struts fields; session identity is never populated from submitted data.
 *
 * @author Chuck Emmons
 */
public abstract class BasePageForm extends ValidatorForm {
  private static final long serialVersionUID = 1L;
  private String id;
  private String pageNumber = "0";
  private String csrf;

  /**
   * Returns the submitted record identifier.
   *
   * @return the submitted record identifier
   */
  public String getId() {
    return id;
  }

  /**
   * Binds the submitted record identifier.
   *
   * @param value the record identifier to set
   */
  public void setId(String value) {
    id = value;
  }

  /**
   * Returns the submitted page index.
   *
   * @return the submitted page index
   */
  public String getPageNumber() {
    return pageNumber;
  }

  /**
   * Binds the submitted page index.
   *
   * @param value the zero-based page index to set
   */
  public void setPageNumber(String value) {
    pageNumber = value;
  }

  /**
   * Returns the submitted CSRF token, which is validated only in the HTTP layer.
   *
   * @return the submitted CSRF token, which is validated only in the HTTP layer
   */
  public String getCsrf() {
    return csrf;
  }

  /**
   * Binds the submitted CSRF token.
   *
   * @param value the CSRF token to set
   */
  public void setCsrf(String value) {
    csrf = value;
  }

  /**
   * Chooses operation-specific XML rules when one form serves multiple Struts mappings.
   *
   * @param mapping the Struts mapping for the current operation
   * @param request the current HTTP request
   * @return the validation key
   * @throws NullPointerException if {@code mapping} is {@code null}
   */
  @Override
  public String getValidationKey(ActionMapping mapping, HttpServletRequest request) {
    String operation = mapping.getParameter();
    return getClass().getSimpleName() + (operation == null ? "" : "." + operation);
  }

  /**
   * Keeps legacy page query parameters compatible while reserving ValidatorForm.page for validation
   * stages.
   *
   * @param mapping the Struts mapping for the current operation
   * @param request the current HTTP request
   * @throws NullPointerException if {@code request} is {@code null}
   */
  @Override
  public void reset(ActionMapping mapping, HttpServletRequest request) {
    super.reset(mapping, request);
    pageNumber = "0";
    String contentType = request.getContentType();

    if (contentType != null
        && contentType
            .toLowerCase(java.util.Locale.ROOT)
            .startsWith(ContentTypes.MULTIPART_PREFIX)) {
      return;
    }

    String value = request.getParameter(WebKeys.PAGE_NUMBER);
    if (value == null) {
      value = request.getParameter(WebKeys.LEGACY_PAGE);
    }
    if (value != null) {
      pageNumber = value;
    }
  }

  /**
   * Runs declarative rules plus shared syntax checks, without loading entities or applying business
   * policy.
   *
   * @param mapping the Struts mapping for the current operation
   * @param request the current HTTP request
   * @return the validation errors, or an empty collection when the submitted values are valid
   * @throws NullPointerException if {@code mapping} or {@code request} is {@code null}
   */
  @Override
  public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
    // These are single-stage forms. A submitted legacy page value must never skip XML rules.
    super.setPage(0);

    if (isInputForward(mapping)) {
      pageNumber = Integer.toString(FormDataUtil.parsePageIndex(pageNumber));
      return new ActionErrors();
    }

    request.setAttribute(WebKeys.VALIDATION_OPERATION, mapping.getParameter());

    try {
      request.setAttribute(WebKeys.RECORD_ID, id == null || id.isEmpty() ? null : Long.valueOf(id));
    } catch (NumberFormatException invalid) {
      request.setAttribute(WebKeys.RECORD_ID, "invalid");
    }

    ActionErrors errors = super.validate(mapping, request);

    FormValidationUtil.validateRecordId(errors, id);

    if (!FormValidationUtil.isBlank(pageNumber)) {
      try {
        int index = Integer.parseInt(pageNumber);
        if (index < 0 || index > com.example.legacy.util.ValidationLimits.MAX_PAGE) {
          throw new NumberFormatException();
        }
      } catch (NumberFormatException invalidPage) {
        errors.add(FormFields.PAGE, new ActionMessage(MessageKeys.VALIDATION_PAGE));
        pageNumber = "0";
      }
    }

    return errors;
  }

  /**
   * Copies parsed common input to a service DTO without including CSRF or session identity.
   *
   * @param <B> the concrete builder type
   * @param target the builder to populate
   * @return the supplied builder populated with the parsed record identifier and page index
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   * @throws NullPointerException if {@code target} is {@code null}
   */
  protected <B extends PageDto.Builder<B>> B getPageData(B target) {
    return target.id(FormDataUtil.parseRecordId(id)).page(FormDataUtil.parsePageIndex(pageNumber));
  }

  /**
   * Copies common response data into this request-scoped form.
   *
   * @param source the source value to copy or map
   * @throws NullPointerException if {@code source} is {@code null}
   */
  protected void setPageData(PageDto source) {
    id = source.getId() == 0 ? null : Long.toString(source.getId());
    pageNumber = Integer.toString(source.getPage());
  }

  /**
   * Identifies read-only input forwards that must not repeat submission validation.
   *
   * @param mapping the Struts mapping for the current operation
   * @return {@code true} if the mapping is an input forward; {@code false} otherwise
   * @throws NullPointerException if {@code mapping} is {@code null}
   */
  protected boolean isInputForward(ActionMapping mapping) {
    return ValidationInputAction.class.getName().equals(mapping.getType());
  }
}
