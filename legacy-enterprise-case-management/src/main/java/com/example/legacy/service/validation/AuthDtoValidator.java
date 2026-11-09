package com.example.legacy.service.validation;

import com.example.legacy.service.dto.AuthDto;
import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.service.exception.MessageArgument;
import com.example.legacy.util.MessageKeys;
import com.example.legacy.util.ServiceValidationUtil;
import com.example.legacy.util.ValidationLimits;
import java.util.Locale;
import java.util.Objects;

/**
 * Validates login fields without imposing new-password rules or trusting an incoming account
 * response.
 *
 * @author Chuck Emmons
 */
public final class AuthDtoValidator implements DtoValidator<AuthDto> {
  /**
   * Checks all input fields and returns a normalized immutable snapshot.
   *
   * @param dto the submitted {@code AuthDto}; it is never modified
   * @return the validated input snapshot
   * @throws NullPointerException if the DTO is {@code null}
   * @throws BusinessValidationException if an input field violates its contract
   */
  @Override
  public AuthDto validate(AuthDto dto) {
    Objects.requireNonNull(dto, "dto");
    String username =
        ServiceValidationUtil.requireText(
                dto.getUsername(), ValidationLimits.USERNAME, MessageKeys.FIELD_USERNAME)
            .toLowerCase(Locale.ROOT);
    if (dto.getPassword() == null || dto.getPassword().isEmpty()) {
      throw new BusinessValidationException(
          MessageKeys.BUSINESS_FIELD_REQUIRED, new MessageArgument(MessageKeys.FIELD_PASSWORD));
    }
    return AuthDto.builder().username(username).password(dto.getPassword()).build();
  }
}
