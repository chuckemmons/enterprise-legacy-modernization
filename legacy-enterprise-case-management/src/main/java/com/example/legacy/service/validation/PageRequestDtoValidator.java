package com.example.legacy.service.validation;

import com.example.legacy.service.dto.PageRequestDto;
import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.util.ServiceValidationUtil;
import java.util.Objects;

/**
 * Validates the supported range of zero-based page indexes.
 *
 * @author Chuck Emmons
 */
public final class PageRequestDtoValidator implements DtoValidator<PageRequestDto> {
  /**
   * Checks all input fields and returns a normalized immutable snapshot.
   *
   * @param dto the submitted {@code PageRequestDto}; it is never modified
   * @return the validated input snapshot
   * @throws NullPointerException if the DTO is {@code null}
   * @throws BusinessValidationException if an input field violates its contract
   */
  @Override
  public PageRequestDto validate(PageRequestDto dto) {
    Objects.requireNonNull(dto, "dto");
    ServiceValidationUtil.requirePageIndex(dto.getPage());
    return dto;
  }
}
