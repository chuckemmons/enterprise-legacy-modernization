package com.example.legacy.service.mapper;

import com.example.legacy.model.CaseDocument;
import com.example.legacy.service.dto.DocumentDto;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts CaseDocument entities and DocumentDto values without exposing persistence objects to the
 * web tier.
 *
 * @author Chuck Emmons
 */
public class DocumentMapper {
  /**
   * Copies an entity to transport data, excluding credentials and mutable-value aliases.
   *
   * @param source the source value to copy or map
   * @return the mapped immutable document DTO, or {@code null} if the source is {@code null}
   */
  public DocumentDto mapToDto(CaseDocument source) {
    if (source == null) {
      return null;
    }
    return DocumentDto.builder()
        .id(source.getId())
        .caseId(source.getCaseId())
        .originalName(source.getOriginalName())
        .contentType(source.getContentType())
        .contentSize(source.getContentSize())
        .payload(source.getPayload())
        .uploadedBy(source.getUploadedBy())
        .createdAt(source.getCreatedAt())
        .build();
  }

  /**
   * Maps only writable fields into a new entity; services assign identity, ownership, and lifecycle
   * state.
   *
   * @param source the source value to copy or map
   * @return the mapped case document, or {@code null} if the source is {@code null}
   */
  public CaseDocument mapToEntity(DocumentDto source) {
    if (source == null) {
      return null;
    }
    CaseDocument target = new CaseDocument();
    target.setOriginalName(source.getOriginalName());
    target.setContentType(source.getContentType());
    target.setContentSize(source.getContentSize());
    target.setPayload(source.getPayload());
    return target;
  }

  /**
   * Converts query results to an entity-free transport list.
   *
   * @param sources the source values to map
   * @return the mapped immutable values in source order
   * @throws NullPointerException if {@code sources} is {@code null}
   */
  public List<DocumentDto> mapToDtos(List<CaseDocument> sources) {
    List<DocumentDto> targets = new ArrayList<>();
    for (CaseDocument source : sources) {
      targets.add(mapToDto(source));
    }
    return targets;
  }

  /**
   * Maps attachment list metadata without transferring the stored binary payload to a page form.
   *
   * @param source the source value to copy or map
   * @return the document DTO
   * @throws NullPointerException if {@code source} is {@code null}
   */
  public DocumentDto mapToMetadataDto(CaseDocument source) {
    return DocumentDto.builder()
        .id(source.getId())
        .caseId(source.getCaseId())
        .originalName(source.getOriginalName())
        .contentType(source.getContentType())
        .contentSize(source.getContentSize())
        .uploadedBy(source.getUploadedBy())
        .createdAt(source.getCreatedAt())
        .build();
  }

  /**
   * Converts attachment metadata for a case-detail result without copying file contents.
   *
   * @param sources the source values to map
   * @return the matching document DTO values
   * @throws NullPointerException if {@code sources} is {@code null}
   */
  public List<DocumentDto> mapToMetadataDtos(List<CaseDocument> sources) {
    List<DocumentDto> targets = new ArrayList<>();
    for (CaseDocument source : sources) {
      targets.add(mapToMetadataDto(source));
    }
    return targets;
  }
}
