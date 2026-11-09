package com.example.legacy.unit.web;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.legacy.service.DocumentService;
import com.example.legacy.service.dto.DocumentDto;
import com.example.legacy.service.dto.RecordReferenceDto;
import com.example.legacy.service.dto.UploadDocumentDto;
import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.unit.support.WebTestSupport;
import com.example.legacy.util.MessageKeys;
import com.example.legacy.web.DocumentAction;
import com.example.legacy.web.HttpConstants;
import com.example.legacy.web.PageView;
import com.example.legacy.web.UploadForm;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForward;
import org.junit.Before;
import org.junit.Test;

/**
 * Behavioral contracts for DocumentAction.
 *
 * @author Chuck Emmons
 */
public class DocumentActionTest extends WebTestSupport {

  private DocumentService service;
  private DocumentAction action;
  private UploadForm form;
  private org.apache.struts.upload.FormFile file;

  /**
   * Creates isolated collaborators and input state for each test.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   */
  @Before
  public void createAction() throws Exception {
    service = mock(DocumentService.class);
    action = new DocumentAction(service);
    form = new UploadForm();
    form.setId("7");
    form.setCsrf("valid-token");
    file = mock(org.apache.struts.upload.FormFile.class);
    when(file.getFileData()).thenReturn(new byte[] {1, 2});
    when(file.getFileSize()).thenReturn(2);
    when(file.getFileName()).thenReturn("note.txt");
    when(file.getContentType()).thenReturn("text/plain");
  }

  /**
   * Uploads the command and always destroys the multipart resource.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void uploadsTheCommandAndAlwaysDestroysTheMultipartResource() {
    form.setFile(file);
    ActionForward result = action.execute(mapping("upload"), form, request, response);
    assertTrue(result.getRedirect());
    assertTrue(result.getPath().contains("id=7"));
    org.mockito.ArgumentCaptor<UploadDocumentDto> command =
        org.mockito.ArgumentCaptor.forClass(UploadDocumentDto.class);
    verify(service).uploadDocument(eq("admin"), command.capture());
    assertArrayEquals(new byte[] {1, 2}, command.getValue().getDocument().getPayload());
    verify(file).destroy();
  }

  /**
   * Streams download bytes with sanitized attachment headers.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if the observed behavior violates the tested contract
   */
  @Test
  public void streamsDownloadBytesWithSanitizedAttachmentHeaders() throws Exception {
    DocumentDto document =
        DocumentDto.builder()
            .originalName("../../note.txt")
            .contentType("text/plain")
            .contentSize(2)
            .payload(new byte[] {3, 4})
            .build();
    when(service.downloadDocument(eq("admin"), any(RecordReferenceDto.class))).thenReturn(document);
    assertNull(action.execute(mapping("download"), form, request, response));
    assertArrayEquals(new byte[] {3, 4}, bytes.toByteArray());
    verify(response).setContentLength(2);
    verify(response)
        .setHeader(HttpConstants.CONTENT_DISPOSITION, "attachment; filename=\"note.txt\"");
    verify(response).setHeader(HttpConstants.CONTENT_TYPE_OPTIONS, HttpConstants.NO_SNIFF);
  }

  /**
   * Returns correctable errors and releases rejected uploads.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void returnsCorrectableErrorsAndReleasesRejectedUploads() {
    form.setFile(file);
    doThrow(new BusinessValidationException(MessageKeys.BUSINESS_INVALID_RECORD_ID))
        .when(service)
        .uploadDocument(anyString(), any(UploadDocumentDto.class));
    assertEquals(
        "page.input", action.execute(mapping("upload"), form, request, response).getPath());
    assertTrue(attributes.get(Globals.ERROR_KEY) instanceof ActionErrors);
    verify(file).destroy();
  }

  /**
   * Rejects bad upload tokens before calling the service.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void rejectsBadUploadTokensBeforeCallingTheService() {
    form.setCsrf("wrong");
    form.setFile(file);
    assertEquals(
        PageView.ERROR.getDefinition(),
        action.execute(mapping("upload"), form, request, response).getPath());
    verifyNoInteractions(service);
    verify(file).destroy();
  }
}
