package com.example.legacy.bootstrap;

import com.example.legacy.db.DatabaseUtil;
import com.example.legacy.service.BootstrapService;
import com.example.legacy.util.EnvironmentKeys;

/**
 * Command-line adapter for explicitly invoking account provisioning from environment variables.
 *
 * @author Chuck Emmons
 */
public final class SeedDemo {
  /**
   * Passes seed configuration to the service, reports success, and always releases database
   * resources.
   *
   * @param args the command-line arguments
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws com.example.legacy.service.exception.BusinessValidationException if the command-line
   *     options or configured account and case data fails validation
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws com.example.legacy.service.exception.RecordNotFoundException if the requested case does
   *     not exist or is archived
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws IllegalStateException if PBKDF2 password key derivation is unavailable or fails
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  public static void main(String[] args) {
    BootstrapService bootstrapService = new BootstrapService();

    try {
      bootstrapService.seedDemo(System.getenv(EnvironmentKeys.CASE_DEMO_PASSWORD));
      System.out.println(
          "Created demo users and eight cases. Do not use shared demo passwords in production.");
    } finally {
      DatabaseUtil.closeSessionFactory();
    }
  }
}
