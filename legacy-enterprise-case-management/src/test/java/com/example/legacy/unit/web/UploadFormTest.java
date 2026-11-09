package com.example.legacy.unit.web;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.legacy.service.dto.DocumentDto;
import com.example.legacy.service.dto.DocumentPageDto;
import com.example.legacy.service.dto.UploadDocumentDto;
import com.example.legacy.support.ValidatorTestSupport;
import com.example.legacy.unit.support.WebTestSupport;
import com.example.legacy.web.UploadForm;
import com.example.legacy.web.ValidationInputAction;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.junit.Before;
import org.junit.Test;

/**
 * Behavioral contracts for UploadForm.
 *
 * @author Chuck Emmons
 */
public class UploadFormTest extends WebTestSupport {

  private UploadForm form;

  /** Creates isolated collaborators and input state for each test. */
  @Before
  public void createForm() {
    form = ValidatorTestSupport.prepareForm(new UploadForm());
    form.setId("7");
    attributes.put(Globals.MODULE_KEY, ValidatorTestSupport.getModuleConfiguration());
  }

  /**
   * Validates metadata without reading attachment bytes.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if the observed behavior violates the tested contract
   */
  @Test
  public void validatesMetadataWithoutReadingAttachmentBytes() throws Exception {
    org.apache.struts.upload.FormFile file = mock(org.apache.struts.upload.FormFile.class);
    form.setFile(file);
    when(file.getFileSize()).thenReturn(1);
    when(file.getContentType()).thenReturn(null);
    assertTrue(form.validate(mapping("upload"), request).isEmpty());
    verify(file, never()).getFileData();
    when(file.getFileSize()).thenReturn(-1);
    when(file.getContentType()).thenReturn("text/html");
    ActionErrors errors = form.validate(mapping("upload"), request);
    assertTrue(errors.get("file").hasNext());
    assertEquals(2, errors.size());
  }

  /**
   * Prioritizes container overflow and missing files.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void prioritizesContainerOverflowAndMissingFiles() {
    attributes.put(
        org.apache.struts.upload.MultipartRequestHandler.ATTRIBUTE_MAX_LENGTH_EXCEEDED,
        Boolean.TRUE);
    assertEquals(1, form.validate(mapping("upload"), request).size());
    attributes.clear();
    attributes.put(Globals.MODULE_KEY, ValidatorTestSupport.getModuleConfiguration());
    assertTrue(form.validate(mapping("upload"), request).get("file").hasNext());
    org.apache.struts.upload.FormFile file = mock(org.apache.struts.upload.FormFile.class);
    form.setFile(file);
    assertTrue(form.validate(mapping("upload"), request).get("file").hasNext());
    ActionMapping input = mapping("upload");
    input.setType(ValidationInputAction.class.getName());
    assertTrue(form.validate(input, request).isEmpty());
    assertTrue(form.validate(mapping("download"), request).isEmpty());
  }

  /**
   * Snapshots download data and materializes upload bytes once.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if the observed behavior violates the tested contract
   */
  @Test
  public void snapshotsDownloadDataAndMaterializesUploadBytesOnce() throws Exception {
    DocumentDto document = DocumentDto.builder().payload(new byte[] {1}).contentSize(1).build();
    form.setData(DocumentPageDto.builder().id(7).document(document).build());
    assertSame(document, form.getDocument());
    assertSame(document, form.getData().getDocument());
    assertEquals(7L, form.getRecordData().getId());
    org.apache.struts.upload.FormFile file = mock(org.apache.struts.upload.FormFile.class);
    when(file.getFileData()).thenReturn(new byte[] {2, 3});
    when(file.getFileSize()).thenReturn(2);
    form.setFile(file);
    UploadDocumentDto command = form.getUploadData();
    assertArrayEquals(new byte[] {2, 3}, command.getDocument().getPayload());
    assertEquals(2, command.getDeclaredSize());
    verify(file, times(1)).getFileData();
  }
}
