package com.example.legacy.service.validation;

import com.example.legacy.domain.Priority;
import com.example.legacy.service.dto.CreateCaseDto;
import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.util.MessageKeys;
import com.example.legacy.util.ServiceValidationUtil;
import com.example.legacy.util.StringsUtil;
import com.example.legacy.util.ValidationLimits;
import java.util.Objects;

/**
 * Validates case-creation fields; the already-parsed due date remains optional.
 *
 * @author Chuck Emmons
 */
public final class CreateCaseDtoValidator implements DtoValidator<CreateCaseDto> {
  /**
   * Checks all input fields and returns a normalized immutable snapshot.
   *
   * @param dto the submitted {@code CreateCaseDto}; it is never modified
   * @return the validated input snapshot
   * @throws NullPointerException if the DTO is {@code null}
   * @throws BusinessValidationException if an input field violates its contract
   */
  @Override
  public CreateCaseDto validate(CreateCaseDto dto) {
    Objects.requireNonNull(dto, "dto");
    return CreateCaseDto.builder()
        .title(
            ServiceValidationUtil.requireText(
                dto.getTitle(), ValidationLimits.TITLE, MessageKeys.FIELD_TITLE))
        .description(
            ServiceValidationUtil.requireText(
                dto.getDescription(), ValidationLimits.DESCRIPTION, MessageKeys.FIELD_DESCRIPTION))
        .category(
            ServiceValidationUtil.requireText(
                dto.getCategory(), ValidationLimits.CATEGORY, MessageKeys.FIELD_CATEGORY))
        .priority(StringsUtil.requireAllowedChoice(dto.getPriority(), Priority.getCodes()))
        .department(
            ServiceValidationUtil.requireText(
                dto.getDepartment(), ValidationLimits.DEPARTMENT, MessageKeys.FIELD_DEPARTMENT))
        .dueAt(dto.getDueAt())
        .build();
  }
}
