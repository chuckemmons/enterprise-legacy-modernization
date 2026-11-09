package com.example.legacy.integration;

import static org.junit.Assert.assertTrue;

import com.example.legacy.service.dto.CaseDto;
import com.example.legacy.service.dto.CasePageDto;
import com.example.legacy.support.ValidatorTestSupport;
import com.example.legacy.web.AuthForm;
import com.example.legacy.web.BasePageForm;
import com.example.legacy.web.CaseForm;
import com.example.legacy.web.NotificationForm;
import com.example.legacy.web.UploadForm;
import com.example.legacy.web.UserForm;
import com.example.legacy.web.ValidationInputAction;
import com.example.legacy.web.WebUtil;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.config.ControllerConfig;
import org.apache.struts.config.impl.ModuleConfigImpl;
import org.apache.struts.upload.FormFile;
import org.apache.struts.upload.MultipartRequestHandler;
import org.apache.struts.util.MessageResources;
import org.apache.struts.validator.ValidatorForm;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Checks operation-specific form validation and the real Struts validation/input-forward lifecycle.
 *
 * @author Chuck Emmons
 */
public class FormValidationIntegrationTest {

  /**
   * Verifies field presence, length, date, version, enum, and pagination validation without service
   * policy checks.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   */
  @Test
  public void validatesCaseFields() {
    HttpServletRequest request = createRequest();
    assertTrue(
        "empty new page must not show errors",
        ValidatorTestSupport.prepareForm(new CaseForm())
            .validate(createActionMapping("new", "input"), request)
            .isEmpty());
    assertTrue(
        "empty search is valid",
        ValidatorTestSupport.prepareForm(new CaseForm())
            .validate(createActionMapping("list", "input"), request)
            .isEmpty());
    CaseForm form = createValidCaseForm();
    assertTrue(
        "valid creation input",
        form.validate(createActionMapping("create", "input"), request).isEmpty());
    form.setTitle(" ");
    form.setDescription(String.join("", Collections.nCopies(4001, "x")));
    form.setPriority("INVALID");
    form.setDue("2026-02-30");
    ActionErrors errors = form.validate(createActionMapping("create", "input"), request);
    assertTrue(
        "multiple independent fields are reported together",
        errors.get("title").hasNext()
            && errors.get("description").hasNext()
            && errors.get("priority").hasNext()
            && errors.get("due").hasNext());
    form = createValidCaseForm();
    form.setDue("2026-11-05 trailing-text");
    assertTrue(
        "date must use the exact control format",
        form.validate(createActionMapping("create", "input"), request).get("due").hasNext());
    form = createValidCaseForm();
    form.setId("5");
    form.setVersion("0");
    form.setCategory(null);
    form.setDepartment(null);
    assertTrue(
        "updates do not require creation-only fields",
        form.validate(createActionMapping("update", "input"), request).isEmpty());
    form.setVersion("not-a-version");
    assertTrue(
        "invalid lock token syntax is rejected",
        form.validate(createActionMapping("update", "input"), request).get("version").hasNext());
    form.setVersion("999");
    assertTrue(
        "stale-version comparison belongs to the service",
        form.validate(createActionMapping("update", "input"), request).isEmpty());
    CaseForm command = ValidatorTestSupport.prepareForm(new CaseForm());
    command.setId("5");
    command.setBody("Note");
    assertTrue(
        "note submission does not require edit controls",
        command.validate(createActionMapping("comment", "input"), request).isEmpty());
    command.setDestination("CLOSED");
    assertTrue(
        "workflow edge and ownership are service rules",
        command.validate(createActionMapping("transition", "input"), request).isEmpty());
    command.setBody(" ");
    assertTrue(
        "blank note rejected",
        command.validate(createActionMapping("comment", "input"), request).get("body").hasNext());
    command.setId("-1");
    assertTrue(
        "record IDs must be positive",
        command.validate(createActionMapping("archive", "input"), request).get("id").hasNext());
    command.setId(null);
    command.setPageNumber("overflow");
    assertTrue(
        "invalid pagination cannot break JSP arithmetic",
        command.validate(createActionMapping("list", "input"), request).get("page").hasNext()
            && "0".equals(command.getPageNumber()));
  }

