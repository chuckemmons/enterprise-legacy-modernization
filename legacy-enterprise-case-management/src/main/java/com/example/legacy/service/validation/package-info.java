/**
 * Complete, stateless DTO input validation independent of HTTP, persistence, and mappers.
 *
 * <p>Each validator checks one command or write DTO and returns an immutable normalized snapshot.
 * Validation fails on the first invalid field using the existing application message keys.
 * Optional, already-parsed dates are copied without imposing a new scheduling policy.
 *
 * <p>Services validate commands before opening transactions. They retain authorization, record
 * lookups, uniqueness, workflow decisions, and comparisons with persisted optimistic-lock versions.
 * Audit and notification services validate their assembled write DTOs on the caller's transaction.
 *
 * <p>Document validation applies to upload content, not to download or metadata-only responses.
 * Server-owned upload identity, ownership, size, and timestamps are discarded or derived rather
 * than trusted. Login validation similarly ignores the response-only account field.
 * Presentation/page DTOs and database response snapshots are not input commands.
 */
package com.example.legacy.service.validation;
