package com.example.legacy.service.validation;

import com.example.legacy.domain.AuditAction;
import com.example.legacy.domain.AuditEntity;
import com.example.legacy.service.dto.AuditEventDto;
import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.util.MessageKeys;
import com.example.legacy.util.ServiceValidationUtil;
import com.example.legacy.util.StringsUtil;
import com.example.legacy.util.ValidationLimits;
import java.util.Objects;

/**
 * Validates a complete audit write, including domain codes, optional identifiers, actor, details,
 * and creation time.
 *
 * @author Chuck Emmons
 */
public final class AuditEventDtoValidator implements DtoValidator<AuditEventDto> {
  /**
   * Checks all input fields and returns a normalized immutable snapshot.
   *
   * @param dto the submitted {@code AuditEventDto}; it is never modified
   * @return the validated input snapshot
   * @throws NullPointerException if the DTO or its creation timestamp is {@code null}
   * @throws BusinessValidationException if an input field violates its contract
   */
  @Override
  public AuditEventDto validate(AuditEventDto dto) {
    Objects.requireNonNull(dto, "dto");
    return AuditEventDto.builder(dto)
        .id(DtoValidation.validateOptionalId(dto.getId()))
        .entityType(StringsUtil.requireAllowedChoice(dto.getEntityType(), AuditEntity.getCodes()))
        .entityId(DtoValidation.validateOptionalId(dto.getEntityId()))
        .actionName(StringsUtil.requireAllowedChoice(dto.getActionName(), AuditAction.getCodes()))
        .actor(
            ServiceValidationUtil.requireText(
                dto.getActor(), ValidationLimits.USERNAME, MessageKeys.FIELD_USERNAME))
        .details(StringsUtil.normalizeText(dto.getDetails(), ValidationLimits.DETAILS))
        .createdAt(Objects.requireNonNull(dto.getCreatedAt(), "createdAt"))
        .build();
  }
}