  /**
   * Checks login versus account-creation controls and prevents password redisplay on validation
   * errors.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   */
  @Test
  public void validatesAccountFields() {
    HttpServletRequest request = createRequest();
    AuthForm login = ValidatorTestSupport.prepareForm(new AuthForm());
    assertTrue(
        "initial login page is not a failed submission",
        login.validate(createActionMapping("login", "input"), request).isEmpty());
    assertTrue(
        "login credentials required",
        login.validate(createActionMapping("authenticate", "input"), request).size() == 2);
    login.setUsername("admin");
    login.setPassword("short");
    assertTrue(
        "login does not impose new-password rules on existing accounts",
        login.validate(createActionMapping("authenticate", "input"), request).isEmpty());
    login.setUsername(null);
    assertTrue(
        "invalid login clears secret form state",
        !login.validate(createActionMapping("authenticate", "input"), request).isEmpty()
            && login.getPassword() == null);
    UserForm user = ValidatorTestSupport.prepareForm(new UserForm());
    assertTrue(
        "user listing does not validate create controls",
        user.validate(createActionMapping("list", "input"), request).isEmpty());
    user.setUsername("Worker");
    user.setName("Worker Name");
    user.setDepartment("Operations");
    user.setPassword("ValidPassword!2026");
    user.setRole("WORKER");
    user.setEmail("worker@example.test");
    assertTrue(
        "valid account controls including normalized username",
        user.validate(createActionMapping("create", "input"), request).isEmpty());
    user.setEmail("invalid-email");
    user.setRole("OWNER");
    user.setPassword("short");
    ActionErrors errors = user.validate(createActionMapping("create", "input"), request);
    assertTrue(
        "account errors collected",
        errors.get("email").hasNext()
            && errors.get("role").hasNext()
            && errors.get("password").hasNext());
    assertTrue(
        "create password must be re-entered after validation failure", user.getPassword() == null);
    UserForm role = ValidatorTestSupport.prepareForm(new UserForm());
    role.setId("5");
    role.setRole("ADMIN");
    assertTrue(
        "role authorization is checked by services, not the form",
        role.validate(createActionMapping("role", "input"), request).isEmpty());
    NotificationForm notice = ValidatorTestSupport.prepareForm(new NotificationForm());
    assertTrue(
        "notification read needs a record ID",
        notice.validate(createActionMapping("read", "input"), request).get("id").hasNext());
  }

