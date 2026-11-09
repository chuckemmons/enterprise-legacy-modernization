package com.example.legacy.unit.service.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.legacy.model.CaseNotice;
import com.example.legacy.service.dto.NoticeDto;
import com.example.legacy.service.mapper.NoticeMapper;
import java.util.Collections;
import java.util.Date;
import org.junit.Test;

/**
 * Behavioral contracts for NoticeMapper.
 *
 * @author Chuck Emmons
 */
public class NoticeMapperTest {

  private final NoticeMapper mapper = new NoticeMapper();

  /**
   * Maps notice state but only updates the read flag.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void mapsNoticeStateButOnlyUpdatesTheReadFlag() {
    CaseNotice notice = new CaseNotice();
    notice.setId(3L);
    notice.setCaseId(7L);
    notice.setRecipient("worker");
    notice.setSubject("Subject");
    notice.setMessage("Body");
    notice.setRead(false);
    notice.setCreatedAt(new Date(1000));
    notice.setSentAt(new Date(2000));
    NoticeDto dto = mapper.mapToDtos(Collections.singletonList(notice)).get(0);
    assertEquals("Subject", dto.getSubject());
    assertEquals("Body", dto.getMessage());
    assertEquals(2000, dto.getSentAt().getTime());
    CaseNotice created = mapper.mapToEntity(dto);
    assertEquals("worker", created.getRecipient());
    assertNull(created.getId());
    assertNull(created.getSentAt());
    mapper.updateRead(NoticeDto.builder(dto).read(true).recipient("ignored").build(), notice);
    assertTrue(notice.isRead());
    assertEquals("worker", notice.getRecipient());
    assertNull(mapper.mapToDto(null));
    assertNull(mapper.mapToEntity(null));
    assertTrue(mapper.mapToDtos(Collections.emptyList()).isEmpty());
  }
}
