package com.example.legacy.unit.service.validation;

import static com.example.legacy.unit.service.validation.DtoValidationFixtures.createValidAccount;
import static com.example.legacy.unit.service.validation.DtoValidationFixtures.createValidAuditEvent;
import static com.example.legacy.unit.service.validation.DtoValidationFixtures.createValidCase;
import static com.example.legacy.unit.service.validation.DtoValidationFixtures.createValidDocument;
import static com.example.legacy.unit.service.validation.DtoValidationFixtures.createValidNotice;
import static com.example.legacy.unit.service.validation.DtoValidationFixtures.createValidUpdate;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.example.legacy.service.AuditService;
import com.example.legacy.service.AuthenticationService;
import com.example.legacy.service.BackOfficeService;
import com.example.legacy.service.DefaultCaseService;
import com.example.legacy.service.DocumentService;
import com.example.legacy.service.NotificationService;
import com.example.legacy.service.dto.AccountDto;
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
import com.example.legacy.service.validation.DtoValidator;
import com.example.legacy.service.validation.NoticeDtoValidator;
import com.example.legacy.service.validation.PageRequestDtoValidator;
import com.example.legacy.service.validation.RecordReferenceDtoValidator;
import com.example.legacy.service.validation.TransitionCaseDtoValidator;
import com.example.legacy.service.validation.UpdateCaseDtoValidator;
import com.example.legacy.service.validation.UploadDocumentDtoValidator;
import com.example.legacy.util.ContentTypes;
import com.example.legacy.util.ValidationLimits;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.hibernate.SessionFactory;
import org.junit.Test;

/**
 * Verifies immutable normalization and validation boundaries independently of persistence.
 *
 * @author Chuck Emmons
 */
