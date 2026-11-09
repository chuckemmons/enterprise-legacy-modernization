package com.example.legacy.service.validation;

import com.example.legacy.service.dto.DocumentDto;
import com.example.legacy.service.dto.UploadDocumentDto;
import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.util.MessageKeys;
import com.example.legacy.util.ServiceValidationUtil;
import java.util.Objects;

/**
 * Validates the parent reference, declared size, and complete nested upload content.
 *
 * @author Chuck Emmons
 */
public final class UploadDocumentDtoValidator implements DtoValidator<UploadDocumentDto> {
  private final DocumentDtoValidator documentValidator = new DocumentDtoValidator();

  /**
   * Checks all input fields and returns a normalized immutable snapshot.
   *
   * @param dto the submitted {@code UploadDocumentDto}; it is never modified
   * @return the validated input snapshot
   * @throws NullPointerException if the DTO is {@code null}
   * @throws BusinessValidationException if an input field violates its contract
   */
  @Override
  public UploadDocumentDto validate(UploadDocumentDto dto) {
    Objects.requireNonNull(dto, "dto");
    long caseId = ServiceValidationUtil.requireRecordId(dto.getCaseId());
    DtoValidation.requireAttachmentSize(dto.getDeclaredSize());
    if (dto.getDocument() == null) {
      throw new BusinessValidationException(MessageKeys.VALIDATION_FILE_REQUIRED);
    }
    DocumentDto document = documentValidator.validate(dto.getDocument());
    return UploadDocumentDto.builder()
        .caseId(caseId)
        .declaredSize(dto.getDeclaredSize())
        .document(document)
        .build();
  }
}
