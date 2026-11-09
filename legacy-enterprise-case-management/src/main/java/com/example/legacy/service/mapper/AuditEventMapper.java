package com.example.legacy.service.mapper;

import com.example.legacy.model.AuditEvent;
import com.example.legacy.service.dto.AuditEventDto;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts AuditEvent entities and AuditEventDto values without exposing persistence objects to the
 * web tier.
 *
 * @author Chuck Emmons
 */
public class AuditEventMapper {
  /**
   * Copies an entity to transport data, excluding credentials and mutable-value aliases.
   *
   * @param source the source value to copy or map
   * @return the mapped immutable audit event DTO, or {@code null} if the source is {@code null}
   */
  public AuditEventDto mapToDto(AuditEvent source) {
    if (source == null) {
      return null;
    }
    return AuditEventDto.builder()
        .id(source.getId())
        .entityType(source.getEntityType())
        .entityId(source.getEntityId())
        .actionName(source.getActionName())
        .actor(source.getActor())
        .details(source.getDetails())
        .createdAt(source.getCreatedAt())
        .build();
  }

  /**
   * Maps only writable fields into a new entity; services assign identity, ownership, and lifecycle
   * state.
   *
   * @param source the source value to copy or map
   * @return the mapped audit event, or {@code null} if the source is {@code null}
   */
  public AuditEvent mapToEntity(AuditEventDto source) {
    if (source == null) {
      return null;
    }
    AuditEvent target = new AuditEvent();
    target.setEntityType(source.getEntityType());
    target.setEntityId(source.getEntityId());
    target.setActionName(source.getActionName());
    target.setActor(source.getActor());
    target.setDetails(source.getDetails());
    target.setCreatedAt(source.getCreatedAt());
    return target;
  }

  /**
   * Converts query results to an entity-free transport list.
   *
   * @param sources the source values to map
   * @return the mapped immutable values in source order
   */
  public List<AuditEventDto> mapToDtos(List<AuditEvent> sources) {
    List<AuditEventDto> targets = new ArrayList<>();
    for (AuditEvent source : sources) {
      targets.add(mapToDto(source));
    }
    return targets;
  }
}