public class DtoValidatorTest {
  /**
   * Rejects absent DTOs consistently, including nested-validator entry points.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void rejectsNullDtos() {
    List<DtoValidator<?>> validators =
        Arrays.<DtoValidator<?>>asList(
            new CreateCaseDtoValidator(),
            new UpdateCaseDtoValidator(),
            new AssignCaseDtoValidator(),
            new TransitionCaseDtoValidator(),
            new AddCaseCommentDtoValidator(),
            new RecordReferenceDtoValidator(),
            new PageRequestDtoValidator(),
            new CaseSearchCriteriaDtoValidator(),
            new CreateAccountDtoValidator(),
            new ChangeAccountRoleDtoValidator(),
            new AuthDtoValidator(),
            new DocumentDtoValidator(),
            new UploadDocumentDtoValidator(),
            new AuditEventDtoValidator(),
            new NoticeDtoValidator());
    for (DtoValidator<?> validator : validators) {
      assertThrows(NullPointerException.class, () -> validator.validate(null));
    }
  }

  /**
   * Normalizes all case text without changing the request or imposing a new due-date policy.
   *
   * @throws BusinessValidationException if a required field is missing, malformed, or outside its
   *     supported range
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void normalizesCaseInputWithoutMutatingTheRequest() {
    Date dueAt = new Date(1000);
    CreateCaseDto input =
        CreateCaseDto.builder(createValidCase())
            .title("  Case\u0001 title  ")
            .description(" Description ")
            .category(" General ")
            .department(" Operations ")
            .dueAt(dueAt)
            .build();
    CreateCaseDto result = new CreateCaseDtoValidator().validate(input);
    assertEquals("Case title", result.getTitle());
    assertEquals("Description", result.getDescription());
    assertEquals("General", result.getCategory());
    assertEquals("Operations", result.getDepartment());
    assertEquals("MEDIUM", result.getPriority());
    assertEquals(dueAt, result.getDueAt());
    assertEquals("  Case\u0001 title  ", input.getTitle());
    assertNotSame(input, result);
    assertNull(new CreateCaseDtoValidator().validate(createValidCase()).getDueAt());
  }

  /**
   * Checks version syntax without comparing its value with any persisted record.
   *
   * @throws BusinessValidationException if a required field is missing, malformed, or outside its
   *     supported range
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void separatesVersionShapeFromDatabaseVersionComparison() {
    UpdateCaseDto result =
        new UpdateCaseDtoValidator()
            .validate(
                UpdateCaseDto.builder(createValidUpdate()).version(5).title(" Updated ").build());
    assertEquals(7L, result.getId());
    assertEquals(Integer.valueOf(5), result.getVersion());
    assertEquals("Updated", result.getTitle());
    assertNull(result.getDueAt());
  }

  /**
   * Normalizes account metadata while preserving every character of the new password.
   *
   * @throws BusinessValidationException if a required field is missing, malformed, or outside its
   *     supported range
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void normalizesAccountFieldsWithoutChangingItsPassword() {
    String password = "  OriginalPassword!2026  ";
    CreateAccountDto input =
        CreateAccountDto.builder(createValidAccount())
            .username(" WORKER1 ")
            .displayName(" Worker ")
            .department(" Operations ")
            .email(" worker@example.org ")
            .password(password)
            .build();
    CreateAccountDto result = new CreateAccountDtoValidator().validate(input);
    assertEquals("worker1", result.getUsername());
    assertEquals("Worker", result.getDisplayName());
    assertEquals("Operations", result.getDepartment());
    assertEquals("worker@example.org", result.getEmail());
    assertEquals(password, result.getPassword());
    assertEquals(" WORKER1 ", input.getUsername());
    assertEquals("", new CreateAccountDtoValidator().validate(createValidAccount()).getEmail());
  }

  /**
   * Accepts legacy login passwords and discards a caller-supplied account response.
   *
   * @throws BusinessValidationException if a required field is missing, malformed, or outside its
   *     supported range
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void validatesLoginWithoutTrustingSubmittedIdentityOrApplyingNewPasswordRules() {
    AccountDto claimed = AccountDto.builder().username("admin").role("ADMIN").build();
    AuthDto input = AuthDto.builder().username(" WORKER ").password(" p ").account(claimed).build();
    AuthDto result = new AuthDtoValidator().validate(input);
    assertEquals("worker", result.getUsername());
    assertEquals(" p ", result.getPassword());
    assertNull(result.getAccount());
    assertSame(claimed, input.getAccount());
  }

  /**
   * Keeps optional filters unrestricted and validates both supported pagination endpoints.
   *
   * @throws BusinessValidationException if a required field is missing, malformed, or outside its
   *     supported range
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void normalizesSearchFiltersAndAcceptsPageBoundaries() {
    CaseSearchCriteriaDto input =
        CaseSearchCriteriaDto.builder()
            .query(" CaSe ")
            .status("")
            .assignee(" worker ")
            .page(ValidationLimits.MAX_PAGE)
            .build();
    CaseSearchCriteriaDto result = new CaseSearchCriteriaDtoValidator().validate(input);
    assertEquals("case", result.getQuery());
    assertEquals("", result.getStatus());
    assertEquals("worker", result.getAssignee());
    assertEquals(ValidationLimits.MAX_PAGE, result.getPage());
    assertEquals(" CaSe ", input.getQuery());
    assertEquals(
        "",
        new CaseSearchCriteriaDtoValidator()
            .validate(CaseSearchCriteriaDto.builder().build())
            .getQuery());
    assertEquals(
        0,
        new PageRequestDtoValidator().validate(PageRequestDto.builder().page(0).build()).getPage());
    assertEquals(
        ValidationLimits.MAX_PAGE,
        new PageRequestDtoValidator()
            .validate(PageRequestDto.builder().page(ValidationLimits.MAX_PAGE).build())
            .getPage());
  }

  /**
   * Sanitizes file metadata, derives the actual size, and excludes server-owned upload fields.
   *
   * @throws BusinessValidationException if a required field is missing, malformed, or outside its
   *     supported range
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void validatesNestedUploadContentWithoutTrustingServerOwnedMetadata() {
    DocumentDto document =
        DocumentDto.builder(createValidDocument())
            .id(99L)
            .caseId(88L)
            .uploadedBy("admin")
            .createdAt(new Date())
            .originalName("..\\notes.txt")
            .contentType(null)
            .contentSize(999)
            .build();
    UploadDocumentDto input =
        UploadDocumentDto.builder().caseId(7).declaredSize(3).document(document).build();
    UploadDocumentDto result = new UploadDocumentDtoValidator().validate(input);
    assertEquals(7L, result.getCaseId());
    assertEquals(3, result.getDeclaredSize());
    DocumentDto content = result.getDocument();
    assertEquals("notes.txt", content.getOriginalName());
    assertEquals(ContentTypes.BINARY, content.getContentType());
    assertEquals(3, content.getContentSize());
    assertArrayEquals(new byte[] {1, 2, 3}, content.getPayload());
    assertNull(content.getId());
    assertNull(content.getCaseId());
    assertNull(content.getUploadedBy());
    assertNull(content.getCreatedAt());
    assertEquals(999, input.getDocument().getContentSize());
    assertEquals(Long.valueOf(99), input.getDocument().getId());
  }

  /**
   * Accepts valid reference/status/role fields without treating database policy as DTO syntax.
   *
   * @throws BusinessValidationException if a required field is missing, malformed, or outside its
   *     supported range
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void acceptsStructurallyValidCommandsWithoutDatabaseState() {
    assertEquals(
        7L,
        new RecordReferenceDtoValidator()
            .validate(RecordReferenceDto.builder().id(7).build())
            .getId());
    assertEquals(
        "worker",
        new AssignCaseDtoValidator()
            .validate(AssignCaseDto.builder().id(7).assignee(" worker ").build())
            .getAssignee());
    assertEquals(
        "CLOSED",
        new TransitionCaseDtoValidator()
            .validate(TransitionCaseDto.builder().id(7).destination("CLOSED").build())
            .getDestination());
    assertEquals(
        "ADMIN",
        new ChangeAccountRoleDtoValidator()
            .validate(ChangeAccountRoleDto.builder().id(7).role("ADMIN").build())
            .getRole());
  }

  /**
   * Validates internal write DTOs while retaining typed notification lifecycle fields.
   *
   * @throws BusinessValidationException if a required field is missing, malformed, or outside its
   *     supported range
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void normalizesAuditAndNotificationWrites() {
    AuditEventDto audit =
        new AuditEventDtoValidator()
            .validate(
                AuditEventDto.builder(createValidAuditEvent())
                    .actor(" worker1 ")
                    .details(" Details ")
                    .entityId(null)
                    .build());
    assertEquals("worker1", audit.getActor());
    assertEquals("Details", audit.getDetails());
    assertNull(audit.getEntityId());
    Date sentAt = new Date(2000);
    NoticeDto notice =
        new NoticeDtoValidator()
            .validate(
                NoticeDto.builder(createValidNotice())
                    .recipient(" worker1 ")
                    .subject(" Subject ")
                    .message(" Message ")
                    .caseId(null)
                    .read(true)
                    .sentAt(sentAt)
                    .build());
    assertEquals("worker1", notice.getRecipient());
    assertEquals("Subject", notice.getSubject());
    assertEquals("Message", notice.getMessage());
    assertTrue(notice.isRead());
    assertEquals(sentAt, notice.getSentAt());
    assertNull(notice.getCaseId());
  }

  /**
   * Requires services to supply creation timestamps when preparing internal persistence writes.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void rejectsMissingServerGeneratedCreationTimes() {
    assertThrows(
        NullPointerException.class,
        () ->
            new AuditEventDtoValidator()
                .validate(AuditEventDto.builder(createValidAuditEvent()).createdAt(null).build()));
    assertThrows(
        NullPointerException.class,
        () ->
            new NoticeDtoValidator()
                .validate(NoticeDto.builder(createValidNotice()).createdAt(null).build()));
  }

  /**
   * Rejects malformed commands before services open a Hibernate session or contact persistence.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void rejectsInvalidDtosBeforeOpeningDatabaseSessions() {
    SessionFactory factory = mock(SessionFactory.class);
    assertThrows(
        BusinessValidationException.class,
        () -> new DefaultCaseService(factory).createCase("actor", CreateCaseDto.builder().build()));
    assertThrows(
        BusinessValidationException.class,
        () ->
            new DefaultCaseService(factory)
                .updateCase("actor", UpdateCaseDto.builder(createValidUpdate()).id(0).build()));
    assertThrows(
        BusinessValidationException.class,
        () ->
            new BackOfficeService(factory).createUser("actor", CreateAccountDto.builder().build()));
    assertThrows(
        BusinessValidationException.class,
        () ->
            new AuthenticationService(factory)
                .authenticate(AuthDto.builder().username("worker").build()));
    assertThrows(
        BusinessValidationException.class,
        () ->
            new DocumentService(factory)
                .uploadDocument(
                    "actor",
                    UploadDocumentDto.builder()
                        .caseId(7)
                        .declaredSize(0)
                        .document(createValidDocument())
                        .build()));
    assertThrows(
        BusinessValidationException.class,
        () ->
            new AuditService(factory)
                .listRecentAuditEvents("actor", PageRequestDto.builder().page(-1).build()));
    assertThrows(
        BusinessValidationException.class,
        () ->
            new NotificationService(factory)
                .markNotificationRead("actor", RecordReferenceDto.builder().id(0).build()));
    verifyNoInteractions(factory);
  }
}
