package com.example.legacy.web;

import com.example.legacy.service.dto.AuditEventDto;
import com.example.legacy.service.dto.AuditPageDto;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;

/**
 * Request-scoped Struts adapter for AuditPageDto input and output.
 *
 * @author Chuck Emmons
 */
public final class AuditForm extends BasePageForm {
  private static final long serialVersionUID = 1L;
  private AuditPageDto data = AuditPageDto.builder().build();

  /**
   * Returns events loaded from a service DTO.
   *
   * @return events loaded from a service DTO
   */
  public List<AuditEventDto> getEvents() {
    return data.getEvents();
  }

  /**
   * Creates a service DTO from bound fields; never passes the ActionForm to a service.
   *
   * @return the audit page DTO
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   */
  public AuditPageDto getData() {
    return getPageData(AuditPageDto.builder()).events(data.getEvents()).build();
  }

  /**
   * Populates this form from service response data for view rendering.
   *
   * @param source the source value to copy or map
   * @throws NullPointerException if {@code source} is {@code null}
   */
  public void setData(AuditPageDto source) {
    data = source;
    setPageData(source);
  }

  /**
   * Validates shared bound field syntax before this read-only page reaches its action.
   *
   * @param mapping the Struts mapping for the current operation
   * @param request the current HTTP request
   * @return the validation errors, or an empty collection when the submitted values are valid
   * @throws NullPointerException if {@code mapping} or {@code request} is {@code null}
   */
  @Override
  public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
    return super.validate(mapping, request);
  }
}
