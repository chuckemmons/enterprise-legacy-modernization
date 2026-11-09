package com.example.legacy.service.validation;

import com.example.legacy.domain.CaseStatus;
import com.example.legacy.service.dto.CaseSearchCriteriaDto;
import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.util.ServiceValidationUtil;
import com.example.legacy.util.StringsUtil;
import com.example.legacy.util.ValidationLimits;
import java.util.Objects;

/**
 * Validates optional case filters and pagination, retaining blank filters as unrestricted queries.
 *
 * @author Chuck Emmons
 */
public final class CaseSearchCriteriaDtoValidator implements DtoValidator<CaseSearchCriteriaDto> {
  /**
   * Checks all input fields and returns a normalized immutable snapshot.
   *
   * @param dto the submitted {@code CaseSearchCriteriaDto}; it is never modified
   * @return the validated input snapshot
   * @throws NullPointerException if the DTO is {@code null}
   * @throws BusinessValidationException if an input field violates its contract
   */
  @Override
  public CaseSearchCriteriaDto validate(CaseSearchCriteriaDto dto) {
    Objects.requireNonNull(dto, "dto");
    String status = dto.getStatus();
    if (status != null && !status.isEmpty()) {
      StringsUtil.requireAllowedChoice(status, CaseStatus.getCodes());
    }
    return CaseSearchCriteriaDto.builder()
        .query(
            StringsUtil.convertToLowerCase(
                StringsUtil.normalizeText(dto.getQuery(), ValidationLimits.SEARCH)))
        .status(status)
        .assignee(StringsUtil.normalizeText(dto.getAssignee(), ValidationLimits.USERNAME))
        .page(ServiceValidationUtil.requirePageIndex(dto.getPage()))
        .build();
  }
}
