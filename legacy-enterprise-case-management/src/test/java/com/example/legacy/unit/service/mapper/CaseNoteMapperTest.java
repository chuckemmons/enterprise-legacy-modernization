package com.example.legacy.unit.service.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.legacy.model.CaseNote;
import com.example.legacy.service.dto.CaseNoteDto;
import com.example.legacy.service.mapper.CaseNoteMapper;
import java.util.Collections;
import java.util.Date;
import org.junit.Test;

/**
 * Behavioral contracts for CaseNoteMapper.
 *
 * @author Chuck Emmons
 */
public class CaseNoteMapperTest {

  private final CaseNoteMapper mapper = new CaseNoteMapper();

  /**
   * Snapshots notes and maps only writable body.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void snapshotsNotesAndMapsOnlyWritableBody() {
    CaseNote note = new CaseNote();
    note.setId(2L);
    note.setCaseId(7L);
    note.setAuthor("worker");
    note.setBody("A note");
    note.setCreatedAt(new Date(1234));
    CaseNoteDto dto = mapper.mapToDtos(Collections.singletonList(note)).get(0);
    assertEquals(Long.valueOf(2), dto.getId());
    assertEquals(Long.valueOf(7), dto.getCaseId());
    assertEquals("worker", dto.getAuthor());
    assertEquals("A note", dto.getBody());
    assertEquals(1234, dto.getCreatedAt().getTime());
    CaseNote created = mapper.mapToEntity(dto);
    assertEquals("A note", created.getBody());
    assertNull(created.getId());
    assertNull(created.getAuthor());
    assertNull(mapper.mapToDto(null));
    assertNull(mapper.mapToEntity(null));
    assertTrue(mapper.mapToDtos(Collections.emptyList()).isEmpty());
  }
}
