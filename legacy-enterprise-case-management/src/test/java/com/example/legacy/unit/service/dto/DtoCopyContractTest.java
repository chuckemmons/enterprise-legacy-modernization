package com.example.legacy.unit.service.dto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Checks immutable copy-builder contracts for each concrete application DTO.
 *
 * @author Chuck Emmons
 */
@RunWith(Parameterized.class)
public class DtoCopyContractTest {
  private final Class<?> type;

  /**
   * Selects one transport class for the copy-builder contract.
   *
   * @param type the DTO type to check
   */
  public DtoCopyContractTest(Class<?> type) {
    this.type = type;
  }

  /**
   * Discovers concrete transport types so newly added DTOs participate in the same contract.
   *
   * @return the DTO types with public builders
   * @throws Exception if a DTO class cannot be loaded
   * @throws AssertionError if source files cannot be inspected
   */
  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> types() throws Exception {
    File[] files =
        new File("src/main/java/com/example/legacy/service/dto")
            .listFiles((directory, name) -> name.endsWith(".java"));
    assertNotNull(files);
    Arrays.sort(files);
    List<Object[]> result = new ArrayList<>();
    for (File file : files) {
      Class<?> type =
          Class.forName("com.example.legacy.service.dto." + file.getName().replace(".java", ""));
      if (!Modifier.isAbstract(type.getModifiers())) {
        result.add(new Object[] {type});
      }
    }
    return result;
  }

  /**
   * Copies populated values without losing fields or exposing mutable aliases.
   *
   * @throws Exception if a DTO builder or accessor cannot be invoked
   * @throws AssertionError if copying changes an observable property or accepts a null source
   */
  @Test
  public void preservesPopulatedValuesInIndependentCopies() throws Exception {
    Object builder = type.getMethod("builder").invoke(null);
    for (Method method : builder.getClass().getMethods()) {
      if (method.getParameterCount() == 1
          && method.getReturnType().isAssignableFrom(builder.getClass())) {
        method.invoke(builder, sample(method.getParameterTypes()[0]));
      }
    }
    Object source = builder.getClass().getMethod("build").invoke(builder);
    Object copyBuilder = type.getMethod("builder", type).invoke(null, source);
    Object copy = copyBuilder.getClass().getMethod("build").invoke(copyBuilder);
    assertNotSame(source, copy);
    assertEquivalentProperties(source, copy);
    InvocationTargetException rejected =
        assertThrows(
            InvocationTargetException.class,
            () -> type.getMethod("builder", type).invoke(null, new Object[] {null}));
    assertTrue(rejected.getCause() instanceof NullPointerException);
  }

  /**
   * Creates a valid Java value for a transport property without applying business validation.
   *
   * @param valueType the builder argument type
   * @return the representative value
   * @throws Exception if a nested DTO cannot be built
   */
  private Object sample(Class<?> valueType) throws Exception {
    if (valueType == String.class) {
      return "sample";
    }
    if (valueType == int.class || valueType == Integer.class) {
      return 7;
    }
    if (valueType == long.class || valueType == Long.class) {
      return 7L;
    }
    if (valueType == boolean.class || valueType == Boolean.class) {
      return true;
    }
    if (valueType == Date.class) {
      return new Date(1234);
    }
    if (valueType == byte[].class) {
      return new byte[] {1, 2};
    }
    if (List.class.isAssignableFrom(valueType)) {
      return Collections.emptyList();
    }
    if (Set.class.isAssignableFrom(valueType)) {
      return new LinkedHashSet<>(Collections.singleton("NEW"));
    }
    Object builder = valueType.getMethod("builder").invoke(null);
    return builder.getClass().getMethod("build").invoke(builder);
  }

  /**
   * Checks the public snapshot contract while accounting for defensive array copies.
   *
   * @param source the original DTO
   * @param copy the copied DTO
   * @throws Exception if a transport accessor fails
   * @throws AssertionError if any copied property differs
   */
  private void assertEquivalentProperties(Object source, Object copy) throws Exception {
    for (Method method : type.getMethods()) {
      if (method.getParameterCount() != 0
          || method.getDeclaringClass() == Object.class
          || Modifier.isStatic(method.getModifiers())) {
        continue;
      }
      Object expected = method.invoke(source);
      Object actual = method.invoke(copy);
      if (expected instanceof byte[]) {
        assertArrayEquals(method.getName(), (byte[]) expected, (byte[]) actual);
      } else {
        assertEquals(method.getName(), expected, actual);
      }
      if (expected instanceof Date || expected instanceof byte[]) {
        assertNotSame(expected, actual);
      }
    }
  }
}
