package com.example.legacy.bootstrap;

import com.example.legacy.db.DatabaseUtil;
import com.example.legacy.service.BatchJobService;
import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.util.MessageKeys;

/**
 * Trigger from a controlled scheduler or a cron calling Maven.
 *
 * @author Chuck Emmons
 */
public final class RunJobs {
  /**
   * Accepts exactly one job name, overdue or email, invokes that job, and closes Hibernate in a
   * finally block.
   *
   * @param args the command-line arguments
   * @throws BusinessValidationException if the command-line options or configured account and case
   *     data fails validation
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws NullPointerException if {@code args} is {@code null}
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  public static void main(String[] args) {
    BatchJobService batchJobService = new BatchJobService();

    if (args.length != 1) {
      throw new BusinessValidationException(MessageKeys.BUSINESS_EXPECTED_OVERDUE_OR_EMAIL);
    }
    try {
      if (JobType.OVERDUE.matchesArgument(args[0])) {
        batchJobService.createOverdueNotifications();
      } else if (JobType.EMAIL.matchesArgument(args[0])) {
        batchJobService.sendPendingEmail();
      } else {
        throw new BusinessValidationException(MessageKeys.BUSINESS_EXPECTED_OVERDUE_OR_EMAIL);
      }
    } finally {
      DatabaseUtil.closeSessionFactory();
    }
  }
}
