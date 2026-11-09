package com.example.legacy.unit.service.exception;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import com.example.legacy.service.exception.AccessDeniedException;
import com.example.legacy.service.exception.ApplicationException;
import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.service.exception.ConcurrentUpdateException;
import com.example.legacy.service.exception.MessageArgument;
import com.example.legacy.service.exception.RecordNotFoundException;
import org.junit.Test;

/**
 * Behavioral contracts for ApplicationException.
 *
 * @author Chuck Emmons
 */
public class ApplicationExceptionTest {

  /**
   * Preserves cause and defensively copies presentation arguments.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void preservesCauseAndDefensivelyCopiesPresentationArguments() {
    Throwable cause = new IllegalStateException("cause");
    Object[] arguments = {"original"};
    for (ApplicationException failure :
        new ApplicationException[] {
          new AccessDeniedException(cause, "key", arguments),
          new BusinessValidationException(cause, "key", arguments),
          new RecordNotFoundException(cause, "key", arguments),
          new ConcurrentUpdateException(cause, "key", arguments)
        }) {
      assertSame(cause, failure.getCause());
      assertEquals("key", failure.getMessageKey());
      failure.getArguments()[0] = "changed";
      assertEquals("original", failure.getArguments()[0]);
    }
    arguments[0] = "changed";
    assertEquals(0, new AccessDeniedException("key", (Object[]) null).getArguments().length);
    assertThrows(NullPointerException.class, () -> new BusinessValidationException((String) null));
    assertEquals("field.key", new MessageArgument("field.key").getKey());
    assertThrows(NullPointerException.class, () -> new MessageArgument(null));
  }
}
