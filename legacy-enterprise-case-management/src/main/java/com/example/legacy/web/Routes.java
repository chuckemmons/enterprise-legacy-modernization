package com.example.legacy.web;

/**
 * Central names for Routes; values preserve the application's external contracts.
 *
 * @author Chuck Emmons
 */
public final class Routes {
  /** Browser route for login: {@code /login.do}. */
  public static final String LOGIN = "/login.do";

  /** Browser route for authenticate: {@code /authenticate.do}. */
  public static final String AUTHENTICATE = "/authenticate.do";

  /** Browser route for logout: {@code /logout.do}. */
  public static final String LOGOUT = "/logout.do";

  /** Browser route for dashboard: {@code /dashboard.do}. */
  public static final String DASHBOARD = "/dashboard.do";

  /** Browser route for cases: {@code /cases.do}. */
  public static final String CASES = "/cases.do";

  /** Browser route for case new: {@code /caseNew.do}. */
  public static final String CASE_NEW = "/caseNew.do";

  /** Browser route for case create: {@code /caseCreate.do}. */
  public static final String CASE_CREATE = "/caseCreate.do";

  /** Browser route for case view: {@code /caseView.do}. */
  public static final String CASE_VIEW = "/caseView.do";

  /** Browser route for case update: {@code /caseUpdate.do}. */
  public static final String CASE_UPDATE = "/caseUpdate.do";

  /** Browser route for case assign: {@code /caseAssign.do}. */
  public static final String CASE_ASSIGN = "/caseAssign.do";

  /** Browser route for case transition: {@code /caseTransition.do}. */
  public static final String CASE_TRANSITION = "/caseTransition.do";

  /** Browser route for case comment: {@code /caseComment.do}. */
  public static final String CASE_COMMENT = "/caseComment.do";

  /** Browser route for case archive: {@code /caseArchive.do}. */
  public static final String CASE_ARCHIVE = "/caseArchive.do";

  /** Browser route for case upload: {@code /caseUpload.do}. */
  public static final String CASE_UPLOAD = "/caseUpload.do";

  /** Browser route for case download: {@code /caseDownload.do}. */
  public static final String CASE_DOWNLOAD = "/caseDownload.do";

  /** Browser route for users: {@code /users.do}. */
  public static final String USERS = "/users.do";

  /** Browser route for user create: {@code /userCreate.do}. */
  public static final String USER_CREATE = "/userCreate.do";

  /** Browser route for user role: {@code /userRole.do}. */
  public static final String USER_ROLE = "/userRole.do";

  /** Browser route for user disable: {@code /userDisable.do}. */
  public static final String USER_DISABLE = "/userDisable.do";

  /** Browser route for audit: {@code /audit.do}. */
  public static final String AUDIT = "/audit.do";

  /** Browser route for reports: {@code /reports.do}. */
  public static final String REPORTS = "/reports.do";

  /** Browser route for reports export: {@code /reportsExport.do}. */
  public static final String REPORTS_EXPORT = "/reportsExport.do";

  /** Browser route for notifications: {@code /notifications.do}. */
  public static final String NOTIFICATIONS = "/notifications.do";

  /** Browser route for notification read: {@code /notificationRead.do}. */
  public static final String NOTIFICATION_READ = "/notificationRead.do";

  /** Browser route for case input: {@code /caseInput.do}. */
  public static final String CASE_INPUT = "/caseInput.do";

  /** Browser route for cases input: {@code /casesInput.do}. */
  public static final String CASES_INPUT = "/casesInput.do";

  /** Browser route for users input: {@code /usersInput.do}. */
  public static final String USERS_INPUT = "/usersInput.do";

  /** Browser route for audit input: {@code /auditInput.do}. */
  public static final String AUDIT_INPUT = "/auditInput.do";

  /** Browser route for notifications input: {@code /notificationsInput.do}. */
  public static final String NOTIFICATIONS_INPUT = "/notificationsInput.do";

  /** Browser route for reports input: {@code /reportsInput.do}. */
  public static final String REPORTS_INPUT = "/reportsInput.do";

  /** Browser route for dashboard input: {@code /dashboardInput.do}. */
  public static final String DASHBOARD_INPUT = "/dashboardInput.do";

  /** Prevents instantiation of this constants class. */
  private Routes() {}

  /**
   * Returns an immutable map for EL navigation using the same route constants as Actions.
   *
   * @return an immutable map for EL navigation using the same route constants as Actions
   */
  public static java.util.Map<String, String> getPaths() {
    java.util.Map<String, String> paths = new java.util.LinkedHashMap<>();
    paths.put("LOGIN", LOGIN);
    paths.put("AUTHENTICATE", AUTHENTICATE);
    paths.put("LOGOUT", LOGOUT);
    paths.put("DASHBOARD", DASHBOARD);
    paths.put("CASES", CASES);
    paths.put("CASE_NEW", CASE_NEW);
    paths.put("CASE_CREATE", CASE_CREATE);
    paths.put("CASE_VIEW", CASE_VIEW);
    paths.put("CASE_UPDATE", CASE_UPDATE);
    paths.put("CASE_ASSIGN", CASE_ASSIGN);
    paths.put("CASE_TRANSITION", CASE_TRANSITION);
    paths.put("CASE_COMMENT", CASE_COMMENT);
    paths.put("CASE_ARCHIVE", CASE_ARCHIVE);
    paths.put("CASE_UPLOAD", CASE_UPLOAD);
    paths.put("CASE_DOWNLOAD", CASE_DOWNLOAD);
    paths.put("USERS", USERS);
    paths.put("USER_CREATE", USER_CREATE);
    paths.put("USER_ROLE", USER_ROLE);
    paths.put("USER_DISABLE", USER_DISABLE);
    paths.put("AUDIT", AUDIT);
    paths.put("REPORTS", REPORTS);
    paths.put("REPORTS_EXPORT", REPORTS_EXPORT);
    paths.put("NOTIFICATIONS", NOTIFICATIONS);
    paths.put("NOTIFICATION_READ", NOTIFICATION_READ);
    paths.put("CASE_INPUT", CASE_INPUT);
    paths.put("CASES_INPUT", CASES_INPUT);
    paths.put("USERS_INPUT", USERS_INPUT);
    paths.put("AUDIT_INPUT", AUDIT_INPUT);
    paths.put("NOTIFICATIONS_INPUT", NOTIFICATIONS_INPUT);
    paths.put("REPORTS_INPUT", REPORTS_INPUT);
    paths.put("DASHBOARD_INPUT", DASHBOARD_INPUT);
    return java.util.Collections.unmodifiableMap(paths);
  }
}
