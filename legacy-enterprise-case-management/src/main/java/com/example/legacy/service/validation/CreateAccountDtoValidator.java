package com.example.legacy.service.validation;

import com.example.legacy.domain.Role;
import com.example.legacy.service.dto.CreateAccountDto;
import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.util.MessageKeys;
import com.example.legacy.util.ServiceValidationUtil;
import com.example.legacy.util.StringsUtil;
import com.example.legacy.util.ValidationLimits;
import java.util.Locale;
import java.util.Objects;
import org.apache.commons.validator.GenericValidator;

/**
 * Validates initial account fields, optional email syntax, and password strength without hashing or
 * lookups.
 *
 * @author Chuck Emmons
 */
public final class CreateAccountDtoValidator implements DtoValidator<CreateAccountDto> {
  /**
   * Checks all input fields and returns a normalized immutable snapshot.
   *
   * @param dto the submitted {@code CreateAccountDto}; it is never modified
   * @return the validated input snapshot
   * @throws NullPointerException if the DTO is {@code null}
   * @throws BusinessValidationException if an input field violates its contract
   */
  @Override
  public CreateAccountDto validate(CreateAccountDto dto) {
    Objects.requireNonNull(dto, "dto");
    String username =
        ServiceValidationUtil.requireText(
                dto.getUsername(), ValidationLimits.USERNAME, MessageKeys.FIELD_USERNAME)
            .toLowerCase(Locale.ROOT);
    if (!username.matches(ValidationLimits.USERNAME_PATTERN)) {
      throw new BusinessValidationException(
          MessageKeys.BUSINESS_USERNAME_MUST_START_WITH_A_LETTER_3_60_CHARACTERS);
    }
    String email = StringsUtil.normalizeText(dto.getEmail(), ValidationLimits.EMAIL);
    if (!email.isEmpty() && !GenericValidator.isEmail(email)) {
      throw new BusinessValidationException(MessageKeys.VALIDATION_EMAIL);
    }
    return CreateAccountDto.builder()
        .username(username)
        .displayName(
            ServiceValidationUtil.requireText(
                dto.getDisplayName(), ValidationLimits.DISPLAY_NAME, MessageKeys.FIELD_NAME))
        .email(email)
        .role(StringsUtil.requireAllowedChoice(dto.getRole(), Role.getCodes()))
        .department(
            ServiceValidationUtil.requireText(
                dto.getDepartment(), ValidationLimits.DEPARTMENT, MessageKeys.FIELD_DEPARTMENT))
        .password(ServiceValidationUtil.requirePassword(dto.getPassword()))
        .build();
  }
}
