package com.example.legacy.web;

import com.example.legacy.util.MessageKeys;
import java.util.Arrays;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMessage;

/**
 * Presentation checks that complement declarative ValidatorForm rules without querying services or
 * DAOs.
 *
 * @author Chuck Emmons
 */
public final class FormValidationUtil {
  /** Prevents construction of this form-validation utility. */
  private FormValidationUtil() {}

  /**
   * Returns whether a text control has no non-whitespace input.
   *
   * @param value the text to check for blank input
   * @return whether a text control has no non-whitespace input
   */
  public static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  /**
   * Rejects select values that the UI never offers; XML rules handle required selections.
   *
   * @param errors the collection to which validation errors are added
   * @param field the form field name
   * @param label the field label
   * @param value the value to validate or convert
   * @param choices the allowed case-sensitive values
   * @throws NullPointerException if {@code errors} is {@code null}
   */
  public static void validateChoice(
      ActionErrors errors, String field, String label, String value, String... choices) {
    if (!isBlank(value) && !Arrays.asList(choices).contains(value)) {
      errors.add(field, new ActionMessage(MessageKeys.VALIDATION_CHOICE, label));
    }
  }

  /**
   * Checks optional record-ID syntax; operation-specific XML rules require IDs when needed.
   *
   * @param errors the collection to which validation errors are added
   * @param value the submitted record identifier
   * @throws NullPointerException if {@code errors} is {@code null}
   */
  public static void validateRecordId(ActionErrors errors, String value) {
    if (errors.get(FormFields.ID).hasNext() || isBlank(value)) {
      return;
    }
    try {
      if (Long.parseLong(value) > 0) {
        return;
      }
    } catch (NumberFormatException ignored) {
      // Malformed and nonpositive IDs both produce the field error below.
    }
    errors.add(FormFields.ID, new ActionMessage(MessageKeys.VALIDATION_RECORD));
  }
}
