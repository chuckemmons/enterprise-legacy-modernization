package com.example.legacy.web;

import com.example.legacy.service.dto.DocumentDto;
import com.example.legacy.service.dto.DocumentPageDto;
import com.example.legacy.service.dto.RecordReferenceDto;
import com.example.legacy.service.dto.UploadDocumentDto;
import com.example.legacy.util.ContentTypes;
import com.example.legacy.util.MessageKeys;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.upload.FormFile;

/**
 * Request-scoped multipart and download adapter; Struts file objects never enter the service layer.
 *
 * @author Chuck Emmons
 */
public final class UploadForm extends BasePageForm {
  private static final long serialVersionUID = 1L;
  private FormFile file;
  private DocumentPageDto data = DocumentPageDto.builder().build();

  /**
   * Returns the multipart resource, which the action releases in a finally block.
   *
   * @return the multipart resource, which the action releases in a finally block
   */
  public FormFile getFile() {
    return file;
  }

  /**
   * Binds the Struts multipart resource.
   *
   * @param value the file to set
   */
  public void setFile(FormFile value) {
    file = value;
  }

  /**
   * Returns downloaded attachment transport data.
   *
   * @return downloaded attachment transport data
   */
  public DocumentDto getDocument() {
    return data.getDocument();
  }

  /**
   * Copies bound metadata and bytes into a DTO without exposing the multipart implementation.
   *
   * @return the document page DTO
   * @throws IOException if an I/O operation fails
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   */
  public DocumentPageDto getData() throws IOException {
    DocumentPageDto.Builder result = getPageData(DocumentPageDto.builder());
    if (file != null) {
      DocumentDto document =
          DocumentDto.builder()
              .originalName(file.getFileName())
              .contentType(file.getContentType())
              .payload(file.getFileData())
              .build();
      result.declaredSize(file.getFileSize()).document(document);
    } else {
      result.document(data.getDocument());
    }
    return result.build();
  }

  /**
   * Copies a document service result into the form for download response handling.
   *
   * @param source the source value to copy or map
   * @throws NullPointerException if {@code source} is {@code null}
   */
  public void setData(DocumentPageDto source) {
    data = source;
    setPageData(source);
  }

  /**
   * Validates multipart display metadata without reading bytes or querying persistence.
   *
   * @param mapping the Struts mapping for the current operation
   * @param request the current HTTP request
   * @return the validation errors, or an empty collection when the submitted values are valid
   * @throws NullPointerException if {@code mapping} or {@code request} is {@code null}
   */
  @Override
  public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
    ActionErrors errors = super.validate(mapping, request);
    if (isInputForward(mapping)
        || !ActionOperation.UPLOAD.matchesParameter(mapping.getParameter())) {
      return errors;
    }
    if (Boolean.TRUE.equals(
        request.getAttribute(
            org.apache.struts.upload.MultipartRequestHandler.ATTRIBUTE_MAX_LENGTH_EXCEEDED))) {
      errors.add(FormFields.FILE, new ActionMessage(MessageKeys.VALIDATION_FILE_SIZE));
      return errors;
    }
    if (file == null || file.getFileSize() == 0) {
      errors.add(FormFields.FILE, new ActionMessage(MessageKeys.VALIDATION_FILE_REQUIRED));
      return errors;
    }
    if (file.getFileSize() < 0
        || file.getFileSize() > com.example.legacy.util.ValidationLimits.ATTACHMENT_BYTES) {
      errors.add(FormFields.FILE, new ActionMessage(MessageKeys.VALIDATION_FILE_SIZE));
    }
    String type = file.getContentType() == null ? ContentTypes.BINARY : file.getContentType();
    if (!ContentTypes.isSupportedAttachment(type)) {
      errors.add(FormFields.FILE, new ActionMessage(MessageKeys.VALIDATION_FILE_TYPE));
    }
    return errors;
  }

  /**
   * Maps only the controls required by the getRecordData application operation.
   *
   * @return the record reference DTO
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   */
  public RecordReferenceDto getRecordData() {
    return RecordReferenceDto.builder().id(FormDataUtil.parseRecordId(getId())).build();
  }

  /**
   * Materializes multipart content once and returns its immutable upload command.
   *
   * @return the upload document DTO
   * @throws java.io.IOException if an I/O operation fails
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   */
  public UploadDocumentDto getUploadData() throws java.io.IOException {
    DocumentPageDto snapshot = getData();
    return UploadDocumentDto.builder()
        .caseId(snapshot.getId())
        .document(snapshot.getDocument())
        .declaredSize(snapshot.getDeclaredSize())
        .build();
  }
}
