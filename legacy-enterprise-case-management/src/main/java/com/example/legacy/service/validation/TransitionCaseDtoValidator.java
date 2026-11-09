package com.example.legacy.service.validation;

import com.example.legacy.domain.CaseStatus;
import com.example.legacy.service.dto.TransitionCaseDto;
import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.util.ServiceValidationUtil;
import com.example.legacy.util.StringsUtil;
import java.util.Objects;

/**
 * Validates a destination status code; allowed transitions and permissions remain policy checks.
 *
 * @author Chuck Emmons
 */
public final class TransitionCaseDtoValidator implements DtoValidator<TransitionCaseDto> {
  /**
   * Checks all input fields and returns a normalized immutable snapshot.
   *
   * @param dto the submitted {@code TransitionCaseDto}; it is never modified
   * @return the validated input snapshot
   * @throws NullPointerException if the DTO is {@code null}
   * @throws BusinessValidationException if an input field violates its contract
   */
  @Override
  public TransitionCaseDto validate(TransitionCaseDto dto) {
    Objects.requireNonNull(dto, "dto");
    return TransitionCaseDto.builder()
        .id(ServiceValidationUtil.requireRecordId(dto.getId()))
        .destination(StringsUtil.requireAllowedChoice(dto.getDestination(), CaseStatus.getCodes()))
        .build();
  }
}
