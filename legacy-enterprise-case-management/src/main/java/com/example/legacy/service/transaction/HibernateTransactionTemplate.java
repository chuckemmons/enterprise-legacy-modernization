package com.example.legacy.service.transaction;

import com.example.legacy.db.DatabaseUtil;
import com.example.legacy.service.exception.ConcurrentUpdateException;
import com.example.legacy.util.MessageKeys;
import java.util.Objects;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StaleStateException;
import org.hibernate.Transaction;

/**
 * Reusable service-layer transaction lifecycle; each invocation owns a fresh, method-local session.
 *
 * @author Chuck Emmons
 */
public class HibernateTransactionTemplate {
  private final SessionFactory sessionFactory;

  /**
   * Creates a template that resolves the application's shared factory only when work is executed.
   */
  public HibernateTransactionTemplate() {
    this(null);
  }

  /**
   * Uses an injected factory, or lazily resolves the application factory when {@code null}.
   *
   * @param sessionFactory the session factory to use, or {@code null} to resolve the shared
   *     application factory lazily
   */
  public HibernateTransactionTemplate(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  /**
   * Commits a callback result, or rolls back failures, preserving secondary rollback and close
   * failures.
   *
   * @param <T> the type of the callback result
   * @param work the callback to execute within the managed transaction
   * @return the callback result after its transaction commits
   * @throws ConcurrentUpdateException if Hibernate reports stale state while executing or
   *     committing the work
   * @throws RuntimeException if the callback or transaction lifecycle fails
   * @throws NullPointerException if {@code work} is {@code null}
   * @throws Error if the transaction callback or session lifecycle raises an error
   */
  public <T> T execute(Work<T> work) {
    Objects.requireNonNull(work, "work");
    Session session =
        (sessionFactory == null ? DatabaseUtil.getSessionFactory() : sessionFactory).openSession();
    Transaction transaction = null;
    Throwable primaryFailure = null;
    try {
      transaction = session.beginTransaction();
      T result = work.execute(session);
      transaction.commit();
      return result;
    } catch (RuntimeException | Error failure) {
      Throwable propagatedFailure =
          failure instanceof StaleStateException
              ? new ConcurrentUpdateException(
                  failure, MessageKeys.BUSINESS_ANOTHER_USER_UPDATED_THIS_CASE_REFRESH_AND_RETRY)
              : failure;
      primaryFailure = propagatedFailure;
      rollbackAfterFailure(transaction, propagatedFailure);
      if (propagatedFailure instanceof Error) {
        throw (Error) propagatedFailure;
      }
      throw (RuntimeException) propagatedFailure;
    } finally {
      closeSession(session, primaryFailure);
    }
  }

  /**
   * Runs a write callback through the same lifecycle without requiring a dummy return value.
   *
   * @param action the callback to execute within the managed transaction
   * @throws ConcurrentUpdateException if Hibernate reports stale state while executing or
   *     committing the action
   * @throws RuntimeException if the callback or transaction lifecycle fails
   * @throws NullPointerException if {@code action} is {@code null}
   * @throws Error if the transaction callback or session lifecycle raises an error
   */
  public void executeWithoutResult(Action action) {
    Objects.requireNonNull(action, "action");
    execute(session -> {
      action.execute(session);
      return null;
    });
  }

  /**
   * Rolls back a started transaction and attaches rollback failures to the failure being
   * propagated.
   *
   * @param transaction the transaction to roll back, or {@code null} if starting it failed
   * @param primaryFailure the nonnull failure that triggered rollback
   * @throws NullPointerException if {@code primaryFailure} is {@code null} and rollback fails
   */
  private void rollbackAfterFailure(Transaction transaction, Throwable primaryFailure) {
    if (transaction == null) {
      return;
    }
    try {
      transaction.rollback();
    } catch (RuntimeException | Error rollbackFailure) {
      if (rollbackFailure != primaryFailure) {
        primaryFailure.addSuppressed(rollbackFailure);
      }
    }
  }

  /**
   * Closes the session, preserving an existing failure or propagating a standalone close failure.
   *
   * @param session the session owned by this invocation
   * @param primaryFailure the failure already being propagated, or {@code null} after a successful
   *     commit
   * @throws RuntimeException if closing fails and no primary failure exists
   * @throws Error if closing raises an error and no primary failure exists
   * @throws NullPointerException if {@code session} is {@code null} and no primary failure exists
   */
  private void closeSession(Session session, Throwable primaryFailure) {
    try {
      session.close();
    } catch (RuntimeException | Error closeFailure) {
      if (primaryFailure == null) {
        throw closeFailure;
      }
      if (closeFailure != primaryFailure) {
        primaryFailure.addSuppressed(closeFailure);
      }
    }
  }

  /**
   * Service work that returns a value and uses the supplied session without managing its lifecycle.
   *
   * @author Chuck Emmons
   * @param <T> the type of the callback result
   */
  @FunctionalInterface
  public interface Work<T> {
    /**
     * Performs business/persistence work inside the template-owned transaction.
     *
     * @param session the Hibernate session used for persistence operations
     * @return the result of the business or persistence work
     */
    T execute(Session session);
  }

  /**
   * Service work with no response value; the template still controls its full transaction
   * lifecycle.
   *
   * @author Chuck Emmons
   */
  @FunctionalInterface
  public interface Action {
    /**
     * Performs writes using the supplied session without starting, committing, or closing it.
     *
     * @param session the Hibernate session used for persistence operations
     */
    void execute(Session session);
  }
}
