package com.example.legacy.service.validation;

import com.example.legacy.domain.Role;
import com.example.legacy.service.dto.ChangeAccountRoleDto;
import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.util.ServiceValidationUtil;
import com.example.legacy.util.StringsUtil;
import java.util.Objects;

/**
 * Validates the account reference and role code without applying self-change restrictions.
 *
 * @author Chuck Emmons
 */
public final class ChangeAccountRoleDtoValidator implements DtoValidator<ChangeAccountRoleDto> {
  /**
   * Checks all input fields and returns a normalized immutable snapshot.
   *
   * @param dto the submitted {@code ChangeAccountRoleDto}; it is never modified
   * @return the validated input snapshot
   * @throws NullPointerException if the DTO is {@code null}
   * @throws BusinessValidationException if an input field violates its contract
   */
  @Override
  public ChangeAccountRoleDto validate(ChangeAccountRoleDto dto) {
    Objects.requireNonNull(dto, "dto");
    return ChangeAccountRoleDto.builder()
        .id(ServiceValidationUtil.requireRecordId(dto.getId()))
        .role(StringsUtil.requireAllowedChoice(dto.getRole(), Role.getCodes()))
        .build();
  }
}
