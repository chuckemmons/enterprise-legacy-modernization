package com.example.legacy.service.validation;

import com.example.legacy.service.exception.BusinessValidationException;

/**
 * Validates complete DTO input without persistence, authorization, or mapper dependencies.
 *
 * @author Chuck Emmons
 * @param <T> the immutable DTO type
 */
public interface DtoValidator<T> {
  /**
   * Checks the DTO's input fields and returns a normalized immutable snapshot.
   *
   * @param dto the submitted DTO; it is never modified
   * @return the validated DTO, with normalized fields where applicable
   * @throws NullPointerException if the DTO is {@code null}
   * @throws BusinessValidationException if a field violates its input contract
   */
  T validate(T dto);
}
