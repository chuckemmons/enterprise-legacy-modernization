package com.example.legacy.util;

import com.example.legacy.domain.CaseStatus;
import com.example.legacy.domain.Role;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Defines valid case status transitions and the roles allowed to perform them. Case ownership and
 * self-approval checks are enforced by the service layer.
 *
 * @author Chuck Emmons
 */
public final class WorkflowUtil {
  private static final Map<String, Set<String>> nextStatuses = new HashMap<String, Set<String>>();

  static {
    registerTransitions(CaseStatus.NEW.name(), CaseStatus.TRIAGED.name());
    registerTransitions(CaseStatus.TRIAGED.name(), CaseStatus.IN_PROGRESS.name());
    registerTransitions(CaseStatus.IN_PROGRESS.name(), CaseStatus.PENDING_APPROVAL.name());
    registerTransitions(
        CaseStatus.PENDING_APPROVAL.name(), CaseStatus.APPROVED.name(), CaseStatus.REJECTED.name());
    registerTransitions(CaseStatus.REJECTED.name(), CaseStatus.IN_PROGRESS.name());
    registerTransitions(CaseStatus.APPROVED.name(), CaseStatus.CLOSED.name());
  }

  /** Prevents instantiation of this static utility class. */
  private WorkflowUtil() {}

  /**
   * Returns whether the workflow graph contains the requested edge; unknown and terminal source
   * states return {@code false}.
   *
   * @param sourceStatus the current workflow status code
   * @param destinationStatus the requested destination status code
   * @return whether the workflow graph contains the requested edge; unknown and terminal source
   *     states return {@code false}
   */
  public static boolean isTransitionAllowed(String sourceStatus, String destinationStatus) {
    return nextStatuses.containsKey(sourceStatus)
        && nextStatuses.get(sourceStatus).contains(destinationStatus);
  }

  /**
   * Checks the workflow edge and role permissions. Administrators and supervisors may take any
   * defined edge; workers may start work, request approval, or resume rejected work. Ownership and
   * self-approval are checked separately by CaseService.
   *
   * @param role the account role code
   * @param sourceStatus the current workflow status code
   * @param destinationStatus the requested destination status code
   * @return {@code true} if the workflow edge exists and the role permits it; {@code false}
   *     otherwise
   */
  public static boolean canTransitionCase(
      String role, String sourceStatus, String destinationStatus) {
    if (!isTransitionAllowed(sourceStatus, destinationStatus)) {
      return false;
    }
    if (Role.ADMIN.name().equals(role) || Role.SUPERVISOR.name().equals(role)) {
      return true;
    }
    return Role.WORKER.name().equals(role)
        && ((CaseStatus.TRIAGED.name().equals(sourceStatus)
                && CaseStatus.IN_PROGRESS.name().equals(destinationStatus))
            || (CaseStatus.IN_PROGRESS.name().equals(sourceStatus)
                && CaseStatus.PENDING_APPROVAL.name().equals(destinationStatus))
            || (CaseStatus.REJECTED.name().equals(sourceStatus)
                && CaseStatus.IN_PROGRESS.name().equals(destinationStatus)));
  }

  /**
   * Returns whether the role is ADMIN, SUPERVISOR, or WORKER; auditors and unknown roles are
   * read-only.
   *
   * @param role the account role code
   * @return whether the role is ADMIN, SUPERVISOR, or WORKER; auditors and unknown roles are
   *     read-only
   */
  public static boolean canWorkOnCases(String role) {
    return Role.ADMIN.name().equals(role)
        || Role.SUPERVISOR.name().equals(role)
        || Role.WORKER.name().equals(role);
  }

  /**
   * Returns whether the role has supervisor privileges, including administrators.
   *
   * @param role the account role code
   * @return whether the role has supervisor privileges, including administrators
   */
  public static boolean isSupervisor(String role) {
    return Role.ADMIN.name().equals(role) || Role.SUPERVISOR.name().equals(role);
  }

  /**
   * Returns a new insertion-ordered set of destinations permitted for the role. Unknown statuses
   * and roles produce an empty set; case-specific restrictions are not evaluated.
   *
   * @param status the case status code
   * @param role the account role code
   * @return a new insertion-ordered set of destinations permitted for the role
   */
  public static Set<String> getNextStatuses(String status, String role) {
    Set<String> results = new LinkedHashSet<String>();
    if (nextStatuses.containsKey(status)) {
      for (String destinationStatus : nextStatuses.get(status)) {
        if (canTransitionCase(role, status, destinationStatus)) {
          results.add(destinationStatus);
        }
      }
    }
    return results;
  }

  /**
   * Registers outgoing transitions in insertion order for a source status during static
   * initialization.
   *
   * @param sourceStatus the current workflow status code
   * @param destinationStatuses the allowed destination status codes
   */
  private static void registerTransitions(String sourceStatus, String... destinationStatuses) {
    nextStatuses.put(sourceStatus, new LinkedHashSet<String>(Arrays.asList(destinationStatuses)));
  }
}
