package com.example.legacy.unit.web;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.example.legacy.model.Account;
import com.example.legacy.model.CaseDocument;
import com.example.legacy.model.CaseNote;
import com.example.legacy.model.ServiceCase;
import com.example.legacy.service.dto.AccountDto;
import com.example.legacy.service.dto.AuditEventDto;
import com.example.legacy.service.dto.AuditPageDto;
import com.example.legacy.service.dto.AuthDto;
import com.example.legacy.service.dto.CaseDetails;
import com.example.legacy.service.dto.CaseDto;
import com.example.legacy.service.dto.CaseNoteDto;
import com.example.legacy.service.dto.CasePageDto;
import com.example.legacy.service.dto.CaseSummary;
import com.example.legacy.service.dto.CountDto;
import com.example.legacy.service.dto.DocumentDto;
import com.example.legacy.service.dto.DocumentPageDto;
import com.example.legacy.service.dto.NoticeDto;
import com.example.legacy.service.dto.NotificationPageDto;
import com.example.legacy.service.dto.ReportPageDto;
import com.example.legacy.service.dto.UserPageDto;
import com.example.legacy.service.mapper.AccountMapper;
import com.example.legacy.service.mapper.AuditEventMapper;
import com.example.legacy.service.mapper.CaseMapper;
import com.example.legacy.service.mapper.CaseNoteMapper;
import com.example.legacy.service.mapper.DocumentMapper;
import com.example.legacy.service.mapper.NoticeMapper;
import com.example.legacy.web.AuditForm;
import com.example.legacy.web.AuthForm;
import com.example.legacy.web.CaseForm;
import com.example.legacy.web.NotificationForm;
import com.example.legacy.web.ReportForm;
import com.example.legacy.web.UploadForm;
import com.example.legacy.web.UserForm;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.struts.upload.FormFile;
import org.junit.Test;

/**
 * Regression checks for actual Struts bean binding, DTO form round trips, and safe entity mapping.
 *
 * @author Chuck Emmons
 */
public class FormDtoTest {

