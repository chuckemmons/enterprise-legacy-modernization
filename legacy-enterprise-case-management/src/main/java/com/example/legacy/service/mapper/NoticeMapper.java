package com.example.legacy.service.mapper;

import com.example.legacy.model.CaseNotice;
import com.example.legacy.service.dto.NoticeDto;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts CaseNotice entities and NoticeDto values without exposing persistence objects to the web
 * tier.
 *
 * @author Chuck Emmons
 */
public class NoticeMapper {
  /**
   * Copies an entity to transport data, excluding credentials and mutable-value aliases.
   *
   * @param source the source value to copy or map
   * @return the mapped immutable notice DTO, or {@code null} if the source is {@code null}
   */
  public NoticeDto mapToDto(CaseNotice source) {
    if (source == null) {
      return null;
    }
    return NoticeDto.builder()
        .id(source.getId())
        .recipient(source.getRecipient())
        .subject(source.getSubject())
        .message(source.getMessage())
        .caseId(source.getCaseId())
        .read(source.isRead())
        .sentAt(source.getSentAt())
        .createdAt(source.getCreatedAt())
        .build();
  }

  /**
   * Maps only writable fields into a new entity; services assign identity, ownership, and lifecycle
   * state.
   *
   * @param source the source value to copy or map
   * @return the mapped case notice, or {@code null} if the source is {@code null}
   */
  public CaseNotice mapToEntity(NoticeDto source) {
    if (source == null) {
      return null;
    }
    CaseNotice target = new CaseNotice();
    target.setRecipient(source.getRecipient());
    target.setSubject(source.getSubject());
    target.setMessage(source.getMessage());
    target.setCaseId(source.getCaseId());
    target.setCreatedAt(source.getCreatedAt());
    return target;
  }

  /**
   * Converts query results to an entity-free transport list.
   *
   * @param sources the source values to map
   * @return the mapped immutable values in source order
   * @throws NullPointerException if {@code sources} is {@code null}
   */
  public List<NoticeDto> mapToDtos(List<CaseNotice> sources) {
    List<NoticeDto> targets = new ArrayList<>();
    for (CaseNotice source : sources) {
      targets.add(mapToDto(source));
    }
    return targets;
  }

  /**
   * Applies an authorized read flag without changing recipient, message, or delivery state.
   *
   * @param source the source value to copy or map
   * @param target the persistent entity to update
   * @throws NullPointerException if {@code source} or {@code target} is {@code null}
   */
  public void updateRead(NoticeDto source, CaseNotice target) {
    target.setRead(source.isRead());
  }
}
