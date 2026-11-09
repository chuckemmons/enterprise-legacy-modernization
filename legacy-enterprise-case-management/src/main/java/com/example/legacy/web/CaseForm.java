package com.example.legacy.web;

import com.example.legacy.domain.Priority;
import com.example.legacy.service.dto.AddCaseCommentDto;
import com.example.legacy.service.dto.AssignCaseDto;
import com.example.legacy.service.dto.CaseDetails;
import com.example.legacy.service.dto.CaseDto;
import com.example.legacy.service.dto.CasePageDto;
import com.example.legacy.service.dto.CaseSearchCriteriaDto;
import com.example.legacy.service.dto.CreateCaseDto;
import com.example.legacy.service.dto.RecordReferenceDto;
import com.example.legacy.service.dto.TransitionCaseDto;
import com.example.legacy.service.dto.UpdateCaseDto;
import com.example.legacy.util.MessageKeys;
import com.example.legacy.util.MessagesUtil;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;

/**
 * Request-scoped Struts adapter for CasePageDto input and output.
 *
 * @author Chuck Emmons
 */
public final class CaseForm extends BasePageForm {
  private static final long serialVersionUID = 1L;
  private CasePageDto data = CasePageDto.builder().build();
  private String title;
  private String description;
  private String category;
  private String priority;
  private String department;
  private String due;
  private String version;
  private String query;
  private String status;
  private String assignee;
  private String destination;
  private String body;

  /**
   * Returns the bound title field.
   *
   * @return the bound title field
   */
  public String getTitle() {
    return title;
  }

  /**
   * Binds the submitted title field.
   *
   * @param value the title to set
   */
  public void setTitle(String value) {
    title = value;
  }

  /**
   * Returns the bound description field.
   *
   * @return the bound description field
   */
  public String getDescription() {
    return description;
  }

  /**
   * Binds the submitted description field.
   *
   * @param value the description to set
   */
  public void setDescription(String value) {
    description = value;
  }

  /**
   * Returns the bound category field.
   *
   * @return the bound category field
   */
  public String getCategory() {
    return category;
  }

  /**
   * Binds the submitted category field.
   *
   * @param value the category to set
   */
  public void setCategory(String value) {
    category = value;
  }

  /**
   * Returns the bound priority field.
   *
   * @return the bound priority field
   */
  public String getPriority() {
    return priority;
  }

  /**
   * Binds the submitted priority field.
   *
   * @param value the priority to set
   */
  public void setPriority(String value) {
    priority = value;
  }

  /**
   * Returns the bound department field.
   *
   * @return the bound department field
   */
  public String getDepartment() {
    return department;
  }

  /**
   * Binds the submitted department field.
   *
   * @param value the department to set
   */
  public void setDepartment(String value) {
    department = value;
  }

  /**
   * Returns the bound due field.
   *
   * @return the bound due field
   */
  public String getDue() {
    return due;
  }

  /**
   * Binds the submitted due field.
   *
   * @param value the due to set
   */
  public void setDue(String value) {
    due = value;
  }

  /**
   * Returns the bound version field.
   *
   * @return the bound version field
   */
  public String getVersion() {
    return version;
  }

  /**
   * Binds the submitted version field.
   *
   * @param value the optimistic-lock version to set
   */
  public void setVersion(String value) {
    version = value;
  }

  /**
   * Returns the bound q field.
   *
   * @return the bound q field
   */
  public String getQ() {
    return query;
  }

  /**
   * Binds the submitted q field.
   *
   * @param value the q to set
   */
  public void setQ(String value) {
    query = value;
  }

  /**
   * Returns the bound status field.
   *
   * @return the bound status field
   */
  public String getStatus() {
    return status;
  }

  /**
   * Binds the submitted status field.
   *
   * @param value the status to set
   */
  public void setStatus(String value) {
    status = value;
  }

  /**
   * Returns the bound assignee field.
   *
   * @return the bound assignee field
   */
  public String getAssignee() {
    return assignee;
  }

  /**
   * Binds the submitted assignee field.
   *
   * @param value the assignee to set
   */
  public void setAssignee(String value) {
    assignee = value;
  }

  /**
   * Returns the bound destination field.
   *
   * @return the bound destination field
   */
  public String getDestination() {
    return destination;
  }

  /**
   * Binds the submitted destination field.
   *
   * @param value the destination to set
   */
  public void setDestination(String value) {
    destination = value;
  }

  /**
   * Returns the bound body field.
   *
   * @return the bound body field
   */
  public String getBody() {
    return body;
  }

  /**
   * Binds the submitted body field.
   *
   * @param value the body to set
   */
  public void setBody(String value) {
    body = value;
  }

  /**
   * Returns item loaded from a service DTO.
   *
   * @return item loaded from a service DTO
   */
  public CaseDto getItem() {
    return data.getItem();
  }

  /**
   * Returns cases loaded from a service DTO.
   *
   * @return cases loaded from a service DTO
   */
  public List<CaseDto> getCases() {
    return data.getCases();
  }

  /**
   * Returns details loaded from a service DTO.
   *
   * @return details loaded from a service DTO
   */
  public CaseDetails getDetails() {
    return data.getDetails();
  }

