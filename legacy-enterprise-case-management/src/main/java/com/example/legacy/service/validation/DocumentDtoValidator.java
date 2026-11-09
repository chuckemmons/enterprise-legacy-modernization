package com.example.legacy.service.validation;

import com.example.legacy.service.dto.DocumentDto;
import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.util.ContentTypes;
import com.example.legacy.util.MessageKeys;
import com.example.legacy.util.StringsUtil;
import java.util.Objects;

/**
 * Validates upload content and whitelists client metadata; identifiers, author, and timestamps are
 * server-owned.
 *
 * @author Chuck Emmons
 */
public final class DocumentDtoValidator implements DtoValidator<DocumentDto> {
  /**
   * Checks all input fields and returns a normalized immutable snapshot.
   *
   * @param dto the submitted {@code DocumentDto}; it is never modified
   * @return the validated input snapshot
   * @throws NullPointerException if the DTO is {@code null}
   * @throws BusinessValidationException if an input field violates its contract
   */
  @Override
  public DocumentDto validate(DocumentDto dto) {
    Objects.requireNonNull(dto, "dto");
    byte[] payload = dto.getPayload();
    DtoValidation.requireAttachmentSize(payload == null ? 0 : payload.length);
    String type = dto.getContentType() == null ? ContentTypes.BINARY : dto.getContentType();
    if (!ContentTypes.isSupportedAttachment(type)) {
      throw new BusinessValidationException(MessageKeys.BUSINESS_UNSUPPORTED_FILE_TYPE);
    }
    return DocumentDto.builder()
        .originalName(StringsUtil.sanitizeFilename(dto.getOriginalName()))
        .contentType(type)
        .contentSize(payload.length)
        .payload(payload)
        .build();
  }
}
