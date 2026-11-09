package com.example.legacy.unit.bootstrap;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.legacy.bootstrap.RunJobs;
import com.example.legacy.db.DatabaseUtil;
import com.example.legacy.service.BatchJobService;
import com.example.legacy.service.exception.BusinessValidationException;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/**
 * Behavioral contracts for RunJobs.
 *
 * @author Chuck Emmons
 */
public class RunJobsTest {

  /**
   * Dispatches both job kinds and always closes the factory.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void dispatchesBothJobKindsAndAlwaysClosesTheFactory() {
    try (MockedConstruction<BatchJobService> jobs = mockConstruction(BatchJobService.class);
        MockedStatic<DatabaseUtil> database = mockStatic(DatabaseUtil.class); ) {
      RunJobs.main(new String[] {"overdue"});
      verify(jobs.constructed().get(0)).createOverdueNotifications();
      RunJobs.main(new String[] {"email"});
      verify(jobs.constructed().get(1)).sendPendingEmail();
      database.verify(DatabaseUtil::closeSessionFactory, times(2));
      assertThrows(BusinessValidationException.class, () -> RunJobs.main(new String[] {"unknown"}));
      database.verify(DatabaseUtil::closeSessionFactory, times(3));
      assertThrows(BusinessValidationException.class, () -> RunJobs.main(new String[0]));
    }
  }

  /**
   * Cleans up after job failures.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void cleansUpAfterJobFailures() {
    try (MockedConstruction<BatchJobService> jobs =
            mockConstruction(
                BatchJobService.class,
                (job, context) ->
                    doThrow(new IllegalStateException("failed")).when(job).sendPendingEmail());
        MockedStatic<DatabaseUtil> database = mockStatic(DatabaseUtil.class); ) {
      assertThrows(IllegalStateException.class, () -> RunJobs.main(new String[] {"email"}));
      database.verify(DatabaseUtil::closeSessionFactory);
    }
  }
}
