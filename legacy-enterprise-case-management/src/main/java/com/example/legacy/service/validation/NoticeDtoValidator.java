package com.example.legacy.service.validation;

import com.example.legacy.service.dto.NoticeDto;
import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.util.MessageKeys;
import com.example.legacy.util.ServiceValidationUtil;
import com.example.legacy.util.StringsUtil;
import com.example.legacy.util.ValidationLimits;
import java.util.Objects;

/**
 * Validates a complete notification write; sent time is optional and the read flag is already
 * typed.
 *
 * @author Chuck Emmons
 */
public final class NoticeDtoValidator implements DtoValidator<NoticeDto> {
  /**
   * Checks all input fields and returns a normalized immutable snapshot.
   *
   * @param dto the submitted {@code NoticeDto}; it is never modified
   * @return the validated input snapshot
   * @throws NullPointerException if the DTO or its creation timestamp is {@code null}
   * @throws BusinessValidationException if an input field violates its contract
   */
  @Override
  public NoticeDto validate(NoticeDto dto) {
    Objects.requireNonNull(dto, "dto");
    return NoticeDto.builder(dto)
        .id(DtoValidation.validateOptionalId(dto.getId()))
        .recipient(
            ServiceValidationUtil.requireText(
                dto.getRecipient(), ValidationLimits.USERNAME, MessageKeys.FIELD_USERNAME))
        .subject(StringsUtil.normalizeText(dto.getSubject(), ValidationLimits.SUBJECT))
        .message(StringsUtil.normalizeText(dto.getMessage(), ValidationLimits.DETAILS))
        .caseId(DtoValidation.validateOptionalId(dto.getCaseId()))
        .createdAt(Objects.requireNonNull(dto.getCreatedAt(), "createdAt"))
        .build();
  }
}
