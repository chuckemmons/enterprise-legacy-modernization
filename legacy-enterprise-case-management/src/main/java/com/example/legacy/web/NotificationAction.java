package com.example.legacy.web;

import com.example.legacy.service.NotificationService;
import com.example.legacy.service.dto.NotificationPageDto;
import com.example.legacy.service.dto.RecordReferenceDto;
import com.example.legacy.util.MessageKeys;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Lists and updates recipient notifications through form and service DTO boundaries.
 *
 * @author Chuck Emmons
 */
public final class NotificationAction extends Action {
  private final NotificationService notificationService;

  /** Creates this action and its reusable service collaborators. */
  public NotificationAction() {
    this(new NotificationService());
  }

  /**
   * Accepts explicitly supplied application collaborators for request-processing tests.
   *
   * @param notificationService the notification service collaborator
   * @throws NullPointerException if {@code notificationService} is {@code null}
   */
  public NotificationAction(NotificationService notificationService) {
    this.notificationService =
        java.util.Objects.requireNonNull(notificationService, "notificationService");
  }

  /**
   * Maps list/read form data to a DTO and populates service results back into the form.
   *
   * @param mapping the Struts mapping for the current operation
   * @param form the form containing the submitted values
   * @param request the current HTTP request
   * @param response the HTTP response to populate
   * @return the next page forward or redirect
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws NullPointerException if {@code request} is {@code null}
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   */
  @Override
  public ActionForward execute(
      ActionMapping mapping,
      ActionForm form,
      HttpServletRequest request,
      HttpServletResponse response) {
    try {
      NotificationForm notificationForm = (NotificationForm) form;
      NotificationPageDto data = notificationForm.getData();
      String actor = WebUtil.getAuthenticatedUsername(request);
      if (ActionOperation.LIST.matchesParameter(mapping.getParameter())) {
        notificationForm.setData(
            NotificationPageDto.builder(data)
                .notices(notificationService.listNotifications(actor))
                .build());
        return WebUtil.createViewForward(PageView.NOTIFICATIONS);
      }
      notificationService.markNotificationRead(
          actor, RecordReferenceDto.builder().id(data.getId()).build());
      notificationForm.setData(data);
      return WebUtil.createRedirect(
          request, Routes.NOTIFICATIONS, MessageKeys.CONFIRMATION_NOTIFICATION_MARKED_AS_READ);
    } catch (Exception exception) {
      return WebUtil.createFailureForward(request, exception);
    }
  }
}
