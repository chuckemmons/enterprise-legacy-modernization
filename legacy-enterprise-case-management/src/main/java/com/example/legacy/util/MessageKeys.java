package com.example.legacy.util;

/**
 * Message-bundle identifiers shared by Java callers; display values live in
 * ApplicationResources.properties.
 *
 * @author Chuck Emmons
 */
public final class MessageKeys {
  /** Resource key for seed admin display name. */
  public static final String SEED_ADMIN_DISPLAY_NAME = "seed.admin.display.name";

  /** Resource key for seed user display name. */
  public static final String SEED_USER_DISPLAY_NAME = "seed.user.display.name";

  /** Resource key for seed case title. */
  public static final String SEED_CASE_TITLE = "seed.case.title";

  /** Resource key for seed case description. */
  public static final String SEED_CASE_DESCRIPTION = "seed.case.description";

  /** Resource key for security invalid csrf. */
  public static final String SECURITY_INVALID_CSRF = "security.invalid.csrf";

  /** Resource key for security post required. */
  public static final String SECURITY_POST_REQUIRED = "security.post.required";

  /** Resource key for report csv header. */
  public static final String REPORT_CSV_HEADER = "report.csv.header";

  /** Resource key for audit account created. */
  public static final String AUDIT_ACCOUNT_CREATED = "audit.account.created";

  /** Resource key for audit account enabled. */
  public static final String AUDIT_ACCOUNT_ENABLED = "audit.account.enabled";

  /** Resource key for audit account role. */
  public static final String AUDIT_ACCOUNT_ROLE = "audit.account.role";

  /** Resource key for audit case archived soft delete. */
  public static final String AUDIT_CASE_ARCHIVED_SOFT_DELETE = "audit.case.archived.soft.delete";

  /** Resource key for audit case assigned. */
  public static final String AUDIT_CASE_ASSIGNED = "audit.case.assigned";

  /** Resource key for audit case details updated. */
  public static final String AUDIT_CASE_DETAILS_UPDATED = "audit.case.details.updated";

  /** Resource key for audit case opened. */
  public static final String AUDIT_CASE_OPENED = "audit.case.opened";

  /** Resource key for audit comment added. */
  public static final String AUDIT_COMMENT_ADDED = "audit.comment.added";

  /** Resource key for audit document uploaded. */
  public static final String AUDIT_DOCUMENT_UPLOADED = "audit.document.uploaded";

  /** Resource key for audit notification delivery failed will retry. */
  public static final String AUDIT_NOTIFICATION_DELIVERY_FAILED_WILL_RETRY =
      "audit.notification.delivery.failed.will.retry";

  /** Resource key for audit status changed. */
  public static final String AUDIT_STATUS_CHANGED = "audit.status.changed";

  /** Resource key for business account already exists no changes made. */
  public static final String BUSINESS_ACCOUNT_ALREADY_EXISTS_NO_CHANGES_MADE =
      "business.account.already.exists.no.changes.made";

  /** Resource key for business account unavailable. */
  public static final String BUSINESS_ACCOUNT_UNAVAILABLE = "business.account.unavailable";

  /** Resource key for business administrator permission required. */
  public static final String BUSINESS_ADMINISTRATOR_PERMISSION_REQUIRED =
      "business.administrator.permission.required";

  /** Resource key for business another user updated this case refresh and retry. */
  public static final String BUSINESS_ANOTHER_USER_UPDATED_THIS_CASE_REFRESH_AND_RETRY =
      "business.another.user.updated.this.case.refresh.and.retry";

  /** Resource key for business cannot change your own role. */
  public static final String BUSINESS_CANNOT_CHANGE_YOUR_OWN_ROLE =
      "business.cannot.change.your.own.role";

  /** Resource key for business cannot disable yourself. */
  public static final String BUSINESS_CANNOT_DISABLE_YOURSELF = "business.cannot.disable.yourself";

  /** Resource key for business case not assigned to you. */
  public static final String BUSINESS_CASE_NOT_ASSIGNED_TO_YOU =
      "business.case.not.assigned.to.you";

  /** Resource key for business case not found. */
  public static final String BUSINESS_CASE_NOT_FOUND = "business.case.not.found";

  /** Resource key for business closed cases cannot be edited. */
  public static final String BUSINESS_CLOSED_CASES_CANNOT_BE_EDITED =
      "business.closed.cases.cannot.be.edited";

  /** Resource key for business closed cases cannot be reassigned. */
  public static final String BUSINESS_CLOSED_CASES_CANNOT_BE_REASSIGNED =
      "business.closed.cases.cannot.be.reassigned";

