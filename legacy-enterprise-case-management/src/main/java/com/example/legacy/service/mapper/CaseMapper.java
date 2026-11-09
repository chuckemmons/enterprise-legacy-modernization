package com.example.legacy.service.mapper;

import com.example.legacy.model.ServiceCase;
import com.example.legacy.service.dto.CaseDto;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts ServiceCase entities and CaseDto values without exposing persistence objects to the web
 * tier.
 *
 * @author Chuck Emmons
 */
public class CaseMapper {
  /**
   * Copies an entity to transport data, excluding credentials and mutable-value aliases.
   *
   * @param source the source value to copy or map
   * @return the mapped immutable case DTO, or {@code null} if the source is {@code null}
   */
  public CaseDto mapToDto(ServiceCase source) {
    if (source == null) {
      return null;
    }
    return CaseDto.builder()
        .id(source.getId())
        .caseNumber(source.getCaseNumber())
        .title(source.getTitle())
        .description(source.getDescription())
        .category(source.getCategory())
        .priority(source.getPriority())
        .status(source.getStatus())
        .createdBy(source.getCreatedBy())
        .assignedTo(source.getAssignedTo())
        .department(source.getDepartment())
        .dueAt(source.getDueAt())
        .closedAt(source.getClosedAt())
        .createdAt(source.getCreatedAt())
        .updatedAt(source.getUpdatedAt())
        .version(source.getVersion())
        .archived(source.isArchived())
        .overdueNoticeSent(source.isOverdueNoticeSent())
        .build();
  }

  /**
   * Maps only writable fields into a new entity; services assign identity, ownership, and lifecycle
   * state.
   *
   * @param source the source value to copy or map
   * @return the mapped service case, or {@code null} if the source is {@code null}
   */
  public ServiceCase mapToEntity(CaseDto source) {
    if (source == null) {
      return null;
    }
    ServiceCase target = new ServiceCase();
    target.setTitle(source.getTitle());
    target.setDescription(source.getDescription());
    target.setCategory(source.getCategory());
    target.setPriority(source.getPriority());
    target.setDepartment(source.getDepartment());
    target.setDueAt(source.getDueAt());
    return target;
  }

  /**
   * Converts query results to an entity-free transport list.
   *
   * @param sources the source values to map
   * @return the mapped immutable values in source order
   */
  public List<CaseDto> mapToDtos(List<ServiceCase> sources) {
    List<CaseDto> targets = new ArrayList<>();
    for (ServiceCase source : sources) {
      targets.add(mapToDto(source));
    }
    return targets;
  }

  /**
   * Applies only editable case fields to a managed entity, preserving identity, ownership,
   * workflow, and version.
   *
   * @param source the source value to copy or map
   * @param target the persistent entity to update
   */
  public void updateEntity(CaseDto source, ServiceCase target) {
    target.setTitle(source.getTitle());
    target.setDescription(source.getDescription());
    target.setPriority(source.getPriority());
    target.setDueAt(source.getDueAt());
  }

  /**
   * Applies service-owned lifecycle changes from a DTO populated from the managed case.
   *
   * @param source the source value to copy or map
   * @param target the persistent entity to update
   */
  public void updateState(CaseDto source, ServiceCase target) {
    target.setAssignedTo(source.getAssignedTo());
    target.setStatus(source.getStatus());
    target.setArchived(source.isArchived());
    target.setUpdatedAt(source.getUpdatedAt());
    target.setClosedAt(source.getClosedAt());
  }
}
