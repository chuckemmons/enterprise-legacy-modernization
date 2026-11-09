package com.example.legacy.service.validation;

import com.example.legacy.service.dto.AssignCaseDto;
import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.util.MessageKeys;
import com.example.legacy.util.ServiceValidationUtil;
import com.example.legacy.util.ValidationLimits;
import java.util.Objects;

/**
 * Validates a case reference and assignee name without loading either record.
 *
 * @author Chuck Emmons
 */
public final class AssignCaseDtoValidator implements DtoValidator<AssignCaseDto> {
  /**
   * Checks all input fields and returns a normalized immutable snapshot.
   *
   * @param dto the submitted {@code AssignCaseDto}; it is never modified
   * @return the validated input snapshot
   * @throws NullPointerException if the DTO is {@code null}
   * @throws BusinessValidationException if an input field violates its contract
   */
  @Override
  public AssignCaseDto validate(AssignCaseDto dto) {
    Objects.requireNonNull(dto, "dto");
    return AssignCaseDto.builder()
        .id(ServiceValidationUtil.requireRecordId(dto.getId()))
        .assignee(
            ServiceValidationUtil.requireText(
                dto.getAssignee(), ValidationLimits.USERNAME, MessageKeys.FIELD_ASSIGNEE))
        .build();
  }
}
