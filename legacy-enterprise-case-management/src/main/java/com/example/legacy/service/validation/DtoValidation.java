package com.example.legacy.service.validation;

import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.util.MessageKeys;
import com.example.legacy.util.ServiceValidationUtil;
import com.example.legacy.util.ValidationLimits;

/**
 * Shared scalar checks used only by complete DTO validators.
 *
 * @author Chuck Emmons
 */
final class DtoValidation {
  /** Prevents construction of this validation helper. */
  private DtoValidation() {}

  /**
   * Checks an identifier only when its field is populated.
   *
   * @param id the optional identifier
   * @return the identifier, or {@code null} when omitted
   * @throws BusinessValidationException if a supplied identifier is not positive
   */
  static Long validateOptionalId(Long id) {
    return id == null ? null : ServiceValidationUtil.requireRecordId(id);
  }

  /**
   * Checks the syntax of a submitted optimistic-lock version without comparing database state.
   *
   * @param version the submitted version
   * @return the nonnegative version
   * @throws BusinessValidationException if the version is missing or negative
   */
  static Integer requireVersion(Integer version) {
    if (version == null || version < 0) {
      throw new BusinessValidationException(MessageKeys.BUSINESS_INVALID_RECORD_VERSION);
    }
    return version;
  }

  /**
   * Applies the shared nonempty attachment size limit.
   *
   * @param size the declared or actual number of attachment bytes
   * @throws BusinessValidationException if the size is outside the supported range
   */
  static void requireAttachmentSize(int size) {
    if (size < 1 || size > ValidationLimits.ATTACHMENT_BYTES) {
      throw new BusinessValidationException(MessageKeys.BUSINESS_FILE_MUST_BE_1_BYTE_TO_2_MB);
    }
  }
}
