package com.example.legacy.unit.service.validation;

import com.example.legacy.service.dto.AuditEventDto;
import com.example.legacy.service.dto.CreateAccountDto;
import com.example.legacy.service.dto.CreateCaseDto;
import com.example.legacy.service.dto.DocumentDto;
import com.example.legacy.service.dto.NoticeDto;
import com.example.legacy.service.dto.UpdateCaseDto;
import java.util.Date;

/**
 * Supplies valid commands so each validation test can isolate a single invalid field.
 *
 * @author Chuck Emmons
 */
final class DtoValidationFixtures {
  /** Prevents construction of this test fixture helper. */
  private DtoValidationFixtures() {}

  /**
   * Creates complete case-creation input with an optional date left unset.
   *
   * @return valid case-creation input
   */
  static CreateCaseDto createValidCase() {
    return CreateCaseDto.builder()
        .title("Case title")
        .description("Case description")
        .category("General")
        .priority("MEDIUM")
        .department("Operations")
        .build();
  }

  /**
   * Creates complete update input with a nonnegative submitted version.
   *
   * @return valid case-update input
   */
  static UpdateCaseDto createValidUpdate() {
    return UpdateCaseDto.builder()
        .id(7)
        .version(0)
        .title("Updated title")
        .description("Updated description")
        .priority("MEDIUM")
        .build();
  }

  /**
   * Creates complete account input with an optional email address left unset.
   *
   * @return valid account-creation input
   */
  static CreateAccountDto createValidAccount() {
    return CreateAccountDto.builder()
        .username("worker1")
        .password("TestPassword!2026")
        .displayName("Worker")
        .role("WORKER")
        .department("Operations")
        .build();
  }

  /**
   * Creates a nonempty attachment with a supported media type.
   *
   * @return valid upload content
   */
  static DocumentDto createValidDocument() {
    return DocumentDto.builder()
        .originalName("note.txt")
        .contentType("text/plain")
        .payload(new byte[] {1, 2, 3})
        .contentSize(3)
        .build();
  }

  /**
   * Creates a complete server-generated audit event.
   *
   * @return valid audit-write input
   */
  static AuditEventDto createValidAuditEvent() {
    return AuditEventDto.builder()
        .entityType("CASE")
        .entityId(7L)
        .actionName("CREATE")
        .actor("worker1")
        .details("Created case")
        .createdAt(new Date(1000))
        .build();
  }

  /**
   * Creates a complete server-generated notification.
   *
   * @return valid notification-write input
   */
  static NoticeDto createValidNotice() {
    return NoticeDto.builder()
        .recipient("worker1")
        .caseId(7L)
        .subject("Assigned")
        .message("A case was assigned")
        .createdAt(new Date(1000))
        .build();
  }
}
