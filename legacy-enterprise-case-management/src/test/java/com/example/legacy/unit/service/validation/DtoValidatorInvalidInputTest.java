package com.example.legacy.unit.service.validation;

import static com.example.legacy.unit.service.validation.DtoValidationFixtures.createValidAccount;
import static com.example.legacy.unit.service.validation.DtoValidationFixtures.createValidAuditEvent;
import static com.example.legacy.unit.service.validation.DtoValidationFixtures.createValidCase;
import static com.example.legacy.unit.service.validation.DtoValidationFixtures.createValidDocument;
import static com.example.legacy.unit.service.validation.DtoValidationFixtures.createValidNotice;
import static com.example.legacy.unit.service.validation.DtoValidationFixtures.createValidUpdate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.example.legacy.service.dto.AddCaseCommentDto;
import com.example.legacy.service.dto.AssignCaseDto;
import com.example.legacy.service.dto.AuditEventDto;
import com.example.legacy.service.dto.AuthDto;
import com.example.legacy.service.dto.CaseSearchCriteriaDto;
import com.example.legacy.service.dto.ChangeAccountRoleDto;
import com.example.legacy.service.dto.CreateAccountDto;
import com.example.legacy.service.dto.CreateCaseDto;
import com.example.legacy.service.dto.DocumentDto;
import com.example.legacy.service.dto.NoticeDto;
import com.example.legacy.service.dto.PageRequestDto;
import com.example.legacy.service.dto.RecordReferenceDto;
import com.example.legacy.service.dto.TransitionCaseDto;
import com.example.legacy.service.dto.UpdateCaseDto;
import com.example.legacy.service.dto.UploadDocumentDto;
import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.service.validation.AddCaseCommentDtoValidator;
import com.example.legacy.service.validation.AssignCaseDtoValidator;
import com.example.legacy.service.validation.AuditEventDtoValidator;
import com.example.legacy.service.validation.AuthDtoValidator;
import com.example.legacy.service.validation.CaseSearchCriteriaDtoValidator;
import com.example.legacy.service.validation.ChangeAccountRoleDtoValidator;
import com.example.legacy.service.validation.CreateAccountDtoValidator;
import com.example.legacy.service.validation.CreateCaseDtoValidator;
import com.example.legacy.service.validation.DocumentDtoValidator;
import com.example.legacy.service.validation.NoticeDtoValidator;
import com.example.legacy.service.validation.PageRequestDtoValidator;
import com.example.legacy.service.validation.RecordReferenceDtoValidator;
import com.example.legacy.service.validation.TransitionCaseDtoValidator;
import com.example.legacy.service.validation.UpdateCaseDtoValidator;
import com.example.legacy.service.validation.UploadDocumentDtoValidator;
import com.example.legacy.util.MessageKeys;
import com.example.legacy.util.ValidationLimits;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Exercises invalid DTO fields without database or mapper collaborators.
 *
 * @author Chuck Emmons
 */
@RunWith(Parameterized.class)
public class DtoValidatorInvalidInputTest {
  private final Runnable operation;
  private final String expectedKey;

  /**
   * Retains an independently invalid input and its expected application error.
   *
   * @param label the field name displayed by JUnit
   * @param operation the DTO validation to execute
   * @param expectedKey the expected message resource key
   */
  public DtoValidatorInvalidInputTest(String label, Runnable operation, String expectedKey) {
    this.operation = operation;
    this.expectedKey = expectedKey;
  }