  /**
   * Exercises the same bean-property population mechanism used by Struts for case request fields.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void bindsCaseFieldsThroughBeanProperties() throws Exception {
    final CaseForm form = new CaseForm();
    Map<String, String> fields = new HashMap<>();
    fields.put("id", "42");
    fields.put("version", "3");
    fields.put("pageNumber", "2");
    fields.put("title", "Bound title");
    fields.put("description", "Bound description");
    fields.put("category", "GENERAL");
    fields.put("priority", "HIGH");
    fields.put("department", "Operations");
    fields.put("due", "2026-11-05");
    fields.put("q", "case search");
    fields.put("status", "TRIAGED");
    fields.put("assignee", "worker");
    fields.put("destination", "IN_PROGRESS");
    fields.put("body", "A note");
    fields.put("csrf", "session-token");
    BeanUtils.populate(form, fields);
    CasePageDto input = form.getData();
    assertTrue(
        "typed common fields",
        input.getId() == 42 && input.getPage() == 2 && input.getItem().getVersion() == 3);
    assertTrue(
        "case input fields",
        "Bound title".equals(input.getItem().getTitle()) && "case search".equals(input.getQuery()));
    assertTrue(
        "workflow form fields",
        "worker".equals(input.getAssignee()) && "IN_PROGRESS".equals(input.getDestination()));
    assertTrue(
        "note and search fields",
        "A note".equals(input.getBody()) && "TRIAGED".equals(input.getStatus()));
    assertTrue(
        "server fields never bound into case input",
        input.getItem().getCreatedBy() == null && input.getItem().getStatus() == null);

    CaseDto item = input.getItem();
    item = CaseDto.builder(item).caseNumber("CASE-000042").build();
    item = CaseDto.builder(item).createdBy("creator").build();
    item = CaseDto.builder(item).status("TRIAGED").build();
    input = CasePageDto.builder(input).item(item).cases(Collections.singletonList(item)).build();
    input =
        CasePageDto.builder(input)
            .details(
                CaseDetails.builder()
                    .item(item)
                    .notes(Collections.emptyList())
                    .documents(Collections.emptyList())
                    .users(Collections.emptyList())
                    .nextStates(new LinkedHashSet<>(Collections.singletonList("IN_PROGRESS")))
                    .mayEdit(true)
                    .mayComment(true)
                    .build())
            .build();
    CaseForm loaded = new CaseForm();
    loaded.setData(input);
    assertTrue(
        "date/version formatted for controls",
        "2026-11-05".equals(loaded.getDue()) && "3".equals(loaded.getVersion()));
    assertTrue(
        "case form round trip",
        loaded.getData().getId() == 42
            && loaded.getData().getItem().getDueAt().equals(item.getDueAt()));
    assertTrue(
        "response DTO graph available to JSP",
        loaded.getItem() == item
            && loaded.getCases().size() == 1
            && loaded.getDetails().isMayEdit());
    assertTrue("DTO cannot overwrite session CSRF state", loaded.getCsrf() == null);
    assertOperationRejected(() -> {
      form.setId("invalid");
      form.getData();
    });
    assertOperationRejected(() -> {
      form.setId("-1");
      form.getData();
    });
    form.setId("42");
    assertOperationRejected(() -> {
      form.setDue("2026-02-30");
      form.getData();
    });
    form.setDue(null);
    assertOperationRejected(() -> {
      form.setVersion("bad");
      form.getData();
    });
    form.setVersion(null);
    form.setPageNumber("not-a-number");
    assertTrue("malformed pagination defaults safely", form.getData().getPage() == 0);
  }

  /**
   * Checks credential handling, user-field aliases, and read-only page form result graphs.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void mapsPageFormsAndPreservesCredentialHandling() throws Exception {
    AuthForm auth = new AuthForm();
    Map<String, String> login = new HashMap<>();
    login.put("username", "worker");
    login.put("password", "InitialSecret!2026");
    BeanUtils.populate(auth, login);
    assertTrue(
        "credentials pass only as input DTO data",
        "InitialSecret!2026".equals(auth.getData().getPassword()));
    AuthDto result =
        AuthDto.builder().username("worker").account(AccountDto.builder().build()).build();
    auth.setData(result);
    assertTrue(
        "credentials cleared when setting response data",
        auth.getPassword() == null && auth.getData().getPassword() == null);
    final UserForm user = new UserForm();
    Map<String, String> account = new HashMap<>();
    account.put("name", "Display Name");
    account.put("role", "WORKER");
    account.put("username", "worker");
    account.put("email", "worker@example.test");
    account.put("department", "Operations");
    account.put("password", "InitialSecret!2026");
    BeanUtils.populate(user, account);
    UserPageDto userData = user.getData();
    assertTrue(
        "HTML name field maps to account displayName",
        "Display Name".equals(userData.getAccount().getDisplayName()));
    userData =
        UserPageDto.builder(userData)
            .users(Collections.singletonList(userData.getAccount()))
            .build();
    user.setData(userData);
    assertTrue(
        "user results and password clearing",
        user.getUsers().size() == 1 && user.getPassword() == null);
    AuditForm audit = new AuditForm();
    audit.setPageNumber("4");
    AuditPageDto auditData = audit.getData();
    auditData =
        AuditPageDto.builder(auditData)
            .events(Collections.singletonList(AuditEventDto.builder().build()))
            .build();
    audit.setData(auditData);
    assertTrue(
        "audit pagination and output",
        audit.getData().getPage() == 4 && audit.getEvents().size() == 1);
    NotificationForm notices = new NotificationForm();
    notices.setId("7");
    NotificationPageDto noticeData = notices.getData();
    noticeData =
        NotificationPageDto.builder(noticeData)
            .notices(Collections.singletonList(NoticeDto.builder().build()))
            .build();
    notices.setData(noticeData);
    assertTrue(
        "notification read ID and listing",
        notices.getData().getId() == 7 && notices.getNotices().size() == 1);
    ReportPageDto reportData =
        ReportPageDto.builder()
            .summary(
                CaseSummary.builder()
                    .statuses(
                        Collections.singletonList(CountDto.builder().label("NEW").total(4).build()))
                    .priorities(Collections.emptyList())
                    .overdue(2L)
                    .build())
            .cases(Collections.singletonList(CaseDto.builder().build()))
            .build();
    ReportForm report = new ReportForm();
    report.setData(reportData);
    assertTrue(
        "typed reports and CSV DTO rows",
        report.getSummary().getStatuses().get(0).getTotal() == 4 && report.getCases().size() == 1);
  }

  /**
   * Verifies editable-field whitelists, immutable identity/state, and detached copies of mutable
   * values.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void mapsEditableFieldsAndCopiesMutableValues() throws Exception {
    final AccountMapper accountMapper = new AccountMapper();
    final AuditEventMapper auditEventMapper = new AuditEventMapper();
    CaseMapper caseMapper = new CaseMapper();
    final CaseNoteMapper caseNoteMapper = new CaseNoteMapper();
    final DocumentMapper documentMapper = new DocumentMapper();
    final NoticeMapper noticeMapper = new NoticeMapper();

    ServiceCase entity = new ServiceCase();
    entity.setId(42L);
    entity.setVersion(3);
    entity.setCaseNumber("CASE-000042");
    entity.setCreatedBy("creator");
    entity.setAssignedTo("worker");
    entity.setStatus("IN_PROGRESS");
    entity.setTitle("Existing title");
    entity.setDueAt(new Date(1000));
    CaseDto dto = caseMapper.mapToDto(entity);
    dto.getDueAt().setTime(2000);
    assertTrue("mapped DTO dates do not alias entity dates", entity.getDueAt().getTime() == 1000);
    dto = CaseDto.builder(dto).id(999L).build();
    dto = CaseDto.builder(dto).version(999).build();
    dto = CaseDto.builder(dto).createdBy("attacker").build();
    dto = CaseDto.builder(dto).status("CLOSED").build();
    dto = CaseDto.builder(dto).title("Updated title").build();
    ServiceCase created = caseMapper.mapToEntity(dto);
    assertTrue(
        "new entity ignores submitted identity, lock version, and lifecycle ownership",
        created.getId() == null
            && created.getVersion() == null
            && created.getCreatedBy() == null
            && created.getStatus() == null);
    caseMapper.updateEntity(dto, entity);
    assertTrue(
        "update mapping preserves managed identity, version, ownership, and workflow",
        entity.getId() == 42
            && entity.getVersion() == 3
            && "creator".equals(entity.getCreatedBy())
            && "IN_PROGRESS".equals(entity.getStatus()));
    assertTrue("editable fields mapped", "Updated title".equals(entity.getTitle()));
    Account account = new Account();
    account.setId(9L);
    account.setUsername("worker");
    account.setPasswordHash("never-expose-this");
    account.setEnabled(true);
    AccountDto accountDto = accountMapper.mapToDto(account);
    Map<?, ?> properties = BeanUtils.describe(accountDto);
    assertTrue(
        "account DTO omits password credentials",
        !properties.containsKey("passwordHash") && !properties.containsKey("password"));
    assertTrue(
        "account DTO cannot choose a generated identity",
        accountMapper.mapToEntity(accountDto).getId() == null);
    CaseDocument attachment = new CaseDocument();
    attachment.setId(5L);
    attachment.setPayload(new byte[] {1, 2});
    DocumentDto document = documentMapper.mapToDto(attachment);
    document.getPayload()[0] = 9;
    assertTrue("download bytes are detached from entity bytes", attachment.getPayload()[0] == 1);
    CaseDocument stored = documentMapper.mapToEntity(document);
    stored.getPayload()[1] = 8;
    assertTrue(
        "upload bytes and generated identity are protected",
        document.getPayload()[1] == 2 && stored.getId() == null);
    assertTrue(
        "attachment lists omit binary payloads",
        documentMapper.mapToMetadataDto(attachment).getPayload() == null);
    CaseNoteDto note =
        CaseNoteDto.builder().id(9L).caseId(999L).author("attacker").body("note").build();
    CaseNote mappedNote = caseNoteMapper.mapToEntity(note);
    assertTrue(
        "note ownership assigned by service",
        mappedNote.getId() == null
            && mappedNote.getCaseId() == null
            && mappedNote.getAuthor() == null
            && "note".equals(mappedNote.getBody()));
    AuditEventDto audit = AuditEventDto.builder().details("details").build();
    assertTrue(
        "audit mapping round trip",
        "details"
            .equals(auditEventMapper.mapToDto(auditEventMapper.mapToEntity(audit)).getDetails()));
    NoticeDto notice = NoticeDto.builder().subject("subject").build();
    assertTrue(
        "notification mapping round trip",
        "subject".equals(noticeMapper.mapToDto(noticeMapper.mapToEntity(notice)).getSubject()));
    assertTrue(
        "nullable entity mapping",
        caseMapper.mapToDto(null) == null && accountMapper.mapToDto(null) == null);
  }

  /**
   * Tests multipart DTO conversion against the actual Struts FormFile contract.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void mapsMultipartFormFileToDocumentInput() throws Exception {
    FormFile file = org.mockito.Mockito.mock(FormFile.class);
    org.mockito.Mockito.when(file.getFileName()).thenReturn("test.txt");
    org.mockito.Mockito.when(file.getContentType()).thenReturn("text/plain");
    org.mockito.Mockito.when(file.getFileSize()).thenReturn(2);
    org.mockito.Mockito.when(file.getFileData()).thenReturn(new byte[] {1, 2});
    UploadForm form = new UploadForm();
    form.setId("42");
    form.setFile(file);
    DocumentPageDto data = form.getData();
    assertTrue(
        "multipart metadata maps to DTO",
        data.getId() == 42
            && data.getDeclaredSize() == 2
            && "test.txt".equals(data.getDocument().getOriginalName()));
    assertTrue("multipart bytes map to DTO", data.getDocument().getPayload().length == 2);
    UploadForm download = new UploadForm();
    download.setData(data);
    assertTrue(
        "download form needs no Struts upload object",
        download.getFile() == null && download.getData().getDocument() == data.getDocument());
  }

  /**
   * Uses JUnit to verify malformed controls fail explicitly.
   *
   * @param operation the operation expected to fail
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  private static void assertOperationRejected(Runnable operation) {
    assertThrows(
        com.example.legacy.service.exception.BusinessValidationException.class, operation::run);
  }
}
