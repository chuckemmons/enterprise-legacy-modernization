package com.example.legacy.unit.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.legacy.dao.AuditDao;
import com.example.legacy.domain.AuditAction;
import com.example.legacy.domain.AuditEntity;
import com.example.legacy.model.Account;
import com.example.legacy.model.AuditEvent;
import com.example.legacy.service.AccountAccessService;
import com.example.legacy.service.AuditService;
import com.example.legacy.service.dto.AuditEventDto;
import com.example.legacy.service.dto.PageRequestDto;
import com.example.legacy.service.mapper.AuditEventMapper;
import com.example.legacy.unit.support.ServiceTestSupport;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Behavioral contracts for AuditService.
 *
 * @author Chuck Emmons
 */
public class AuditServiceTest extends ServiceTestSupport {

  private AuditDao audits;
  private AccountAccessService access;
  private AuditService service;

  /** Creates isolated collaborators and input state for each test. */
  @Before
  public void createService() {
    audits = mock(AuditDao.class);
    access = mock(AccountAccessService.class);
    service = new AuditService(template, access, audits, new AuditEventMapper());
  }

  /**
   * Validates and persists an audit with caller owned session.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void validatesAndPersistsAnAuditWithCallerOwnedSession() {
    service.recordAuditEvent(
        session, AuditEntity.CASE, 7L, AuditAction.UPDATE, "worker", "Changed title");
    ArgumentCaptor<AuditEvent> saved = ArgumentCaptor.forClass(AuditEvent.class);
    verify(audits).save(eq(session), saved.capture());
    assertEquals("CASE", saved.getValue().getEntityType());
    assertEquals("UPDATE", saved.getValue().getActionName());
    assertEquals("worker", saved.getValue().getActor());
    assertEquals("Changed title", saved.getValue().getDetails());
    assertNotNull(saved.getValue().getCreatedAt());
    verifyNoInteractions(factory);
  }

  /**
   * Checks reporting permission and maps the requested page.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void checksReportingPermissionAndMapsTheRequestedPage() {
    Account actor = account("auditor", "AUDITOR");
    when(access.requireActiveAccount(session, "auditor")).thenReturn(actor);
    AuditEvent event = new AuditEvent();
    event.setDetails("Recorded");
    when(audits.findRecentAuditEvents(session, 2)).thenReturn(Collections.singletonList(event));
    List<AuditEventDto> result =
        service.listRecentAuditEvents("auditor", PageRequestDto.builder().page(2).build());
    assertEquals("Recorded", result.get(0).getDetails());
    verify(access).requireReportingPermission(actor);
    verify(transaction).commit();
  }
}