  /**
   * Creates a service DTO from bound fields; never passes the ActionForm to a service.
   *
   * @return the case page DTO
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   */
  public CasePageDto getData() {
    long id = FormDataUtil.parseRecordId(getId());
    CaseDto item =
        CaseDto.builder()
            .id(id == 0 ? null : id)
            .title(title)
            .description(description)
            .category(category)
            .priority(priority)
            .department(department)
            .dueAt(WebUtil.parseDate(due))
            .version(FormDataUtil.parseRecordVersion(version))
            .build();

    return getPageData(CasePageDto.builder())
        .item(item)
        .cases(data.getCases())
        .details(data.getDetails())
        .query(query)
        .status(status)
        .assignee(assignee)
        .destination(destination)
        .body(body)
        .build();
  }

  /**
   * Populates this form from service response data for view rendering.
   *
   * @param source the source value to copy or map
   * @throws NullPointerException if {@code source} is {@code null}
   */
  public void setData(CasePageDto source) {
    data = source;
    setPageData(source);
    query = source.getQuery();
    status = source.getStatus();
    assignee = source.getAssignee();
    destination = source.getDestination();
    body = source.getBody();

    if (source.getItem() != null) {
      CaseDto item = source.getItem();
      setId(item.getId() == null ? null : item.getId().toString());
      title = item.getTitle();
      description = item.getDescription();
      category = item.getCategory();
      priority = item.getPriority();
      department = item.getDepartment();
      due = WebUtil.formatDate(item.getDueAt());
      version = item.getVersion() == null ? null : item.getVersion().toString();
    }
  }

  /**
   * Validates controls required by the mapped case operation, leaving ownership and workflow rules
   * to services.
   *
   * @param mapping the Struts mapping for the current operation
   * @param request the current HTTP request
   * @return the validation errors, or an empty collection when the submitted values are valid
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws NullPointerException if {@code mapping} or {@code request} is {@code null}
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   */
  @Override
  public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
    ActionErrors errors = super.validate(mapping, request);
    if (isInputForward(mapping)) {
      return errors;
    }
    String operation = mapping.getParameter();

    if (ActionOperation.CREATE.matchesParameter(operation)
        || ActionOperation.UPDATE.matchesParameter(operation)) {
      FormValidationUtil.validateChoice(
          errors,
          FormFields.PRIORITY,
          MessagesUtil.formatMessage(MessageKeys.FIELD_PRIORITY),
          priority,
          Priority.getCodes());
    }
    if (ActionOperation.TRANSITION.matchesParameter(operation)) {
      FormValidationUtil.validateChoice(
          errors,
          FormFields.DESTINATION,
          MessagesUtil.formatMessage(MessageKeys.FIELD_NEXT_STATUS),
          destination,
          com.example.legacy.domain.CaseStatus.getCodes());
    }
    if (ActionOperation.LIST.matchesParameter(operation)) {
      FormValidationUtil.validateChoice(
          errors,
          FormFields.STATUS,
          MessagesUtil.formatMessage(MessageKeys.FIELD_STATUS),
          status,
          com.example.legacy.domain.CaseStatus.getCodes());
    }

    return errors;
  }

  /**
   * Refreshes server display data without overwriting invalid update fields or the submitted lock
   * version.
   *
   * @param source the source value to copy or map
   */
  public void setDisplayData(CasePageDto source) {
    data = source;
  }

  /**
   * Maps only the controls required by the getCreateData application operation.
   *
   * @return the create case DTO
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   */
  public CreateCaseDto getCreateData() {
    return CreateCaseDto.builder()
        .title(title)
        .description(description)
        .category(category)
        .priority(priority)
        .department(department)
        .dueAt(WebUtil.parseDate(due))
        .build();
  }

  /**
   * Maps only the controls required by the getUpdateData application operation.
   *
   * @return the update case DTO
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   */
  public UpdateCaseDto getUpdateData() {
    return UpdateCaseDto.builder()
        .id(FormDataUtil.parseRecordId(getId()))
        .version(FormDataUtil.parseRecordVersion(version))
        .title(title)
        .description(description)
        .priority(priority)
        .dueAt(WebUtil.parseDate(due))
        .build();
  }

  /**
   * Maps only the controls required by the getAssignData application operation.
   *
   * @return the assign case DTO
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   */
  public AssignCaseDto getAssignData() {
    return AssignCaseDto.builder()
        .id(FormDataUtil.parseRecordId(getId()))
        .assignee(assignee)
        .build();
  }

  /**
   * Maps only the controls required by the getTransitionData application operation.
   *
   * @return the transition case DTO
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   */
  public TransitionCaseDto getTransitionData() {
    return TransitionCaseDto.builder()
        .id(FormDataUtil.parseRecordId(getId()))
        .destination(destination)
        .build();
  }

  /**
   * Maps only the controls required by the getCommentData application operation.
   *
   * @return the add case comment DTO
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   */
  public AddCaseCommentDto getCommentData() {
    return AddCaseCommentDto.builder().id(FormDataUtil.parseRecordId(getId())).body(body).build();
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
   * Maps only the controls required by the getSearchData application operation.
   *
   * @return the case search criteria DTO
   */
  public CaseSearchCriteriaDto getSearchData() {
    return CaseSearchCriteriaDto.builder()
        .query(query)
        .status(status)
        .assignee(assignee)
        .page(FormDataUtil.parsePageIndex(getPageNumber()))
        .build();
  }
}
