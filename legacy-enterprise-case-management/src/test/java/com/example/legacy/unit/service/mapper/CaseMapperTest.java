package com.example.legacy.unit.service.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.legacy.model.ServiceCase;
import com.example.legacy.service.dto.CaseDto;
import com.example.legacy.service.mapper.CaseMapper;
import java.util.Collections;
import java.util.Date;
import org.junit.Test;

/**
 * Behavioral contracts for CaseMapper.
 *
 * @author Chuck Emmons
 */
public class CaseMapperTest {

  private final CaseMapper mapper = new CaseMapper();

  /**
   * Maps writable fields without accepting client lifecycle state.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void mapsWritableFieldsWithoutAcceptingClientLifecycleState() {
    CaseDto source =
        CaseDto.builder()
            .id(55L)
            .title("Title")
            .description("Description")
            .category("ACCESS")
            .priority("HIGH")
            .department("Operations")
            .dueAt(new Date(1234))
            .status("CLOSED")
            .createdBy("forged")
            .build();
    ServiceCase entity = mapper.mapToEntity(source);
    assertEquals("Title", entity.getTitle());
    assertEquals("Description", entity.getDescription());
    assertEquals("ACCESS", entity.getCategory());
    assertEquals("HIGH", entity.getPriority());
    assertEquals(1234, entity.getDueAt().getTime());
    assertNull(entity.getId());
    assertNull(entity.getCreatedBy());
    entity.setId(7L);
    entity.setCreatedBy("creator");
    entity.setAssignedTo("worker");
    entity.setStatus("NEW");
    entity.setVersion(3);
    entity.setCreatedAt(new Date(2000));
    entity.setUpdatedAt(new Date(3000));
    CaseDto dto = mapper.mapToDtos(Collections.singletonList(entity)).get(0);
    assertEquals(Long.valueOf(7), dto.getId());
    assertEquals("creator", dto.getCreatedBy());
    assertEquals(Integer.valueOf(3), dto.getVersion());
    assertNull(mapper.mapToDto(null));
    assertNull(mapper.mapToEntity(null));
    assertTrue(mapper.mapToDtos(Collections.emptyList()).isEmpty());
  }

  /**
   * Separates editable fields from workflow state.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void separatesEditableFieldsFromWorkflowState() {
    ServiceCase target = new ServiceCase();
    target.setId(8L);
    target.setCreatedBy("owner");
    target.setStatus("NEW");
    target.setCategory("ORIGINAL");
    CaseDto changes =
        CaseDto.builder()
            .title("Updated")
            .description("New text")
            .priority("LOW")
            .dueAt(new Date(1000))
            .assignedTo("worker")
            .status("CLOSED")
            .archived(true)
            .closedAt(new Date(2000))
            .updatedAt(new Date(3000))
            .build();
    mapper.updateEntity(changes, target);
    assertEquals("Updated", target.getTitle());
    assertEquals("ORIGINAL", target.getCategory());
    assertEquals("NEW", target.getStatus());
    mapper.updateState(changes, target);
    assertEquals("CLOSED", target.getStatus());
    assertEquals("worker", target.getAssignedTo());
    assertTrue(target.isArchived());
    assertEquals(2000, target.getClosedAt().getTime());
    assertEquals("owner", target.getCreatedBy());
    assertEquals(Long.valueOf(8), target.getId());
  }
}