  /**
   * Checks automatic input forwarding, multipart rollback, and validation-free read-only redisplay.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void preservesValidationLifecycle() throws Exception {
    Processor processor = new Processor();
    HttpServletRequest request = createRequest();
    CaseForm form = createValidCaseForm();
    form.setTitle("");
    assertTrue(
        "Struts must stop before executing the mutation action",
        !processor.validateFormThroughProcessor(
            request, form, createActionMapping("create", "page.case-new")));
    assertTrue(
        "Tiles input receives standard Struts errors",
        "page.case-new".equals(processor.forwarded)
            && request.getAttribute(Globals.ERROR_KEY) instanceof ActionErrors);
    final int[] readsAndRollbacks = {0, 0};
    FormFile file = org.mockito.Mockito.mock(FormFile.class);
    org.mockito.Mockito.when(file.getFileSize()).thenReturn(2 * 1024 * 1024 + 1);
    org.mockito.Mockito.when(file.getContentType()).thenReturn("text/html");
    org.mockito.Mockito.when(file.getFileData())
        .thenAnswer(call -> {
          readsAndRollbacks[0]++;
          throw new AssertionError("Validation must not read upload bytes");
        });
    UploadForm upload = ValidatorTestSupport.prepareForm(new UploadForm());
    upload.setId("5");
    upload.setFile(file);
    MultipartRequestHandler multipart = org.mockito.Mockito.mock(MultipartRequestHandler.class);
    org.mockito.Mockito.doAnswer(
            call -> {
              readsAndRollbacks[1]++;
              return null;
            })
        .when(multipart)
        .rollback();
    upload.setMultipartRequestHandler(multipart);
    assertTrue(
        "invalid multipart input is stopped before DocumentAction",
        !processor.validateFormThroughProcessor(
            request, upload, createActionMapping("upload", "page.upload-errors")));
    assertTrue(
        "multipart input must go directly to a Tiles definition without a second parsing pass",
        "page.upload-errors".equals(processor.forwarded));
    assertTrue(
        "Struts rolls back rejected multipart resources without materializing bytes",
        readsAndRollbacks[0] == 0 && readsAndRollbacks[1] == 1);
    HttpServletRequest oversized = createRequest();
    oversized.setAttribute(MultipartRequestHandler.ATTRIBUTE_MAX_LENGTH_EXCEEDED, Boolean.TRUE);
    UploadForm missing = ValidatorTestSupport.prepareForm(new UploadForm());
    missing.setId("5");
    assertTrue(
        "container upload overflow is a displayable form error",
        missing.validate(createActionMapping("upload", "input"), oversized).get("file").hasNext());
    ActionMapping input = createActionMapping("case", "page.error");
    input.setType(ValidationInputAction.class.getName());
    CaseForm invalid = ValidatorTestSupport.prepareForm(new CaseForm());
    invalid.setId("bad");
    invalid.setDue("bad");
    invalid.setVersion("bad");
    assertTrue(
        "read-only input routes must not revalidate or loop",
        processor.validateFormThroughProcessor(request, invalid, input));
    assertTrue(
        "input routing preserves original errors", request.getAttribute(Globals.ERROR_KEY) != null);
    ActionForward fallback = new ValidationInputAction().execute(input, invalid, request, null);
    assertTrue(
        "invalid IDs fail gracefully without querying an arbitrary record",
        "page.error".equals(fallback.getPath()));
  }

  /**
   * Ensures form redisplay preserves invalid edit values and the submitted version instead of
   * overwriting them.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   */
  @Test
  public void preservesSubmittedValuesOnRedisplay() {
    CaseForm form = createValidCaseForm();
    form.setTitle("");
    form.setDue("bad-date");
    form.setVersion("stale-token");
    CaseDto persisted = CaseDto.builder().title("Database title").version(9).build();
    CasePageDto loaded = CasePageDto.builder().item(persisted).build();
    form.setDisplayData(loaded);
    assertTrue(
        "server reload must not overwrite correction controls or optimistic-lock context",
        "".equals(form.getTitle())
            && "bad-date".equals(form.getDue())
            && "stale-token".equals(form.getVersion()));
    assertTrue(
        "server-owned display data remains available",
        "Database title".equals(form.getItem().getTitle()));
    HttpServletRequest request = createRequest();
    WebUtil.addBusinessError(
        request,
        new com.example.legacy.service.exception.BusinessValidationException(
            "validation.business", "<script>alert(1)</script>"));
    ActionMessage error =
        (ActionMessage) ((ActionErrors) request.getAttribute(Globals.ERROR_KEY)).get().next();
    assertTrue(
        "business messages are escaped before html:errors renders them",
        !error.getValues()[0].toString().contains("<script>"));
    MessageResources messages = MessageResources.getMessageResources("ApplicationResources");
    assertTrue(
        "configured properties bundle formats field errors",
        messages.getMessage(Locale.ENGLISH, "validation.required", "Title").contains("Title"));
    assertTrue(
        "validation summary has accessible markup",
        messages.getMessage(Locale.ENGLISH, "errors.header").contains("role=\"alert\""));
  }

