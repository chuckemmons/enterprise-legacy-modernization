package com.example.legacy.service;

import com.example.legacy.domain.CaseStatus;
import com.example.legacy.domain.Role;
import com.example.legacy.model.Account;
import com.example.legacy.model.ServiceCase;
import com.example.legacy.service.exception.AccessDeniedException;
import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.util.MessageKeys;
import com.example.legacy.util.WorkflowUtil;
import java.util.Set;

/**
 * Case-specific permissions shared by service mutations and detail-screen capability flags.
 *
 * @author Chuck Emmons
 */
public final class CasePolicyService {
  /**
   * Requires a working role without depending on a higher-level case service.
   *
   * @param actor the authenticated actor's account
   * @throws AccessDeniedException if the account has a read-only role
   * @throws NullPointerException if {@code actor} is {@code null}
   */
  public void requireCaseWorkPermission(Account actor) {
    if (!WorkflowUtil.canWorkOnCases(actor.getRole())) {
      throw new AccessDeniedException(MessageKeys.BUSINESS_READ_ONLY_ACCESS);
    }
  }

  /**
   * Returns whether the role and case ownership permit editing a nonclosed case.
   *
   * @param actor the authenticated actor's account
   * @param item the case whose permissions are checked
   * @return whether the role and case ownership permit editing a nonclosed case
   * @throws NullPointerException if {@code actor} or {@code item} is {@code null}
   */
  public boolean canEditCase(Account actor, ServiceCase item) {
    return WorkflowUtil.canWorkOnCases(actor.getRole())
        && !CaseStatus.CLOSED.name().equals(item.getStatus())
        && (!Role.WORKER.name().equals(actor.getRole())
            || actor.getUsername().equals(item.getAssignedTo())
            || actor.getUsername().equals(item.getCreatedBy()));
  }

  /**
   * Enforces working-role, open-case, and worker-ownership restrictions before editing or
   * uploading.
   *
   * @param actor the authenticated actor's account
   * @param item the case whose permissions are checked
   * @throws AccessDeniedException if the actor has a read-only role or lacks ownership or
   *     assignment permission to edit the case
   * @throws BusinessValidationException if the case is closed
   * @throws NullPointerException if {@code actor} or {@code item} is {@code null}
   */
  public void requireCaseEditPermission(Account actor, ServiceCase item) {
    requireCaseWorkPermission(actor);

    if (CaseStatus.CLOSED.name().equals(item.getStatus())) {
      throw new BusinessValidationException(MessageKeys.BUSINESS_CLOSED_CASES_CANNOT_BE_EDITED);
    }

    if (!canEditCase(actor, item)) {
      throw new AccessDeniedException(MessageKeys.BUSINESS_CASE_NOT_ASSIGNED_TO_YOU);
    }
  }

  /**
   * Returns whether role, assignment, and creator self-approval rules permit the destination.
   *
   * @param actor the authenticated actor's account
   * @param item the case whose permissions are checked
   * @param destination the requested destination status code
   * @return whether role, assignment, and creator self-approval rules permit the destination
   * @throws NullPointerException if {@code actor} or {@code item} is {@code null}
   */
  public boolean canTransitionCase(Account actor, ServiceCase item, String destination) {
    return WorkflowUtil.canTransitionCase(actor.getRole(), item.getStatus(), destination)
        && (!Role.WORKER.name().equals(actor.getRole())
            || actor.getUsername().equals(item.getAssignedTo()))
        && !(CaseStatus.PENDING_APPROVAL.name().equals(item.getStatus())
            && CaseStatus.APPROVED.name().equals(destination)
            && actor.getUsername().equals(item.getCreatedBy()));
  }

  /**
   * Enforces the same transition rules used to calculate available workflow choices.
   *
   * @param actor the authenticated actor's account
   * @param item the case whose permissions are checked
   * @param destination the requested destination status code
   * @throws AccessDeniedException if the role does not permit the transition or the worker is not
   *     assigned to the case
   * @throws BusinessValidationException if the case creator attempts to approve their own case
   * @throws NullPointerException if {@code actor} or {@code item} is {@code null}
   */
  public void requireCaseTransitionPermission(Account actor, ServiceCase item, String destination) {
    if (!WorkflowUtil.canTransitionCase(actor.getRole(), item.getStatus(), destination)) {
      throw new AccessDeniedException(MessageKeys.BUSINESS_TRANSITION_NOT_PERMITTED);
    }

    if (Role.WORKER.name().equals(actor.getRole())
        && !actor.getUsername().equals(item.getAssignedTo())) {
      throw new AccessDeniedException(
          MessageKeys.BUSINESS_ONLY_THE_ASSIGNED_WORKER_MAY_CHANGE_STATUS);
    }

    if (!canTransitionCase(actor, item, destination)) {
      throw new BusinessValidationException(MessageKeys.BUSINESS_CREATOR_CANNOT_SELF_APPROVE);
    }
  }

  /**
   * Returns role-based destinations filtered by assignment and self-approval rules.
   *
   * @param actor the authenticated actor's account
   * @param item the case whose permissions are checked
   * @return role-based destinations filtered by assignment and self-approval rules
   * @throws NullPointerException if {@code actor} or {@code item} is {@code null}
   */
  public Set<String> getAllowedNextStatuses(Account actor, ServiceCase item) {
    Set<String> nextStatuses = WorkflowUtil.getNextStatuses(item.getStatus(), actor.getRole());

    nextStatuses.removeIf(destination -> !canTransitionCase(actor, item, destination));

    return nextStatuses;
  }
}