  /**
   * Supplies field-specific invalid input, including nested content and numeric boundaries.
   *
   * @return the named validation cases
   * @throws BusinessValidationException if a required field is missing, malformed, or outside its
   *     supported range
   */
  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> invalidInputs() {
    return Arrays.asList(
        new Object[][] {
          {
            "update/version/required",
            (Runnable)
                () ->
                    new UpdateCaseDtoValidator()
                        .validate(UpdateCaseDto.builder(createValidUpdate()).version(null).build()),
            MessageKeys.BUSINESS_INVALID_RECORD_VERSION
          },
          {
            "CreateCaseDto/title/required",
            (Runnable)
                () ->
                    new CreateCaseDtoValidator()
                        .validate(CreateCaseDto.builder(createValidCase()).title(" ").build()),
            MessageKeys.BUSINESS_FIELD_REQUIRED
          },
          {
            "CreateCaseDto/title/maximum",
            (Runnable)
                () ->
                    new CreateCaseDtoValidator()
                        .validate(
                            CreateCaseDto.builder(createValidCase())
                                .title(repeat(ValidationLimits.TITLE + 1))
                                .build()),
            MessageKeys.BUSINESS_FIELD_TOO_LONG
          },
          {
            "CreateCaseDto/description/required",
            (Runnable)
                () ->
                    new CreateCaseDtoValidator()
                        .validate(
                            CreateCaseDto.builder(createValidCase()).description(" ").build()),
            MessageKeys.BUSINESS_FIELD_REQUIRED
          },
          {
            "CreateCaseDto/description/maximum",
            (Runnable)
                () ->
                    new CreateCaseDtoValidator()
                        .validate(
                            CreateCaseDto.builder(createValidCase())
                                .description(repeat(ValidationLimits.DESCRIPTION + 1))
                                .build()),
            MessageKeys.BUSINESS_FIELD_TOO_LONG
          },
          {
            "CreateCaseDto/category/required",
            (Runnable)
                () ->
                    new CreateCaseDtoValidator()
                        .validate(CreateCaseDto.builder(createValidCase()).category(" ").build()),
            MessageKeys.BUSINESS_FIELD_REQUIRED
          },
          {
            "CreateCaseDto/category/maximum",
            (Runnable)
                () ->
                    new CreateCaseDtoValidator()
                        .validate(
                            CreateCaseDto.builder(createValidCase())
                                .category(repeat(ValidationLimits.CATEGORY + 1))
                                .build()),
            MessageKeys.BUSINESS_FIELD_TOO_LONG
          },
          {
            "CreateCaseDto/department/required",
            (Runnable)
                () ->
                    new CreateCaseDtoValidator()
                        .validate(CreateCaseDto.builder(createValidCase()).department(" ").build()),
            MessageKeys.BUSINESS_FIELD_REQUIRED
          },
          {
            "CreateCaseDto/department/maximum",
            (Runnable)
                () ->
                    new CreateCaseDtoValidator()
                        .validate(
                            CreateCaseDto.builder(createValidCase())
                                .department(repeat(ValidationLimits.DEPARTMENT + 1))
                                .build()),
            MessageKeys.BUSINESS_FIELD_TOO_LONG
          },
          {
            "UpdateCaseDto/title/required",
            (Runnable)
                () ->
                    new UpdateCaseDtoValidator()
                        .validate(UpdateCaseDto.builder(createValidUpdate()).title(" ").build()),
            MessageKeys.BUSINESS_FIELD_REQUIRED
          },
          {
            "UpdateCaseDto/title/maximum",
            (Runnable)
                () ->
                    new UpdateCaseDtoValidator()
                        .validate(
                            UpdateCaseDto.builder(createValidUpdate())
                                .title(repeat(ValidationLimits.TITLE + 1))
                                .build()),
            MessageKeys.BUSINESS_FIELD_TOO_LONG
          },
          {
            "UpdateCaseDto/description/required",
            (Runnable)
                () ->
                    new UpdateCaseDtoValidator()
                        .validate(
                            UpdateCaseDto.builder(createValidUpdate()).description(" ").build()),
            MessageKeys.BUSINESS_FIELD_REQUIRED
          },
          {
            "UpdateCaseDto/description/maximum",
            (Runnable)
                () ->
                    new UpdateCaseDtoValidator()
                        .validate(
                            UpdateCaseDto.builder(createValidUpdate())
                                .description(repeat(ValidationLimits.DESCRIPTION + 1))
                                .build()),
            MessageKeys.BUSINESS_FIELD_TOO_LONG
          },
          {
            "CreateAccountDto/username/required",
            (Runnable)
                () ->
                    new CreateAccountDtoValidator()
                        .validate(
                            CreateAccountDto.builder(createValidAccount()).username(" ").build()),
            MessageKeys.BUSINESS_FIELD_REQUIRED
          },
          {
            "CreateAccountDto/username/maximum",
            (Runnable)
                () ->
                    new CreateAccountDtoValidator()
                        .validate(
                            CreateAccountDto.builder(createValidAccount())
                                .username(repeat(ValidationLimits.USERNAME + 1))
                                .build()),
            MessageKeys.BUSINESS_FIELD_TOO_LONG
          },
          {
            "CreateAccountDto/displayName/required",
            (Runnable)
                () ->
                    new CreateAccountDtoValidator()
                        .validate(
                            CreateAccountDto.builder(createValidAccount())
                                .displayName(" ")
                                .build()),
            MessageKeys.BUSINESS_FIELD_REQUIRED
          },
          {
            "CreateAccountDto/displayName/maximum",
            (Runnable)
                () ->
                    new CreateAccountDtoValidator()
                        .validate(
                            CreateAccountDto.builder(createValidAccount())
                                .displayName(repeat(ValidationLimits.DISPLAY_NAME + 1))
                                .build()),
            MessageKeys.BUSINESS_FIELD_TOO_LONG
          },
          {
            "CreateAccountDto/department/required",
            (Runnable)
                () ->
                    new CreateAccountDtoValidator()
                        .validate(
                            CreateAccountDto.builder(createValidAccount()).department(" ").build()),
            MessageKeys.BUSINESS_FIELD_REQUIRED
          },
          {
            "CreateAccountDto/department/maximum",
            (Runnable)
                () ->
                    new CreateAccountDtoValidator()
                        .validate(
                            CreateAccountDto.builder(createValidAccount())
                                .department(repeat(ValidationLimits.DEPARTMENT + 1))
                                .build()),
            MessageKeys.BUSINESS_FIELD_TOO_LONG
          },
          {
            "CreateCaseDto/priority/null",
            (Runnable)
                () ->
                    new CreateCaseDtoValidator()
                        .validate(CreateCaseDto.builder(createValidCase()).priority(null).build()),
            MessageKeys.BUSINESS_INVALID_SELECTION
          },
          {
            "CreateCaseDto/priority/\"URGENT\"",
            (Runnable)
                () ->
                    new CreateCaseDtoValidator()
                        .validate(
                            CreateCaseDto.builder(createValidCase()).priority("URGENT").build()),
            MessageKeys.BUSINESS_INVALID_SELECTION
          },
          {
            "UpdateCaseDto/priority/null",
            (Runnable)
                () ->
                    new UpdateCaseDtoValidator()
                        .validate(
                            UpdateCaseDto.builder(createValidUpdate()).priority(null).build()),
            MessageKeys.BUSINESS_INVALID_SELECTION
          },
          {
            "UpdateCaseDto/priority/\"URGENT\"",
            (Runnable)
                () ->
                    new UpdateCaseDtoValidator()
                        .validate(
                            UpdateCaseDto.builder(createValidUpdate()).priority("URGENT").build()),
            MessageKeys.BUSINESS_INVALID_SELECTION
          },
          {
            "update/id",
            (Runnable)
                () ->
                    new UpdateCaseDtoValidator()
                        .validate(UpdateCaseDto.builder(createValidUpdate()).id(0).build()),
            MessageKeys.BUSINESS_INVALID_RECORD_ID
          },
          {
            "update/version",
            (Runnable)
                () ->
                    new UpdateCaseDtoValidator()
                        .validate(UpdateCaseDto.builder(createValidUpdate()).version(-1).build()),
            MessageKeys.BUSINESS_INVALID_RECORD_VERSION
          },
          {
            "account/username/pattern/\"bad name\"",
            (Runnable)
                () ->
                    new CreateAccountDtoValidator()
                        .validate(
                            CreateAccountDto.builder(createValidAccount())
                                .username("bad name")
                                .build()),
            MessageKeys.BUSINESS_USERNAME_MUST_START_WITH_A_LETTER_3_60_CHARACTERS
          },
          {
            "account/username/pattern/\"1worker\"",
            (Runnable)
                () ->
                    new CreateAccountDtoValidator()
                        .validate(
                            CreateAccountDto.builder(createValidAccount())
                                .username("1worker")
                                .build()),
            MessageKeys.BUSINESS_USERNAME_MUST_START_WITH_A_LETTER_3_60_CHARACTERS
          },
          {
            "account/password/null",
            (Runnable)
                () ->
                    new CreateAccountDtoValidator()
                        .validate(
                            CreateAccountDto.builder(createValidAccount()).password(null).build()),
            MessageKeys.BUSINESS_PASSWORD_MUST_BE_AT_LEAST_12_CHARACTERS
          },
          {
            "account/password/\"short\"",
            (Runnable)
                () ->
                    new CreateAccountDtoValidator()
                        .validate(
                            CreateAccountDto.builder(createValidAccount())
                                .password("short")
                                .build()),
            MessageKeys.BUSINESS_PASSWORD_MUST_BE_AT_LEAST_12_CHARACTERS
          },
          {
            "account/email/syntax",
            (Runnable)
                () ->
                    new CreateAccountDtoValidator()
                        .validate(
                            CreateAccountDto.builder(createValidAccount())
                                .email("bad-email")
                                .build()),
            MessageKeys.VALIDATION_EMAIL
          },
          {
            "account/email/maximum",
            (Runnable)
                () ->
                    new CreateAccountDtoValidator()
                        .validate(
                            CreateAccountDto.builder(createValidAccount())
                                .email(repeat(ValidationLimits.EMAIL + 1))
                                .build()),
            MessageKeys.BUSINESS_FIELD_TOO_LONG
          },
          {
            "account/role",
            (Runnable)
                () ->
                    new CreateAccountDtoValidator()
                        .validate(
                            CreateAccountDto.builder(createValidAccount()).role("OWNER").build()),
            MessageKeys.BUSINESS_INVALID_SELECTION
          },
          {
            "reference/id/0",
            (Runnable)
                () ->
                    new RecordReferenceDtoValidator()
                        .validate(RecordReferenceDto.builder().id(0).build()),
            MessageKeys.BUSINESS_INVALID_RECORD_ID
          },
          {
            "assignment/id/0",
            (Runnable)
                () ->
                    new AssignCaseDtoValidator()
                        .validate(AssignCaseDto.builder().id(0).assignee("worker").build()),
            MessageKeys.BUSINESS_INVALID_RECORD_ID
          },
          {
            "transition/id/0",
            (Runnable)
                () ->
                    new TransitionCaseDtoValidator()
                        .validate(TransitionCaseDto.builder().id(0).destination("NEW").build()),
            MessageKeys.BUSINESS_INVALID_RECORD_ID
          },
          {
            "comment/id/0",
            (Runnable)
                () ->
                    new AddCaseCommentDtoValidator()
                        .validate(AddCaseCommentDto.builder().id(0).body("Comment").build()),
            MessageKeys.BUSINESS_INVALID_RECORD_ID
          },
          {
            "role/id/0",
            (Runnable)
                () ->
                    new ChangeAccountRoleDtoValidator()
                        .validate(ChangeAccountRoleDto.builder().id(0).role("ADMIN").build()),
            MessageKeys.BUSINESS_INVALID_RECORD_ID
          },
          {
            "reference/id/-1",
            (Runnable)
                () ->
                    new RecordReferenceDtoValidator()
                        .validate(RecordReferenceDto.builder().id(-1).build()),
            MessageKeys.BUSINESS_INVALID_RECORD_ID
          },
          {
            "assignment/id/-1",
            (Runnable)
                () ->
                    new AssignCaseDtoValidator()
                        .validate(AssignCaseDto.builder().id(-1).assignee("worker").build()),
            MessageKeys.BUSINESS_INVALID_RECORD_ID
          },
          {
            "transition/id/-1",
            (Runnable)
                () ->
                    new TransitionCaseDtoValidator()
                        .validate(TransitionCaseDto.builder().id(-1).destination("NEW").build()),
            MessageKeys.BUSINESS_INVALID_RECORD_ID
          },
          {
            "comment/id/-1",
            (Runnable)
                () ->
                    new AddCaseCommentDtoValidator()
                        .validate(AddCaseCommentDto.builder().id(-1).body("Comment").build()),
            MessageKeys.BUSINESS_INVALID_RECORD_ID
          },
          {
            "role/id/-1",
            (Runnable)
                () ->
                    new ChangeAccountRoleDtoValidator()
                        .validate(ChangeAccountRoleDto.builder().id(-1).role("ADMIN").build()),
            MessageKeys.BUSINESS_INVALID_RECORD_ID
          },
          {
            "assignment/assignee/null",
            (Runnable)
                () ->
                    new AssignCaseDtoValidator()
                        .validate(AssignCaseDto.builder().id(7).assignee(null).build()),
            MessageKeys.BUSINESS_FIELD_REQUIRED
          },
          {
            "assignment/assignee/\"\"",
            (Runnable)
                () ->
                    new AssignCaseDtoValidator()
                        .validate(AssignCaseDto.builder().id(7).assignee("").build()),
            MessageKeys.BUSINESS_FIELD_REQUIRED
          },
          {
            "assignment/assignee/maximum",
            (Runnable)
                () ->
                    new AssignCaseDtoValidator()
                        .validate(
                            AssignCaseDto.builder()
                                .id(7)
                                .assignee(repeat(ValidationLimits.USERNAME + 1))
                                .build()),
            MessageKeys.BUSINESS_FIELD_TOO_LONG
          },
          {
            "transition/destination/null",
            (Runnable)
                () ->
                    new TransitionCaseDtoValidator()
                        .validate(TransitionCaseDto.builder().id(7).destination(null).build()),
            MessageKeys.BUSINESS_INVALID_SELECTION
          },
          {
            "role/code/null",
            (Runnable)
                () ->
                    new ChangeAccountRoleDtoValidator()
                        .validate(ChangeAccountRoleDto.builder().id(7).role(null).build()),
            MessageKeys.BUSINESS_INVALID_SELECTION
          },
          {
            "transition/destination/\"UNKNOWN\"",
            (Runnable)
                () ->
                    new TransitionCaseDtoValidator()
                        .validate(TransitionCaseDto.builder().id(7).destination("UNKNOWN").build()),
            MessageKeys.BUSINESS_INVALID_SELECTION
          },
          {
            "role/code/\"UNKNOWN\"",
            (Runnable)
                () ->
                    new ChangeAccountRoleDtoValidator()
                        .validate(ChangeAccountRoleDto.builder().id(7).role("UNKNOWN").build()),
            MessageKeys.BUSINESS_INVALID_SELECTION
          },
          {
            "comment/body/BUSINESS_FIELD_REQUIRED",
            (Runnable)
                () ->
                    new AddCaseCommentDtoValidator()
                        .validate(AddCaseCommentDto.builder().id(7).body(" ").build()),
            MessageKeys.BUSINESS_FIELD_REQUIRED
          },
          {
            "comment/body/BUSINESS_FIELD_TOO_LONG",
            (Runnable)
                () ->
                    new AddCaseCommentDtoValidator()
                        .validate(
                            AddCaseCommentDto.builder()
                                .id(7)
                                .body(repeat(ValidationLimits.COMMENT + 1))
                                .build()),
            MessageKeys.BUSINESS_FIELD_TOO_LONG
          },
          {
            "page/range/-1",
            (Runnable)
                () ->
                    new PageRequestDtoValidator()
                        .validate(PageRequestDto.builder().page(-1).build()),
            MessageKeys.VALIDATION_PAGE
          },
          {
            "search/page/-1",
            (Runnable)
                () ->
                    new CaseSearchCriteriaDtoValidator()
                        .validate(CaseSearchCriteriaDto.builder().page(-1).build()),
            MessageKeys.VALIDATION_PAGE
          },
          {
            "page/range/ValidationLimits.MAX_PAGE + 1",
            (Runnable)
                () ->
                    new PageRequestDtoValidator()
                        .validate(
                            PageRequestDto.builder().page(ValidationLimits.MAX_PAGE + 1).build()),
            MessageKeys.VALIDATION_PAGE
          },
          {
            "search/page/ValidationLimits.MAX_PAGE + 1",
            (Runnable)
                () ->
                    new CaseSearchCriteriaDtoValidator()
                        .validate(
                            CaseSearchCriteriaDto.builder()
                                .page(ValidationLimits.MAX_PAGE + 1)
                                .build()),
            MessageKeys.VALIDATION_PAGE
          },
          {
            "search/query/maximum",
            (Runnable)
                () ->
                    new CaseSearchCriteriaDtoValidator()
                        .validate(
                            CaseSearchCriteriaDto.builder()
                                .query(repeat(ValidationLimits.SEARCH + 1))
                                .build()),
            MessageKeys.BUSINESS_FIELD_TOO_LONG
          },
          {
            "search/assignee/maximum",
            (Runnable)
                () ->
                    new CaseSearchCriteriaDtoValidator()
                        .validate(
                            CaseSearchCriteriaDto.builder()
                                .assignee(repeat(ValidationLimits.USERNAME + 1))
                                .build()),
            MessageKeys.BUSINESS_FIELD_TOO_LONG
          },
          {
            "search/status",
            (Runnable)
                () ->
                    new CaseSearchCriteriaDtoValidator()
                        .validate(CaseSearchCriteriaDto.builder().status("UNKNOWN").build()),
            MessageKeys.BUSINESS_INVALID_SELECTION
          },
          {
            "login/username/null",
            (Runnable)
                () ->
                    new AuthDtoValidator()
                        .validate(AuthDto.builder().username(null).password("legacy").build()),
            MessageKeys.BUSINESS_FIELD_REQUIRED
          },
          {
            "login/username/\" \"",
            (Runnable)
                () ->
                    new AuthDtoValidator()
                        .validate(AuthDto.builder().username(" ").password("legacy").build()),
            MessageKeys.BUSINESS_FIELD_REQUIRED
          },
          {
            "login/password/null",
            (Runnable)
                () ->
                    new AuthDtoValidator()
                        .validate(AuthDto.builder().username("worker").password(null).build()),
            MessageKeys.BUSINESS_FIELD_REQUIRED
          },
          {
            "login/password/\"\"",
            (Runnable)
                () ->
                    new AuthDtoValidator()
                        .validate(AuthDto.builder().username("worker").password("").build()),
            MessageKeys.BUSINESS_FIELD_REQUIRED
          },
          {
            "login/username/maximum",
            (Runnable)
                () ->
                    new AuthDtoValidator()
                        .validate(
                            AuthDto.builder()
                                .username(repeat(ValidationLimits.USERNAME + 1))
                                .password("legacy")
                                .build()),
            MessageKeys.BUSINESS_FIELD_TOO_LONG
          },
          {
            "document/payload/null",
            (Runnable)
                () ->
                    new DocumentDtoValidator()
                        .validate(DocumentDto.builder(createValidDocument()).payload(null).build()),
            MessageKeys.BUSINESS_FILE_MUST_BE_1_BYTE_TO_2_MB
          },
          {
            "document/payload/new byte[0]",
            (Runnable)
                () ->
                    new DocumentDtoValidator()
                        .validate(
                            DocumentDto.builder(createValidDocument())
                                .payload(new byte[0])
                                .build()),
            MessageKeys.BUSINESS_FILE_MUST_BE_1_BYTE_TO_2_MB
          },
          {
            "document/payload/new byte[ValidationLimits.ATTACHMENT_BYTES + 1]",
            (Runnable)
                () ->
                    new DocumentDtoValidator()
                        .validate(
                            DocumentDto.builder(createValidDocument())
                                .payload(new byte[ValidationLimits.ATTACHMENT_BYTES + 1])
                                .build()),
            MessageKeys.BUSINESS_FILE_MUST_BE_1_BYTE_TO_2_MB
          },
          {
            "document/type",
            (Runnable)
                () ->
                    new DocumentDtoValidator()
                        .validate(
                            DocumentDto.builder(createValidDocument())
                                .contentType("text/html")
                                .build()),
            MessageKeys.BUSINESS_UNSUPPORTED_FILE_TYPE
          },
          {
            "upload/case",
            (Runnable)
                () ->
                    new UploadDocumentDtoValidator()
                        .validate(
                            UploadDocumentDto.builder()
                                .caseId(0)
                                .declaredSize(3)
                                .document(createValidDocument())
                                .build()),
            MessageKeys.BUSINESS_INVALID_RECORD_ID
          },
          {
            "upload/declared-size/0",
            (Runnable)
                () ->
                    new UploadDocumentDtoValidator()
                        .validate(
                            UploadDocumentDto.builder()
                                .caseId(7)
                                .declaredSize(0)
                                .document(createValidDocument())
                                .build()),
            MessageKeys.BUSINESS_FILE_MUST_BE_1_BYTE_TO_2_MB
          },
          {
            "upload/declared-size/-1",
            (Runnable)
                () ->
                    new UploadDocumentDtoValidator()
                        .validate(
                            UploadDocumentDto.builder()
                                .caseId(7)
                                .declaredSize(-1)
                                .document(createValidDocument())
                                .build()),
            MessageKeys.BUSINESS_FILE_MUST_BE_1_BYTE_TO_2_MB
          },
          {
            "upload/declared-size/ValidationLimits.ATTACHMENT_BYTES + 1",
            (Runnable)
                () ->
                    new UploadDocumentDtoValidator()
                        .validate(
                            UploadDocumentDto.builder()
                                .caseId(7)
                                .declaredSize(ValidationLimits.ATTACHMENT_BYTES + 1)
                                .document(createValidDocument())
                                .build()),
            MessageKeys.BUSINESS_FILE_MUST_BE_1_BYTE_TO_2_MB
          },
          {
            "upload/missing-document",
            (Runnable)
                () ->
                    new UploadDocumentDtoValidator()
                        .validate(UploadDocumentDto.builder().caseId(7).declaredSize(3).build()),
            MessageKeys.VALIDATION_FILE_REQUIRED
          },
          {
            "upload/nested-content",
            (Runnable)
                () ->
                    new UploadDocumentDtoValidator()
                        .validate(
                            UploadDocumentDto.builder()
                                .caseId(7)
                                .declaredSize(3)
                                .document(
                                    DocumentDto.builder(createValidDocument())
                                        .payload(null)
                                        .build())
                                .build()),
            MessageKeys.BUSINESS_FILE_MUST_BE_1_BYTE_TO_2_MB
          },
          {
            "audit/id",
            (Runnable)
                () ->
                    new AuditEventDtoValidator()
                        .validate(AuditEventDto.builder(createValidAuditEvent()).id(-1L).build()),
            MessageKeys.BUSINESS_INVALID_RECORD_ID
          },
          {
            "audit/entityId",
            (Runnable)
                () ->
                    new AuditEventDtoValidator()
                        .validate(
                            AuditEventDto.builder(createValidAuditEvent()).entityId(-1L).build()),
            MessageKeys.BUSINESS_INVALID_RECORD_ID
          },
          {
            "audit/entityType",
            (Runnable)
                () ->
                    new AuditEventDtoValidator()
                        .validate(
                            AuditEventDto.builder(createValidAuditEvent())
                                .entityType("UNKNOWN")
                                .build()),
            MessageKeys.BUSINESS_INVALID_SELECTION
          },
          {
            "audit/actionName",
            (Runnable)
                () ->
                    new AuditEventDtoValidator()
                        .validate(
                            AuditEventDto.builder(createValidAuditEvent())
                                .actionName("UNKNOWN")
                                .build()),
            MessageKeys.BUSINESS_INVALID_SELECTION
          },
          {
            "audit/actor/required",
            (Runnable)
                () ->
                    new AuditEventDtoValidator()
                        .validate(
                            AuditEventDto.builder(createValidAuditEvent()).actor(" ").build()),
            MessageKeys.BUSINESS_FIELD_REQUIRED
          },
          {
            "audit/actor/maximum",
            (Runnable)
                () ->
                    new AuditEventDtoValidator()
                        .validate(
                            AuditEventDto.builder(createValidAuditEvent())
                                .actor(repeat(ValidationLimits.USERNAME + 1))
                                .build()),
            MessageKeys.BUSINESS_FIELD_TOO_LONG
          },
          {
            "audit/details/maximum",
            (Runnable)
                () ->
                    new AuditEventDtoValidator()
                        .validate(
                            AuditEventDto.builder(createValidAuditEvent())
                                .details(repeat(ValidationLimits.DETAILS + 1))
                                .build()),
            MessageKeys.BUSINESS_FIELD_TOO_LONG
          },
          {
            "notice/id",
            (Runnable)
                () ->
                    new NoticeDtoValidator()
                        .validate(NoticeDto.builder(createValidNotice()).id(-1L).build()),
            MessageKeys.BUSINESS_INVALID_RECORD_ID
          },
          {
            "notice/caseId",
            (Runnable)
                () ->
                    new NoticeDtoValidator()
                        .validate(NoticeDto.builder(createValidNotice()).caseId(-1L).build()),
            MessageKeys.BUSINESS_INVALID_RECORD_ID
          },
          {
            "notice/recipient/required",
            (Runnable)
                () ->
                    new NoticeDtoValidator()
                        .validate(NoticeDto.builder(createValidNotice()).recipient(" ").build()),
            MessageKeys.BUSINESS_FIELD_REQUIRED
          },
          {
            "notice/recipient/maximum",
            (Runnable)
                () ->
                    new NoticeDtoValidator()
                        .validate(
                            NoticeDto.builder(createValidNotice())
                                .recipient(repeat(ValidationLimits.USERNAME + 1))
                                .build()),
            MessageKeys.BUSINESS_FIELD_TOO_LONG
          },
          {
            "notice/subject/maximum",
            (Runnable)
                () ->
                    new NoticeDtoValidator()
                        .validate(
                            NoticeDto.builder(createValidNotice())
                                .subject(repeat(ValidationLimits.SUBJECT + 1))
                                .build()),
            MessageKeys.BUSINESS_FIELD_TOO_LONG
          },
          {
            "notice/message/maximum",
            (Runnable)
                () ->
                    new NoticeDtoValidator()
                        .validate(
                            NoticeDto.builder(createValidNotice())
                                .message(repeat(ValidationLimits.DETAILS + 1))
                                .build()),
            MessageKeys.BUSINESS_FIELD_TOO_LONG
          }
        });
  }

  /**
   * Rejects the input with a stable application message rather than a persistence failure.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void rejectsInvalidFieldWithExpectedMessage() {
    BusinessValidationException failure =
        assertThrows(BusinessValidationException.class, operation::run);
    assertEquals(expectedKey, failure.getMessageKey());
  }

  /**
   * Creates text just beyond the selected contract limit.
   *
   * @param length the requested number of characters
   * @return the generated test text
   */
  private static String repeat(int length) {
    char[] characters = new char[length];
    Arrays.fill(characters, 'x');
    return new String(characters);
  }
}
