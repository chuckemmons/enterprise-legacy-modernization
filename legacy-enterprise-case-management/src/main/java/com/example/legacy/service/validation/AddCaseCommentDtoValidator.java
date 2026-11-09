package com.example.legacy.service.validation;

import com.example.legacy.service.dto.AddCaseCommentDto;
import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.util.MessageKeys;
import com.example.legacy.util.ServiceValidationUtil;
import com.example.legacy.util.ValidationLimits;
import java.util.Objects;

/**
 * Validates the case identifier and normalizes the required comment text.
 *
 * @author Chuck Emmons
 */
public final class AddCaseCommentDtoValidator implements DtoValidator<AddCaseCommentDto> {
  /**
   * Checks all input fields and returns a normalized immutable snapshot.
   *
   * @param dto the submitted {@code AddCaseCommentDto}; it is never modified
   * @return the validated input snapshot
   * @throws NullPointerException if the DTO is {@code null}
   * @throws BusinessValidationException if an input field violates its contract
   */
  @Override
  public AddCaseCommentDto validate(AddCaseCommentDto dto) {
    Objects.requireNonNull(dto, "dto");
    return AddCaseCommentDto.builder()
        .id(ServiceValidationUtil.requireRecordId(dto.getId()))
        .body(
            ServiceValidationUtil.requireText(
                dto.getBody(), ValidationLimits.COMMENT, MessageKeys.FIELD_COMMENT))
        .build();
  }
}
