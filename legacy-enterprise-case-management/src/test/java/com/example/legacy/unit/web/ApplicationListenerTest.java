package com.example.legacy.unit.web;

import static org.mockito.Mockito.mockStatic;

import com.example.legacy.db.DatabaseUtil;
import com.example.legacy.web.ApplicationListener;
import org.junit.Test;

/**
 * Behavioral contracts for ApplicationListener.
 *
 * @author Chuck Emmons
 */
public class ApplicationListenerTest {

  /**
   * Initializes lazily and closes the shared factory on shutdown.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void initializesLazilyAndClosesTheSharedFactoryOnShutdown() {
    try (org.mockito.MockedStatic<DatabaseUtil> database = mockStatic(DatabaseUtil.class); ) {
      ApplicationListener listener = new ApplicationListener();
      listener.contextInitialized(null);
      database.verifyNoInteractions();
      listener.contextDestroyed(null);
      database.verify(DatabaseUtil::closeSessionFactory);
    }
  }
}