  /**
   * Checks all mappings enable validation and have resolvable input pages/actions and a configured
   * bundle.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void validatesRouteConfiguration() throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    Document document =
        factory.newDocumentBuilder().parse(new File("src/main/webapp/WEB-INF/struts-config.xml"));
    NodeList actions = document.getElementsByTagName("action");
    Document tiles =
        factory.newDocumentBuilder().parse(new File("src/main/webapp/WEB-INF/tiles-defs.xml"));
    Set<String> definitions = new HashSet<>();
    NodeList tilesPages = tiles.getElementsByTagName("definition");
    for (int i = 0; i < tilesPages.getLength(); i++) {
      definitions.add(((Element) tilesPages.item(i)).getAttribute("name"));
    }
    Set<String> paths = new HashSet<>();
    for (int i = 0; i < actions.getLength(); i++) {
      paths.add(((Element) actions.item(i)).getAttribute("path") + ".do");
    }
    for (int i = 0; i < actions.getLength(); i++) {
      Element action = (Element) actions.item(i);
      assertTrue(
          "validation enabled for " + action.getAttribute("path"),
          "true".equals(action.getAttribute("validate")));
      String input = action.getAttribute("input");
      assertTrue(
          "resolvable validation input for " + action.getAttribute("path"),
          paths.contains(input) || definitions.contains(input));
      if ("/caseUpload".equals(action.getAttribute("path"))) {
        assertTrue(
            "rejected multipart bodies must never be forwarded through another Action",
            "page.upload-errors".equals(input));
      }
    }
    Map<String, String> formTypes = new HashMap<>();
    NodeList beans = document.getElementsByTagName("form-bean");
    for (int i = 0; i < beans.getLength(); i++) {
      Element bean = (Element) beans.item(i);
      formTypes.put(bean.getAttribute("name"), bean.getAttribute("type"));
    }
    for (int i = 0; i < actions.getLength(); i++) {
      Element action = (Element) actions.item(i);
      if (ValidationInputAction.class.getName().equals(action.getAttribute("type"))) {
        continue;
      }
      BasePageForm form =
          (BasePageForm)
              Class.forName(formTypes.get(action.getAttribute("name")))
                  .getDeclaredConstructor()
                  .newInstance();
      ActionMapping configured =
          createActionMapping(
              action.hasAttribute("parameter") ? action.getAttribute("parameter") : null,
              action.getAttribute("input"));
      assertTrue(
          "missing XML validation rules for " + action.getAttribute("path"),
          ValidatorTestSupport.getValidatorResources()
                  .getForm(Locale.ENGLISH, form.getValidationKey(configured, createRequest()))
              != null);
    }
    assertTrue(
        "default validation message resources configured",
        "ApplicationResources"
            .equals(
                ((Element) document.getElementsByTagName("message-resources").item(0))
                    .getAttribute("parameter")));
  }

  /**
   * Prevents legacy pagination from changing validation stages and proves rules are
   * operation-specific.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void keepsValidationIndependentOfPagination() throws Exception {
    CaseForm form = createValidCaseForm();
    assertTrue("shared forms must extend ValidatorForm", form instanceof ValidatorForm);
    form.setTitle("");
    Map<String, String> legacy = new HashMap<>();
    legacy.put("page", "7");
    HttpServletRequest request = createRequest(legacy);
    ActionMapping create = createActionMapping("create", "input");
    form.reset(create, request);
    BeanUtils.populate(form, legacy);
    assertTrue(
        "legacy page must never bypass stage-zero XML rules",
        form.validate(create, request).get("title").hasNext());
    assertTrue(
        "pagination and framework validation stage are independent",
        "7".equals(form.getPageNumber()) && form.getPage() == 0);
    assertTrue("real ValidatorForm executed Commons Validator", form.getValidatorResults() != null);
    assertTrue(
        "creation-only fields are declared in XML",
        ValidatorTestSupport.getValidatorResources()
                .getForm(Locale.ENGLISH, "CaseForm.create")
                .getField("category")
            != null);
    assertTrue(
        "update rule set must not require creation-only controls",
        ValidatorTestSupport.getValidatorResources()
                .getForm(Locale.ENGLISH, "CaseForm.update")
                .getField("category")
            == null);
    assertTrue(
        "version syntax is declarative while stale-version comparison stays in services",
        ValidatorTestSupport.getValidatorResources()
                .getForm(Locale.ENGLISH, "CaseForm.update")
                .getField("version")
            != null);
    legacy.put("page", "invalid");
    form.reset(create, request);
    BeanUtils.populate(form, legacy);
    assertTrue(
        "raw legacy pagination is checked despite BeanUtils numeric conversion",
        form.validate(create, request).get("page").hasNext());
    ActionMapping input = createActionMapping("case", "input");
    input.setType(ValidationInputAction.class.getName());
    form.reset(input, request);
    BeanUtils.populate(form, legacy);
    assertTrue(
        "input redisplay normalizes rebound invalid pagination without looping",
        form.validate(input, request).isEmpty() && "0".equals(form.getPageNumber()));
    UploadForm upload = ValidatorTestSupport.prepareForm(new UploadForm());
    Map<String, String> multipart = new HashMap<>();
    multipart.put("_contentType", "multipart/form-data");
    upload.reset(createActionMapping("upload", "input"), createRequest(multipart));
    assertTrue(
        "reset must not parse multipart controls before Struts does",
        "0".equals(upload.getPageNumber()));
  }

  /**
   * Creates a request proxy with request attributes and an authenticated session, without database
   * access.
   *
   * @return the mocked HTTP request with the configured attributes and session
   */
  private static HttpServletRequest createRequest() {
    return createRequest(Collections.emptyMap());
  }

