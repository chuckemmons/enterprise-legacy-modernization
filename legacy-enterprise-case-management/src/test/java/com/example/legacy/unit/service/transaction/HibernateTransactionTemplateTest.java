package com.example.legacy.unit.service.transaction;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.legacy.service.exception.ConcurrentUpdateException;
import com.example.legacy.service.transaction.HibernateTransactionTemplate;
import org.hibernate.SessionFactory;
import org.hibernate.StaleStateException;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Verifies the production transaction lifecycle using Mockito Hibernate collaborators.
 *
 * @author Chuck Emmons
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class HibernateTransactionTemplateTest {
  @Mock private SessionFactory factory;
  @Mock private Session session;
  @Mock private Transaction transaction;
  @Mock private HibernateTransactionTemplate.Work<String> work;
  private HibernateTransactionTemplate template;

  /** Creates a fresh template without opening a session. */
  @Before
  public void setUp() {
    template = new HibernateTransactionTemplate(factory);
  }

  /**
   * Commits the callback result and closes its session in order.
   *
   * @throws ConcurrentUpdateException if submitted data is stale or Hibernate detects an
   *     optimistic-lock conflict
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void commitsResultThenCloses() {
    stubTransactionLifecycle();
    when(work.execute(session)).thenReturn("result");
    assertEquals("result", template.execute(work));
    InOrder order = inOrder(factory, session, transaction, work);
    order.verify(factory).openSession();
    order.verify(session).beginTransaction();
    order.verify(work).execute(session);
    order.verify(transaction).commit();
    order.verify(session).close();
    verify(transaction, never()).rollback();
  }

  /**
   * Allows a normal {@code null} result without skipping commit or cleanup.
   *
   * @throws ConcurrentUpdateException if submitted data is stale or Hibernate detects an
   *     optimistic-lock conflict
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void commitsNullResults() {
    stubTransactionLifecycle();
    assertNull(template.execute(active -> null));
    verify(transaction).commit();
    verify(session).close();
  }

  /**
   * Uses the same lifecycle for no-result callbacks.
   *
   * @throws ConcurrentUpdateException if submitted data is stale or Hibernate detects an
   *     optimistic-lock conflict
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void executesWriteCallbacks() {
    stubTransactionLifecycle();
    HibernateTransactionTemplate.Action action = mock(HibernateTransactionTemplate.Action.class);
    template.executeWithoutResult(action);
    verify(action).execute(session);
    verify(transaction).commit();
    verify(session).close();
  }

  /**
   * Rejects missing callbacks before opening any session.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void rejectsNullCallbacksWithoutDatabaseAccess() {
    assertThrows(NullPointerException.class, () -> template.execute(null));
    assertThrows(NullPointerException.class, () -> template.executeWithoutResult(null));
    verifyNoInteractions(factory, session, transaction);
  }

  /**
   * Propagates an open failure without inventing a session to close.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void propagatesOpenFailure() {
    RuntimeException expected = new IllegalStateException("open failed");
    when(factory.openSession()).thenThrow(expected);
    assertSame(
        expected, assertThrows(RuntimeException.class, () -> template.execute(active -> null)));
    verifyNoInteractions(session, transaction);
  }

  /**
   * Closes a session when beginning its transaction fails.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void closesAfterBeginFailure() {
    when(factory.openSession()).thenReturn(session);
    RuntimeException expected = new IllegalStateException("begin failed");
    when(session.beginTransaction()).thenThrow(expected);
    assertSame(
        expected, assertThrows(RuntimeException.class, () -> template.execute(active -> null)));
    verify(session).close();
    verifyNoInteractions(transaction);
  }

  /**
   * Rolls back work failures and preserves their identity.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void rollsBackWorkFailure() {
    stubTransactionLifecycle();
    RuntimeException expected = new IllegalStateException("work failed");
    assertSame(
        expected,
        assertThrows(
            RuntimeException.class,
            () ->
                template.execute(active -> {
                  throw expected;
                })));
    InOrder order = inOrder(transaction, session);
    order.verify(transaction).rollback();
    order.verify(session).close();
    verify(transaction, never()).commit();
  }

  /**
   * Attempts rollback when committing successful callback work fails.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void rollsBackCommitFailure() {
    stubTransactionLifecycle();
    RuntimeException expected = new IllegalStateException("commit failed");
    doThrow(expected).when(transaction).commit();
    assertSame(
        expected, assertThrows(RuntimeException.class, () -> template.execute(active -> "value")));
    verify(transaction).rollback();
    verify(session).close();
  }

  /**
   * Attaches cleanup failures to the original work failure.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void suppressesRollbackAndCloseFailures() {
    stubTransactionLifecycle();
    RuntimeException expected = new IllegalStateException("work failed");
    RuntimeException rollback = new IllegalStateException("rollback failed");
    RuntimeException close = new IllegalStateException("close failed");
    doThrow(rollback).when(transaction).rollback();
    doThrow(close).when(session).close();
    assertSame(
        expected,
        assertThrows(
            RuntimeException.class,
            () ->
                template.execute(active -> {
                  throw expected;
                })));
    assertArrayEquals(new Throwable[] {rollback, close}, expected.getSuppressed());
  }

  /**
   * Propagates a standalone close failure after the transaction has already committed.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void propagatesCloseFailureAfterCommit() {
    stubTransactionLifecycle();
    RuntimeException close = new IllegalStateException("close failed");
    doThrow(close).when(session).close();
    assertSame(close, assertThrows(RuntimeException.class, () -> template.execute(active -> null)));
    verify(transaction).commit();
    verify(transaction, never()).rollback();
  }

  /**
   * Avoids self-suppression when a collaborator rethrows the original failure during cleanup.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void preventsSelfSuppression() {
    stubTransactionLifecycle();
    RuntimeException expected = new IllegalStateException("same failure");
    doThrow(expected).when(transaction).rollback();
    doThrow(expected).when(session).close();
    assertSame(
        expected,
        assertThrows(
            RuntimeException.class,
            () ->
                template.execute(active -> {
                  throw expected;
                })));
    assertEquals(0, expected.getSuppressed().length);
    verify(session).close();
  }

  /**
   * Cleans up serious callback errors and retains secondary cleanup errors.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void preservesErrorsDuringCleanup() {
    stubTransactionLifecycle();
    AssertionError expected = new AssertionError("work failed");
    AssertionError rollback = new AssertionError("rollback failed");
    AssertionError close = new AssertionError("close failed");
    doThrow(rollback).when(transaction).rollback();
    doThrow(close).when(session).close();
    assertSame(
        expected,
        assertThrows(
            AssertionError.class,
            () ->
                template.executeWithoutResult(active -> {
                  throw expected;
                })));
    assertArrayEquals(new Throwable[] {rollback, close}, expected.getSuppressed());
  }

  /**
   * Rolls back serious commit failures as well as ordinary runtime failures.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void rollsBackCommitErrors() {
    stubTransactionLifecycle();
    AssertionError expected = new AssertionError("commit failed");
    doThrow(expected).when(transaction).commit();
    assertSame(
        expected, assertThrows(AssertionError.class, () -> template.execute(active -> null)));
    verify(transaction).rollback();
    verify(session).close();
  }

  /**
   * Converts Hibernate version conflicts while preserving the underlying database failure.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void translatesOptimisticLockFailures() {
    stubTransactionLifecycle();
    StaleStateException stale = new StaleStateException("version changed");
    ConcurrentUpdateException failure =
        assertThrows(
            ConcurrentUpdateException.class,
            () ->
                template.execute(active -> {
                  throw stale;
                }));
    assertSame(stale, failure.getCause());
    verify(transaction).rollback();
    verify(session).close();
  }

  /**
   * Keeps rollback and close failures on the translated conflict when committing detects stale
   * state.
   *
   * @throws AssertionError if conflict translation or cleanup ordering changes
   * @throws org.hibernate.HibernateException if configuring a mocked Hibernate operation fails
   */
  @Test
  public void translatesCommitConflictsAndSuppressesCleanupFailures() {
    stubTransactionLifecycle();
    StaleStateException stale = new StaleStateException("commit version changed");
    RuntimeException rollback = new IllegalStateException("rollback failed");
    AssertionError close = new AssertionError("close failed");
    doThrow(stale).when(transaction).commit();
    doThrow(rollback).when(transaction).rollback();
    doThrow(close).when(session).close();
    ConcurrentUpdateException failure =
        assertThrows(ConcurrentUpdateException.class, () -> template.execute(active -> "value"));
    assertSame(stale, failure.getCause());
    assertArrayEquals(new Throwable[] {rollback, close}, failure.getSuppressed());
    InOrder order = inOrder(transaction, session);
    order.verify(transaction).commit();
    order.verify(transaction).rollback();
    order.verify(session).close();
  }

  /**
   * Preserves a failed transaction start when closing the opened session also fails.
   *
   * @throws AssertionError if the original failure is replaced or the callback runs
   * @throws org.hibernate.HibernateException if configuring a mocked Hibernate operation fails
   */
  @Test
  public void suppressesCloseFailureWhenBeginningTransactionFails() {
    RuntimeException begin = new IllegalStateException("begin failed");
    RuntimeException close = new IllegalStateException("close failed");
    when(factory.openSession()).thenReturn(session);
    when(session.beginTransaction()).thenThrow(begin);
    doThrow(close).when(session).close();
    assertSame(begin, assertThrows(RuntimeException.class, () -> template.execute(work)));
    assertArrayEquals(new Throwable[] {close}, begin.getSuppressed());
    verifyNoInteractions(work, transaction);
    verify(session).close();
  }

  /**
   * Propagates a standalone cleanup error after commit without attempting rollback.
   *
   * @throws AssertionError if the cleanup error is replaced or rollback occurs after commit
   * @throws org.hibernate.HibernateException if configuring a mocked Hibernate operation fails
   */
  @Test
  public void propagatesCloseErrorAfterCommit() {
    stubTransactionLifecycle();
    AssertionError close = new AssertionError("close failed");
    doThrow(close).when(session).close();
    assertSame(
        close, assertThrows(AssertionError.class, () -> template.execute(active -> "value")));
    verify(transaction).commit();
    verify(transaction, never()).rollback();
  }

  /**
   * Opens independent sessions when a stateless template is reused.
   *
   * @throws ConcurrentUpdateException if submitted data is stale or Hibernate detects an
   *     optimistic-lock conflict
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void usesFreshSessionsForEachInvocation() {
    stubTransactionLifecycle();
    Session second = mock(Session.class);
    Transaction secondTransaction = mock(Transaction.class);
    when(factory.openSession()).thenReturn(session, second);
    when(second.beginTransaction()).thenReturn(secondTransaction);
    template.execute(active -> null);
    template.execute(active -> null);
    verify(factory, times(2)).openSession();
    verify(session).close();
    verify(second).close();
    verify(transaction).commit();
    verify(secondTransaction).commit();
  }

  /**
   * Resolves the shared factory only when lazy work executes.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void resolvesTheSharedFactoryOnlyWhenLazyWorkExecutes() {
    stubTransactionLifecycle();
    try (org.mockito.MockedStatic<com.example.legacy.db.DatabaseUtil> database =
        mockStatic(com.example.legacy.db.DatabaseUtil.class); ) {
      database.when(com.example.legacy.db.DatabaseUtil::getSessionFactory).thenReturn(factory);
      HibernateTransactionTemplate lazy = new HibernateTransactionTemplate();
      assertEquals("value", lazy.execute(active -> "value"));
      database.verify(com.example.legacy.db.DatabaseUtil::getSessionFactory);
      verify(transaction).commit();
      verify(session).close();
    }
  }

  /**
   * Supplies only the lifecycle operations needed by the current test.
   *
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  private void stubTransactionLifecycle() {
    when(factory.openSession()).thenReturn(session);
    when(session.beginTransaction()).thenReturn(transaction);
  }
}
