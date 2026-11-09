package com.example.legacy.unit.service.mapper;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.legacy.model.CaseDocument;
import com.example.legacy.service.dto.DocumentDto;
import com.example.legacy.service.mapper.DocumentMapper;
import java.util.Collections;
import java.util.Date;
import org.junit.Test;

/**
 * Behavioral contracts for DocumentMapper.
 *
 * @author Chuck Emmons
 */
public class DocumentMapperTest {

  private final DocumentMapper mapper = new DocumentMapper();

  /**
   * Distinguishes download payload from metadata only lists.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void distinguishesDownloadPayloadFromMetadataOnlyLists() {
    CaseDocument source = new CaseDocument();
    source.setId(9L);
    source.setCaseId(7L);
    source.setOriginalName("note.txt");
    source.setContentType("text/plain");
    source.setContentSize(2);
    source.setPayload(new byte[] {1, 2});
    source.setUploadedBy("worker");
    source.setCreatedAt(new Date(1234));
    DocumentDto download = mapper.mapToDtos(Collections.singletonList(source)).get(0);
    DocumentDto metadata = mapper.mapToMetadataDtos(Collections.singletonList(source)).get(0);
    assertArrayEquals(new byte[] {1, 2}, download.getPayload());
    assertNull(metadata.getPayload());
    assertEquals(Long.valueOf(9), metadata.getId());
    assertEquals(Long.valueOf(7), metadata.getCaseId());
    assertEquals(2, metadata.getContentSize());
    assertEquals("worker", metadata.getUploadedBy());
    assertEquals("note.txt", metadata.getOriginalName());
    source.getPayload()[0] = 5;
    assertEquals(1, download.getPayload()[0]);
    CaseDocument created = mapper.mapToEntity(download);
    assertNull(created.getId());
    assertNull(created.getUploadedBy());
    assertEquals("text/plain", created.getContentType());
    assertArrayEquals(new byte[] {1, 2}, created.getPayload());
    assertNull(mapper.mapToDto(null));
    assertNull(mapper.mapToEntity(null));
    assertTrue(mapper.mapToDtos(Collections.emptyList()).isEmpty());
    assertTrue(mapper.mapToMetadataDtos(Collections.emptyList()).isEmpty());
  }
}