  /**
   * Supplies Mockito servlet collaborators to the production validation pipeline.
   *
   * @param parameters the request parameters indexed by name
   * @return the mocked HTTP request with the configured attributes and session
   * @throws NullPointerException if {@code parameters} is {@code null}
   */
  private static HttpServletRequest createRequest(Map<String, String> parameters) {
    return com.example.legacy.support.ServletTestSupport.createRequest(
        ValidatorTestSupport.getModuleConfiguration(), parameters);
  }

  /**
   * Creates an ordinary validated mapping with an explicit input destination.
   *
   * @param operation the application operation name
   * @param input the input text
   * @return the action mapping
   */
  private static ActionMapping createActionMapping(String operation, String input) {
    ActionMapping mapping = new ActionMapping();
    mapping.setPath("/test");
    mapping.setParameter(operation);
    mapping.setValidate(true);
    mapping.setInput(input);
    return mapping;
  }

  /**
   * Creates a valid creation form that can be modified to exercise individual checks.
   *
   * @return the case form
   */
  private static CaseForm createValidCaseForm() {
    CaseForm form = ValidatorTestSupport.prepareForm(new CaseForm());
    form.setTitle("Title");
    form.setDescription("Description");
    form.setPriority("MEDIUM");
    form.setCategory("GENERAL");
    form.setDepartment("Operations");
    form.setDue("2026-11-05");
    return form;
  }

  /**
   * Exposes Struts' actual validation gate while capturing its input forward instead of dispatching
   * a servlet.
   *
   * @author Chuck Emmons
   */
  private static final class Processor extends org.apache.struts.tiles.TilesRequestProcessor {
    private String forwarded;

    /** Initializes the standard default controller configuration used by validation forwarding. */
    private Processor() {
      moduleConfig = new ModuleConfigImpl("");
      moduleConfig.setControllerConfig(new ControllerConfig());
    }

    /**
     * Records the forward path while leaving the original request errors intact.
     *
     * @param path the application-relative path
     * @param request the current HTTP request
     * @param response the HTTP response to populate
     */
    @Override
    protected void internalModuleRelativeForward(
        String path, HttpServletRequest request, HttpServletResponse response) {
      forwarded = path;
    }

    /**
     * Runs the real Struts validation gate to verify invalid submissions cannot reach an action.
     *
     * @param request the current HTTP request
     * @param form the form containing the submitted values
     * @param mapping the Struts mapping for the current operation
     * @return {@code true} if the submitted form passes validation; {@code false} otherwise
     * @throws Exception if test setup, execution, or cleanup fails
     */
    private boolean validateFormThroughProcessor(
        HttpServletRequest request, ActionForm form, ActionMapping mapping) throws Exception {
      return processValidate(request, null, form, mapping);
    }
  }
}
