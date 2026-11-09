package com.example.legacy.unit.service.dto;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.example.legacy.service.dto.AccountDto;
import com.example.legacy.service.dto.AuthDto;
import com.example.legacy.service.dto.CaseDetails;
import com.example.legacy.service.dto.CaseDto;
import com.example.legacy.service.dto.CaseNoteDto;
import com.example.legacy.service.dto.CasePageDto;
import com.example.legacy.service.dto.CaseSummary;
import com.example.legacy.service.dto.CountDto;
import com.example.legacy.service.dto.DocumentDto;
import com.example.legacy.service.dto.NoticeDto;
import com.example.legacy.service.dto.PageDto;
import com.example.legacy.service.dto.UserPageDto;
import com.example.legacy.web.AuthForm;
import com.example.legacy.web.UserForm;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

/**
 * Verifies immutable DTO state, builder reuse, and protection of mutable values at transport
 * boundaries.
 *
 * @author Chuck Emmons
 */
public class DtoImmutabilityTest {

  /**
   * Checks builders isolate inputs and copies while reusable builder changes leave earlier
   * snapshots intact.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void isolatesBuilderValuesAndCopies() {
    Date createdAt = new Date(1000);
    NoticeDto.Builder builder =
        NoticeDto.builder().recipient("worker").subject("Assigned").createdAt(createdAt);
    createdAt.setTime(2000);
    NoticeDto first = builder.build();
    assertTrue("builder snapshots mutable date input", first.getCreatedAt().getTime() == 1000);
    first.getCreatedAt().setTime(3000);
    assertTrue("date getter must not expose DTO state", first.getCreatedAt().getTime() == 1000);
    NoticeDto second = builder.subject("Changed").read(true).build();
    assertTrue(
        "builder reuse cannot change a built DTO",
        "Assigned".equals(first.getSubject())
            && !first.isRead()
            && "Changed".equals(second.getSubject()));
    NoticeDto copy = NoticeDto.builder(first).read(true).sentAt(new Date(4000)).build();
    assertTrue(
        "copy builders preserve and change values without mutating the source",
        !first.isRead()
            && first.getSentAt() == null
            && copy.isRead()
            && "worker".equals(copy.getRecipient()));
    byte[] payload = {1, 2};
    DocumentDto.Builder documentBuilder = DocumentDto.builder().payload(payload);
    payload[0] = 9;
    DocumentDto document = documentBuilder.build();
    assertTrue("builder copies byte-array inputs", document.getPayload()[0] == 1);
    document.getPayload()[0] = 8;
    assertTrue("byte-array getters return defensive copies", document.getPayload()[0] == 1);
    DocumentDto updatedDocument = documentBuilder.payload(new byte[] {3, 4}).build();
    assertTrue(
        "document builder reuse is isolated",
        document.getPayload()[0] == 1 && updatedDocument.getPayload()[0] == 3);
    DocumentDto copied = DocumentDto.builder(document).originalName("copied.txt").build();
    copied.getPayload()[1] = 7;
    assertTrue(
        "copied DTOs do not expose shared writable arrays",
        document.getPayload()[1] == 2 && copied.getPayload()[1] == 2);
    assertTrue(
        "nullable mutable values remain nullable",
        DocumentDto.builder().build().getPayload() == null
            && NoticeDto.builder().build().getCreatedAt() == null);
  }

  /**
   * Checks page/detail/summary collections form immutable snapshots rather than retaining source
   * list aliases.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void createsImmutablePageCollections() {
    CaseDto item = CaseDto.builder().id(7L).title("Original").build();
    List<CaseDto> cases = new ArrayList<>();
    cases.add(item);
    CasePageDto.Builder builder = CasePageDto.builder().id(7).page(3).cases(cases).item(item);
    cases.clear();
    CasePageDto first = builder.build();
    assertTrue(
        "page builder snapshots list inputs and inherited fields",
        first.getCases().size() == 1 && first.getId() == 7 && first.getPage() == 3);
    rejectsMutation(() -> first.getCases().clear());
    rejectsMutation(() -> first.getCases().add(item));
    Iterator<CaseDto> iterator = first.getCases().iterator();
    iterator.next();
    rejectsMutation(iterator::remove);
    CasePageDto copied =
        CasePageDto.builder(first)
            .page(4)
            .item(CaseDto.builder(item).title("Changed").build())
            .build();
    assertTrue(
        "page copy builders preserve inherited fields and isolate nested updates",
        first.getPage() == 3
            && "Original".equals(first.getItem().getTitle())
            && copied.getPage() == 4
            && copied.getId() == 7
            && copied.getCases().size() == 1);
    Set<String> nextStates = new LinkedHashSet<>();
    nextStates.add("APPROVED");
    nextStates.add("REJECTED");
    CaseDetails details =
        CaseDetails.builder().item(item).nextStates(nextStates).mayEdit(true).build();
    nextStates.clear();
    assertTrue(
        "workflow choice snapshots preserve insertion order",
        new ArrayList<>(details.getNextStates()).equals(Arrays.asList("APPROVED", "REJECTED")));
    rejectsMutation(() -> details.getNextStates().add("CLOSED"));
    rejectsMutation(() -> details.getNotes().add(CaseNoteDto.builder().build()));
    rejectsMutation(() -> details.getDocuments().clear());
    rejectsMutation(() -> details.getUsers().add(AccountDto.builder().build()));
    List<CountDto> statuses = new ArrayList<>();
    statuses.add(CountDto.builder().label("NEW").total(2).build());
    CaseSummary summary = CaseSummary.builder().statuses(statuses).overdue(1L).build();
    statuses.clear();
    assertTrue(
        "aggregate results are immutable snapshots too",
        summary.getStatuses().size() == 1 && summary.getStatuses().get(0).getTotal() == 2);
    rejectsMutation(() -> summary.getStatuses().clear());
    assertTrue(
        "null collection inputs normalize to empty read-only collections",
        CasePageDto.builder().cases(null).build().getCases().isEmpty());
  }

  /**
   * Ensures forms can clear display credentials without changing immutable incoming request values.
   *
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void preservesRequestCredentialsWhenFormsAreCleared() {
    AuthDto credentials =
        AuthDto.builder().username("worker").password("OriginalSecret!2026").build();
    AuthForm auth = new AuthForm();
    auth.setData(credentials);
    assertTrue(
        "login form stores a redacted snapshot instead of modifying the request DTO",
        auth.getPassword() == null
            && auth.getData().getPassword() == null
            && credentials.getPassword() != null);
    UserPageDto request =
        UserPageDto.builder()
            .account(AccountDto.builder().username("worker").build())
            .password("OriginalSecret!2026")
            .build();
    UserForm user = new UserForm();
    user.setData(request);
    assertTrue(
        "user form never redisplays immutable input credentials",
        user.getPassword() == null
            && user.getData().getPassword() == null
            && request.getPassword() != null);
  }

  /**
   * Verifies every DTO has final private state, nonpublic construction, and only public getter
   * instance methods.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void exposesOnlyImmutableDtoState() throws Exception {
    File[] files =
        new File("src/main/java/com/example/legacy/service/dto")
            .listFiles((directory, name) -> name.endsWith(".java"));
    assertTrue("all DTO types must be inspected", files != null && files.length > 0);
    for (File file : files) {
      String name = file.getName().replace(".java", "");
      Class<?> type = Class.forName("com.example.legacy.service.dto." + name);
      assertTrue(
          "DTO type must prevent mutable subclasses: " + name,
          Modifier.isFinal(type.getModifiers())
              || type == PageDto.class && Modifier.isAbstract(type.getModifiers()));
      assertImmutableFields(type);
      for (Constructor<?> constructor : type.getDeclaredConstructors()) {
        assertTrue(
            "DTO construction must use a builder: " + name,
            !Modifier.isPublic(constructor.getModifiers()));
      }
      assertGetterOnlyMethods(type);
      if (type != PageDto.class) {
        Object builder = type.getMethod("builder").invoke(null);
        assertTrue(
            "builder must create its declared DTO type: " + name,
            type.isInstance(builder.getClass().getMethod("build").invoke(builder)));
      }
    }
  }

  /**
   * Uses JUnit to verify collection snapshots reject mutation.
   *
   * @param operation the operation expected to fail
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  private static void rejectsMutation(Runnable operation) {
    assertThrows(UnsupportedOperationException.class, operation::run);
  }

  /**
   * Checks that every instance field is private and final, excluding static constants.
   *
   * @param type the DTO class to inspect
   * @throws AssertionError if a DTO instance field is mutable or visible outside the class
   * @throws NullPointerException if {@code type} is {@code null}
   */
  private void assertImmutableFields(Class<?> type) {
    for (Field field : type.getDeclaredFields()) {
      if (!Modifier.isStatic(field.getModifiers())) {
        assertTrue(
            "DTO field is writable: " + type.getSimpleName() + "." + field.getName(),
            Modifier.isPrivate(field.getModifiers()) && Modifier.isFinal(field.getModifiers()));
      }
    }
  }

  /**
   * Checks that every public instance method is a getter without parameters.
   *
   * @param type the DTO class to inspect
   * @throws AssertionError if a DTO exposes a public instance method other than a getter
   * @throws NullPointerException if {@code type} is {@code null}
   */
  private void assertGetterOnlyMethods(Class<?> type) {
    for (Method method : type.getDeclaredMethods()) {
      if (Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers())) {
        assertTrue(
            "DTO exposes a mutator: " + type.getSimpleName() + "." + method.getName(),
            method.getParameterCount() == 0
                && (method.getName().startsWith("get") || method.getName().startsWith("is")));
      }
    }
  }
}
