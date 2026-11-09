package com.example.legacy.service;

import com.example.legacy.service.dto.AddCaseCommentDto;
import com.example.legacy.service.dto.AssignCaseDto;
import com.example.legacy.service.dto.CaseDetails;
import com.example.legacy.service.dto.CaseDto;
import com.example.legacy.service.dto.CaseSearchCriteriaDto;
import com.example.legacy.service.dto.CreateCaseDto;
import com.example.legacy.service.dto.RecordReferenceDto;
import com.example.legacy.service.dto.TransitionCaseDto;
import com.example.legacy.service.dto.UpdateCaseDto;
import java.util.List;

/**
 * Application boundary for case queries and commands; implementations own transaction boundaries.
 *
 * @author Chuck Emmons
 */
public interface CaseService {
  /**
   * Executes the search use case for the trusted actor and immutable input.
   *
   * @param actor the authenticated actor's username
   * @param request the case filters and requested page
   * @return the case snapshots matching the filters and requested page
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws NullPointerException if {@code request} is {@code null}
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  List<CaseDto> searchCases(String actor, CaseSearchCriteriaDto request);

  /**
   * Executes the details use case for the trusted actor and immutable input.
   *
   * @param actor the authenticated actor's username
   * @param request the record identifier
   * @return the case snapshot and its associated notes, documents, and permitted transitions
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws com.example.legacy.service.exception.RecordNotFoundException if the requested case does
   *     not exist or is archived
   * @throws NullPointerException if {@code request} is {@code null}
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  CaseDetails getCaseDetails(String actor, RecordReferenceDto request);

  /**
   * Executes the createCase use case for the trusted actor and immutable input.
   *
   * @param actor the authenticated actor's username
   * @param request the initial case details
   * @return the newly created case snapshot
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws NullPointerException if {@code request} is {@code null}
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  CaseDto createCase(String actor, CreateCaseDto request);

  /**
   * Executes the update use case for the trusted actor and immutable input.
   *
   * @param actor the authenticated actor's username
   * @param request the case reference, expected version, and editable fields
   * @return the case snapshot after applying the requested changes
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws com.example.legacy.service.exception.RecordNotFoundException if the requested case does
   *     not exist or is archived
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws NullPointerException if {@code request} is {@code null}
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  CaseDto updateCase(String actor, UpdateCaseDto request);

  /**
   * Executes the assign use case for the trusted actor and immutable input.
   *
   * @param actor the authenticated actor's username
   * @param request the case reference and assignee
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws com.example.legacy.service.exception.BusinessValidationException if the request fields
   *     are invalid or the case is closed
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws com.example.legacy.service.exception.RecordNotFoundException if the requested case does
   *     not exist or is archived
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws NullPointerException if {@code request} is {@code null}
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  void assignCase(String actor, AssignCaseDto request);

  /**
   * Executes the transition use case for the trusted actor and immutable input.
   *
   * @param actor the authenticated actor's username
   * @param request the case reference and destination status
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws com.example.legacy.service.exception.BusinessValidationException if the request fields
   *     are invalid or the case creator attempts self-approval
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws com.example.legacy.service.exception.RecordNotFoundException if the requested case does
   *     not exist or is archived
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws NullPointerException if {@code actor} or {@code request} is {@code null}
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  void transitionCase(String actor, TransitionCaseDto request);

  /**
   * Executes the comment use case for the trusted actor and immutable input.
   *
   * @param actor the authenticated actor's username
   * @param request the case reference and comment text
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws com.example.legacy.service.exception.RecordNotFoundException if the requested case does
   *     not exist or is archived
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws NullPointerException if {@code actor} or {@code request} is {@code null}
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  void addCaseComment(String actor, AddCaseCommentDto request);

  /**
   * Executes the archive use case for the trusted actor and immutable input.
   *
   * @param actor the authenticated actor's username
   * @param request the record identifier
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws com.example.legacy.service.exception.RecordNotFoundException if the requested case does
   *     not exist or is archived
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws NullPointerException if {@code request} is {@code null}
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  void archiveCase(String actor, RecordReferenceDto request);
}