  /** Resource key for business creator cannot self approve. */
  public static final String BUSINESS_CREATOR_CANNOT_SELF_APPROVE =
      "business.creator.cannot.self.approve";

  /** Resource key for business date must be yyyy mm dd. */
  public static final String BUSINESS_DATE_MUST_BE_YYYY_MM_DD = "business.date.must.be.yyyy.mm.dd";

  /** Resource key for business document not found. */
  public static final String BUSINESS_DOCUMENT_NOT_FOUND = "business.document.not.found";

  /** Resource key for business due date must be yyyy mm dd. */
  public static final String BUSINESS_DUE_DATE_MUST_BE_YYYY_MM_DD =
      "business.due.date.must.be.yyyy.mm.dd";

  /** Resource key for business expected overdue or email. */
  public static final String BUSINESS_EXPECTED_OVERDUE_OR_EMAIL =
      "business.expected.overdue.or.email";

  /** Resource key for business field required. */
  public static final String BUSINESS_FIELD_REQUIRED = "business.field.required";

  /** Resource key for business field too long. */
  public static final String BUSINESS_FIELD_TOO_LONG = "business.field.too.long";

  /** Resource key for business file must be 1 byte to 2 mb. */
  public static final String BUSINESS_FILE_MUST_BE_1_BYTE_TO_2_MB =
      "business.file.must.be.1.byte.to.2.mb";

  /** Resource key for business invalid grouping. */
  public static final String BUSINESS_INVALID_GROUPING = "business.invalid.grouping";

  /** Resource key for business invalid record id. */
  public static final String BUSINESS_INVALID_RECORD_ID = "business.invalid.record.id";

  /** Resource key for business invalid record version. */
  public static final String BUSINESS_INVALID_RECORD_VERSION = "business.invalid.record.version";

  /** Resource key for business invalid selection. */
  public static final String BUSINESS_INVALID_SELECTION = "business.invalid.selection";

  /** Resource key for business invalid upload token. */
  public static final String BUSINESS_INVALID_UPLOAD_TOKEN = "business.invalid.upload.token";

  /** Resource key for business notification not accessible. */
  public static final String BUSINESS_NOTIFICATION_NOT_ACCESSIBLE =
      "business.notification.not.accessible";

  /** Resource key for business only the assigned worker may change status. */
  public static final String BUSINESS_ONLY_THE_ASSIGNED_WORKER_MAY_CHANGE_STATUS =
      "business.only.the.assigned.worker.may.change.status";

  /** Resource key for business password must be at least 12 characters. */
  public static final String BUSINESS_PASSWORD_MUST_BE_AT_LEAST_12_CHARACTERS =
      "business.password.must.be.at.least.12.characters";

  /** Resource key for business pbkdf2 unavailable. */
  public static final String BUSINESS_PBKDF2_UNAVAILABLE = "business.pbkdf2.unavailable";

  /** Resource key for business please sign in. */
  public static final String BUSINESS_PLEASE_SIGN_IN = "business.please.sign.in";

  /** Resource key for business read only access. */
  public static final String BUSINESS_READ_ONLY_ACCESS = "business.read.only.access";

  /** Resource key for business reporting permission required. */
  public static final String BUSINESS_REPORTING_PERMISSION_REQUIRED =
      "business.reporting.permission.required";

  /**
   * Resource key for business set case admin user and case admin password 12 chars environment
   * variables.
   */
  public static final String
      BUSINESS_SET_CASE_ADMIN_USER_AND_CASE_ADMIN_PASSWORD_12_CHARS_ENVIRONMENT_VARIABLES =
          "business.set.case.admin.user.and.case.admin.password.12.chars.environment.variables";

  /** Resource key for business set case demo password to a nontrivial password 12 chars. */
  public static final String BUSINESS_SET_CASE_DEMO_PASSWORD_TO_A_NONTRIVIAL_PASSWORD_12_CHARS =
      "business.set.case.demo.password.to.a.nontrivial.password.12.chars";

  /** Resource key for business smtp failure. */
  public static final String BUSINESS_SMTP_FAILURE = "business.smtp.failure";

  /** Resource key for business supervisor permission required. */
  public static final String BUSINESS_SUPERVISOR_PERMISSION_REQUIRED =
      "business.supervisor.permission.required";

  /** Resource key for business transition not permitted. */
  public static final String BUSINESS_TRANSITION_NOT_PERMITTED =
      "business.transition.not.permitted";

  /** Resource key for business unknown action. */
  public static final String BUSINESS_UNKNOWN_ACTION = "business.unknown.action";

