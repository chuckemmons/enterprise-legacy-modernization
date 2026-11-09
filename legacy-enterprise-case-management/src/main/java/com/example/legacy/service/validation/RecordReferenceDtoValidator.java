package com.example.legacy.service.validation;

import com.example.legacy.service.dto.RecordReferenceDto;
import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.util.ServiceValidationUtil;
import java.util.Objects;

/**
 * Validates a persistent record identifier without checking existence or access.
 *
 * @author Chuck Emmons
 */
public final class RecordReferenceDtoValidator implements DtoValidator<RecordReferenceDto> {
  /**
   * Checks all input fields and returns a normalized immutable snapshot.
   *
   * @param dto the submitted {@code RecordReferenceDto}; it is never modified
   * @return the validated input snapshot
   * @throws NullPointerException if the DTO is {@code null}
   * @throws BusinessValidationException if an input field violates its contract
   */
  @Override
  public RecordReferenceDto validate(RecordReferenceDto dto) {
    Objects.requireNonNull(dto, "dto");
    ServiceValidationUtil.requireRecordId(dto.getId());
    return dto;
  }
}
