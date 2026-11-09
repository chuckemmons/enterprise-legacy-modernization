package com.example.legacy.web;

import com.example.legacy.service.dto.NoticeDto;
import com.example.legacy.service.dto.NotificationPageDto;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;

/**
 * Request-scoped Struts adapter for NotificationPageDto input and output.
 *
 * @author Chuck Emmons
 */
public final class NotificationForm extends BasePageForm {
  private static final long serialVersionUID = 1L;
  private NotificationPageDto data = NotificationPageDto.builder().build();

  /**
   * Returns notices loaded from a service DTO.
   *
   * @return notices loaded from a service DTO
   */
  public List<NoticeDto> getNotices() {
    return data.getNotices();
  }

  /**
   * Creates a service DTO from bound fields; never passes the ActionForm to a service.
   *
   * @return the notification page DTO
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   */
  public NotificationPageDto getData() {
    return getPageData(NotificationPageDto.builder()).notices(data.getNotices()).build();
  }

  /**
   * Populates this form from service response data for view rendering.
   *
   * @param source the source value to copy or map
   * @throws NullPointerException if {@code source} is {@code null}
   */
  public void setData(NotificationPageDto source) {
    data = source;
    setPageData(source);
  }

  /**
   * Checks the submitted notification identifier; recipient ownership is enforced by the service.
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
