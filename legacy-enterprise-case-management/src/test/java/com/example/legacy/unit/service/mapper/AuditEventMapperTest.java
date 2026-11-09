package com.example.legacy.unit.service.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.legacy.model.AuditEvent;
import com.example.legacy.service.dto.AuditEventDto;
import com.example.legacy.service.mapper.AuditEventMapper;
import java.util.Collections;
import java.util.Date;
import org.junit.Test;

/**
 * Behavioral contracts for AuditEventMapper.
 *
 * @author Chuck Emmons
 */
public class AuditEventMapperTest {

  private final AuditEventMapper mapper = new AuditEventMapper();

  /**
   * Preserves audit content without copying persistence identity.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void preservesAuditContentWithoutCopyingPersistenceIdentity() {
    AuditEventDto source =
        AuditEventDto.builder()
            .id(3L)
            .entityType("CASE")
            .entityId(7L)
            .actionName("UPDATE")
            .actor("worker")
            .details("Changed title")
            .createdAt(new Date(1234))
            .build();
    AuditEvent entity = mapper.mapToEntity(source);
    assertNull(entity.getId());
    AuditEventDto dto = mapper.mapToDtos(Collections.singletonList(entity)).get(0);
    assertEquals("CASE", dto.getEntityType());
    assertEquals(Long.valueOf(7), dto.getEntityId());
    assertEquals("UPDATE", dto.getActionName());
    assertEquals("worker", dto.getActor());
    assertEquals("Changed title", dto.getDetails());
    assertEquals(1234, dto.getCreatedAt().getTime());
    assertNull(mapper.mapToDto(null));
    assertNull(mapper.mapToEntity(null));
    assertTrue(mapper.mapToDtos(Collections.emptyList()).isEmpty());
  }
}
