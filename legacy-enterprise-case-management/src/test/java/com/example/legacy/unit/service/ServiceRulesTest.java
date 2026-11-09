package com.example.legacy.unit.service;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.example.legacy.model.Account;
import com.example.legacy.model.ServiceCase;
import com.example.legacy.service.BootstrapService;
import com.example.legacy.service.CasePolicyService;
import com.example.legacy.service.DocumentService;
import org.junit.Test;

/**
 * Exercises business rules independently of HTTP adapters and database connections.
 *
 * @author Chuck Emmons
 */
public class ServiceRulesTest {

  /**
   * Verifies ownership, role, closure, assignment, and self-approval rules used by service
   * mutations and screens.
   *
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void enforcesCasePermissions() {
    final CasePolicyService casePolicyService = new CasePolicyService();

    final Account worker = createAccount("worker", "WORKER");
    final Account supervisor = createAccount("supervisor", "SUPERVISOR");
    final Account admin = createAccount("admin", "ADMIN");
    final Account auditor = createAccount("auditor", "AUDITOR");
    ServiceCase item = new ServiceCase();
    item.setStatus("TRIAGED");
    item.setCreatedBy("creator");
    item.setAssignedTo("worker");
    casePolicyService.requireCaseEditPermission(worker, item);
    casePolicyService.requireCaseTransitionPermission(worker, item, "IN_PROGRESS");
    assertTrue(
        "assigned worker starts work",
        casePolicyService.getAllowedNextStatuses(worker, item).contains("IN_PROGRESS"));
    assertTrue(
        "supervisor edits another user's case", casePolicyService.canEditCase(supervisor, item));
    assertTrue("admin edits another user's case", casePolicyService.canEditCase(admin, item));
    assertTrue("auditor remains read-only", !casePolicyService.canEditCase(auditor, item));
    assertThrowsExpectedException(
        com.example.legacy.service.exception.AccessDeniedException.class,
        () -> casePolicyService.requireCaseEditPermission(auditor, item));
    assertThrowsExpectedException(
        com.example.legacy.service.exception.AccessDeniedException.class,
        () -> casePolicyService.requireCaseTransitionPermission(worker, item, "CLOSED"));

    item.setAssignedTo("another-worker");
    assertTrue("unrelated worker cannot edit", !casePolicyService.canEditCase(worker, item));
    assertTrue(
        "unassigned worker has no workflow choices",
        casePolicyService.getAllowedNextStatuses(worker, item).isEmpty());
    assertThrowsExpectedException(
        com.example.legacy.service.exception.AccessDeniedException.class,
        () -> casePolicyService.requireCaseEditPermission(worker, item));
    assertThrowsExpectedException(
        com.example.legacy.service.exception.AccessDeniedException.class,
        () -> casePolicyService.requireCaseTransitionPermission(worker, item, "IN_PROGRESS"));
    item.setCreatedBy("worker");
    assertTrue(
        "creator may edit while assigned elsewhere", casePolicyService.canEditCase(worker, item));
    assertTrue(
        "creator still needs assignment to transition",
        casePolicyService.getAllowedNextStatuses(worker, item).isEmpty());

    item.setStatus("PENDING_APPROVAL");
    item.setCreatedBy("supervisor");
    assertTrue(
        "self-approval hidden",
        !casePolicyService.getAllowedNextStatuses(supervisor, item).contains("APPROVED"));
    assertTrue(
        "creator may reject",
        casePolicyService.getAllowedNextStatuses(supervisor, item).contains("REJECTED"));
    assertThrowsExpectedException(
        com.example.legacy.service.exception.BusinessValidationException.class,
        () -> casePolicyService.requireCaseTransitionPermission(supervisor, item, "APPROVED"));
    casePolicyService.requireCaseTransitionPermission(admin, item, "APPROVED");
    item.setCreatedBy("admin");
    assertThrowsExpectedException(
        com.example.legacy.service.exception.BusinessValidationException.class,
        () -> casePolicyService.requireCaseTransitionPermission(admin, item, "APPROVED"));

    item.setStatus("CLOSED");
    assertTrue(
        "closed cases cannot be edited by administrators",
        !casePolicyService.canEditCase(admin, item));
    assertThrowsExpectedException(
        com.example.legacy.service.exception.BusinessValidationException.class,
        () -> casePolicyService.requireCaseEditPermission(admin, item));
    assertTrue(
        "closed cases are terminal",
        casePolicyService.getAllowedNextStatuses(admin, item).isEmpty());
  }

  /**
   * Checks attachment limits and media-type validation at service entry points before persistence
   * is invoked.
   *
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws com.example.legacy.service.exception.RecordNotFoundException if the requested case does
   *     not exist or is archived
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void validatesAttachmentLimitsAndTypes() {
    final DocumentService documentService = new DocumentService();
    com.example.legacy.service.validation.DocumentDtoValidator documentValidator =
        new com.example.legacy.service.validation.DocumentDtoValidator();

    for (String type :
        new String[] {
          null,
          "application/pdf",
          "text/plain",
          "image/png",
          "image/jpeg",
          "application/octet-stream"
        }) {
      documentValidator.validate(
          com.example.legacy.service.dto.DocumentDto.builder()
              .contentType(type)
              .payload(new byte[] {1})
              .build());
    }
    documentValidator.validate(
        com.example.legacy.service.dto.DocumentDto.builder()
            .contentType("text/plain")
            .payload(new byte[com.example.legacy.util.ValidationLimits.ATTACHMENT_BYTES])
            .build());
    assertThrowsExpectedException(
        com.example.legacy.service.exception.BusinessValidationException.class,
        () ->
            documentValidator.validate(
                com.example.legacy.service.dto.DocumentDto.builder().payload(new byte[0]).build()));
    assertThrowsExpectedException(
        com.example.legacy.service.exception.BusinessValidationException.class,
        () ->
            documentValidator.validate(
                com.example.legacy.service.dto.DocumentDto.builder()
                    .payload(
                        new byte[com.example.legacy.util.ValidationLimits.ATTACHMENT_BYTES + 1])
                    .build()));
    assertThrowsExpectedException(
        com.example.legacy.service.exception.BusinessValidationException.class,
        () ->
            documentValidator.validate(
                com.example.legacy.service.dto.DocumentDto.builder()
                    .contentType("text/html")
                    .payload(new byte[] {1})
                    .build()));
    assertThrowsExpectedException(
        com.example.legacy.service.exception.BusinessValidationException.class,
        () ->
            documentService.uploadDocument(
                "worker",
                com.example.legacy.service.dto.UploadDocumentDto.builder()
                    .caseId(1)
                    .declaredSize(1)
                    .document(
                        com.example.legacy.service.dto.DocumentDto.builder()
                            .originalName("empty.txt")
                            .contentType("text/plain")
                            .build())
                    .build()));
    assertThrowsExpectedException(
        com.example.legacy.service.exception.BusinessValidationException.class,
        () ->
            documentService.uploadDocument(
                "worker",
                com.example.legacy.service.dto.UploadDocumentDto.builder()
                    .caseId(1)
                    .declaredSize(1)
                    .document(
                        com.example.legacy.service.dto.DocumentDto.builder()
                            .originalName("bad.html")
                            .contentType("text/html")
                            .payload(new byte[] {1})
                            .build())
                    .build()));
  }

  /**
   * Checks business role guards outside the servlet layer.
   *
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws com.example.legacy.service.exception.RecordNotFoundException if the requested case does
   *     not exist or is archived
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws IllegalStateException if a required runtime service cannot complete the operation
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void enforcesRolePermissions() {
    com.example.legacy.service.AccountAccessService caseService =
        new com.example.legacy.service.AccountAccessService();
    final BootstrapService bootstrapService = new BootstrapService();

    for (String role : new String[] {"ADMIN", "SUPERVISOR", "AUDITOR"}) {
      caseService.requireReportingPermission(createAccount("user", role));
    }
    assertThrowsExpectedException(
        com.example.legacy.service.exception.AccessDeniedException.class,
        () -> caseService.requireReportingPermission(createAccount("user", "WORKER")));
    assertThrowsExpectedException(
        com.example.legacy.service.exception.AccessDeniedException.class,
        () -> caseService.requireReportingPermission(createAccount("user", null)));
    caseService.requireAdministratorPermission(createAccount("user", "ADMIN"));
    assertThrowsExpectedException(
        com.example.legacy.service.exception.AccessDeniedException.class,
        () -> caseService.requireAdministratorPermission(createAccount("user", "SUPERVISOR")));
    assertThrowsExpectedException(
        com.example.legacy.service.exception.AccessDeniedException.class,
        () -> caseService.requireCaseWorkPermission(createAccount("user", "AUDITOR")));
    assertThrowsExpectedException(
        com.example.legacy.service.exception.BusinessValidationException.class,
        () -> bootstrapService.seedAdmin("INVALID USER", "SomePassword!2026"));
    assertThrowsExpectedException(
        com.example.legacy.service.exception.BusinessValidationException.class,
        () -> bootstrapService.seedDemo("short"));
  }

  /**
   * Uses JUnit's exception assertion for expected business-rule failures.
   *
   * @param expected the expected exception type
   * @param operation the operation expected to fail
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  private static void assertThrowsExpectedException(
      Class<? extends RuntimeException> expected, Runnable operation) {
    assertThrows(expected, operation::run);
  }

  /**
   * Creates a policy-test account without persistence or servlet state.
   *
   * @param username the username identifying the account
   * @param role the account role code
   * @return the account
   */
  private static Account createAccount(String username, String role) {
    Account account = new Account();
    account.setUsername(username);
    account.setRole(role);
    account.setEnabled(true);
    return account;
  }
}
