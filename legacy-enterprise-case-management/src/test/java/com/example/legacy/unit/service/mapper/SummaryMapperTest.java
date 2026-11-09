package com.example.legacy.unit.service.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.legacy.service.dto.CountDto;
import com.example.legacy.service.mapper.SummaryMapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/**
 * Behavioral contracts for SummaryMapper.
 *
 * @author Chuck Emmons
 */
public class SummaryMapperTest {

  /**
   * Converts numeric counts and keeps grouping order.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void convertsNumericCountsAndKeepsGroupingOrder() {
    List<CountDto> result =
        new SummaryMapper()
            .mapToDtos(
                Arrays.asList(
                    new Object[] {"HIGH", Integer.valueOf(2)},
                    new Object[] {"LOW", java.math.BigInteger.valueOf(9)}));
    assertEquals("HIGH", result.get(0).getLabel());
    assertEquals(2L, result.get(0).getTotal());
    assertEquals("LOW", result.get(1).getLabel());
    assertEquals(9L, result.get(1).getTotal());
    assertTrue(new SummaryMapper().mapToDtos(Collections.emptyList()).isEmpty());
  }
}