  /** Resource key for business unknown user. */
  public static final String BUSINESS_UNKNOWN_USER = "business.unknown.user";

  /** Resource key for business unsupported file type. */
  public static final String BUSINESS_UNSUPPORTED_FILE_TYPE = "business.unsupported.file.type";

  /** Resource key for business username is already taken. */
  public static final String BUSINESS_USERNAME_IS_ALREADY_TAKEN =
      "business.username.is.already.taken";

  /** Resource key for business username must start with a letter 3 60 characters. */
  public static final String BUSINESS_USERNAME_MUST_START_WITH_A_LETTER_3_60_CHARACTERS =
      "business.username.must.start.with.a.letter.3.60.characters";

  /** Resource key for confirmation case archived. */
  public static final String CONFIRMATION_CASE_ARCHIVED = "confirmation.case.archived";

  /** Resource key for confirmation case created. */
  public static final String CONFIRMATION_CASE_CREATED = "confirmation.case.created";

  /** Resource key for confirmation case updated. */
  public static final String CONFIRMATION_CASE_UPDATED = "confirmation.case.updated";

  /** Resource key for confirmation document uploaded. */
  public static final String CONFIRMATION_DOCUMENT_UPLOADED = "confirmation.document.uploaded";

  /** Resource key for confirmation notification marked as read. */
  public static final String CONFIRMATION_NOTIFICATION_MARKED_AS_READ =
      "confirmation.notification.marked.as.read";

  /** Resource key for confirmation user updated. */
  public static final String CONFIRMATION_USER_UPDATED = "confirmation.user.updated";

  /** Resource key for field assignee. */
  public static final String FIELD_ASSIGNEE = "field.assignee";

  /** Resource key for field category. */
  public static final String FIELD_CATEGORY = "field.category";

  /** Resource key for field comment. */
  public static final String FIELD_COMMENT = "field.comment";

  /** Resource key for field department. */
  public static final String FIELD_DEPARTMENT = "field.department";

  /** Resource key for field description. */
  public static final String FIELD_DESCRIPTION = "field.description";

  /** Resource key for field name. */
  public static final String FIELD_NAME = "field.name";

  /** Resource key for field next status. */
  public static final String FIELD_NEXT_STATUS = "field.next.status";

  /** Resource key for field password. */
  public static final String FIELD_PASSWORD = "field.password";

  /** Resource key for field priority. */
  public static final String FIELD_PRIORITY = "field.priority";

  /** Resource key for field role. */
  public static final String FIELD_ROLE = "field.role";

  /** Resource key for field status. */
  public static final String FIELD_STATUS = "field.status";

  /** Resource key for field title. */
  public static final String FIELD_TITLE = "field.title";

  /** Resource key for field username. */
  public static final String FIELD_USERNAME = "field.username";

  /** Resource key for notice case assigned. */
  public static final String NOTICE_CASE_ASSIGNED = "notice.case.assigned";

  /** Resource key for notice case comment. */
  public static final String NOTICE_CASE_COMMENT = "notice.case.comment";

  /** Resource key for notice case overdue. */
  public static final String NOTICE_CASE_OVERDUE = "notice.case.overdue";

  /** Resource key for notice case status. */
  public static final String NOTICE_CASE_STATUS = "notice.case.status";

  /** Resource key for notice comment body. */
  public static final String NOTICE_COMMENT_BODY = "notice.comment.body";

  /** Resource key for validation accessdenied. */
  public static final String VALIDATION_ACCESSDENIED = "validation.accessDenied";

  /** Resource key for validation business. */
  public static final String VALIDATION_BUSINESS = "validation.business";

  /** Resource key for validation choice. */
  public static final String VALIDATION_CHOICE = "validation.choice";

  /** Resource key for validation credentials. */
  public static final String VALIDATION_CREDENTIALS = "validation.credentials";

  /** Resource key for validation email. */
  public static final String VALIDATION_EMAIL = "validation.email";

  /** Resource key for validation file required. */
  public static final String VALIDATION_FILE_REQUIRED = "validation.file.required";

  /** Resource key for validation file size. */
  public static final String VALIDATION_FILE_SIZE = "validation.file.size";

  /** Resource key for validation file type. */
  public static final String VALIDATION_FILE_TYPE = "validation.file.type";

  /** Resource key for validation page. */
  public static final String VALIDATION_PAGE = "validation.page";

  /** Resource key for validation record. */
  public static final String VALIDATION_RECORD = "validation.record";

  /** Resource key for validation unexpected. */
  public static final String VALIDATION_UNEXPECTED = "validation.unexpected";

  /** Prevents instantiation of this constants class. */
  private MessageKeys() {}
}
