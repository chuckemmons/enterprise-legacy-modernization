package com.example.legacy.service.validation;

import com.example.legacy.domain.Priority;
import com.example.legacy.service.dto.UpdateCaseDto;
import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.util.MessageKeys;
import com.example.legacy.util.ServiceValidationUtil;
import com.example.legacy.util.StringsUtil;
import com.example.legacy.util.ValidationLimits;
import java.util.Objects;

/**
 * Validates editable case input; version comparison belongs to the service and the due date is
 * optional.
 *
 * @author Chuck Emmons
 */
public final class UpdateCaseDtoValidator implements DtoValidator<UpdateCaseDto> {
  /**
   * Checks all input fields and returns a normalized immutable snapshot.
   *
   * @param dto the submitted {@code UpdateCaseDto}; it is never modified
   * @return the validated input snapshot
   * @throws NullPointerException if the DTO is {@code null}
   * @throws BusinessValidationException if an input field violates its contract
   */
  @Override
  public UpdateCaseDto validate(UpdateCaseDto dto) {
    Objects.requireNonNull(dto, "dto");
    return UpdateCaseDto.builder()
        .id(ServiceValidationUtil.requireRecordId(dto.getId()))
        .version(DtoValidation.requireVersion(dto.getVersion()))
        .title(
            ServiceValidationUtil.requireText(
                dto.getTitle(), ValidationLimits.TITLE, MessageKeys.FIELD_TITLE))
        .description(
            ServiceValidationUtil.requireText(
                dto.getDescription(), ValidationLimits.DESCRIPTION, MessageKeys.FIELD_DESCRIPTION))
        .priority(StringsUtil.requireAllowedChoice(dto.getPriority(), Priority.getCodes()))
        .dueAt(dto.getDueAt())
        .build();
  }
}
