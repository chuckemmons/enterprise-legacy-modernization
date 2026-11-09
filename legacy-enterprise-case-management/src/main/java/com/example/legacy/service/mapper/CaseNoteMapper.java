package com.example.legacy.service.mapper;

import com.example.legacy.model.CaseNote;
import com.example.legacy.service.dto.CaseNoteDto;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts CaseNote entities and CaseNoteDto values without exposing persistence objects to the web
 * tier.
 *
 * @author Chuck Emmons
 */
public class CaseNoteMapper {
  /**
   * Copies an entity to transport data, excluding credentials and mutable-value aliases.
   *
   * @param source the source value to copy or map
   * @return the mapped immutable case note DTO, or {@code null} if the source is {@code null}
   */
  public CaseNoteDto mapToDto(CaseNote source) {
    if (source == null) {
      return null;
    }
    return CaseNoteDto.builder()
        .id(source.getId())
        .caseId(source.getCaseId())
        .author(source.getAuthor())
        .body(source.getBody())
        .createdAt(source.getCreatedAt())
        .build();
  }

  /**
   * Maps only writable fields into a new entity; services assign identity, ownership, and lifecycle
   * state.
   *
   * @param source the source value to copy or map
   * @return the mapped case note, or {@code null} if the source is {@code null}
   */
  public CaseNote mapToEntity(CaseNoteDto source) {
    if (source == null) {
      return null;
    }
    CaseNote target = new CaseNote();
    target.setBody(source.getBody());
    return target;
  }

  /**
   * Converts query results to an entity-free transport list.
   *
   * @param sources the source values to map
   * @return the mapped immutable values in source order
   */
  public List<CaseNoteDto> mapToDtos(List<CaseNote> sources) {
    List<CaseNoteDto> targets = new ArrayList<>();
    for (CaseNote source : sources) {
      targets.add(mapToDto(source));
    }
    return targets;
  }
}
