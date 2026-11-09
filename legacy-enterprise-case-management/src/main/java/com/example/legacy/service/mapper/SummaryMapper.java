package com.example.legacy.service.mapper;

import com.example.legacy.service.dto.CountDto;
import java.util.ArrayList;
import java.util.List;

/**
 * Maps Hibernate aggregate query rows to named report transport values.
 *
 * @author Chuck Emmons
 */
public class SummaryMapper {
  /**
   * Converts grouped query rows to independent report DTOs while the service owns the query result.
   *
   * @param rows the grouped value/count query results
   * @return the mapped immutable values in source order
   * @throws NullPointerException if {@code rows} is {@code null}
   */
  public List<CountDto> mapToDtos(List<Object[]> rows) {
    List<CountDto> result = new ArrayList<>();
    for (Object[] row : rows) {
      result.add(
          CountDto.builder().label((String) row[0]).total(((Number) row[1]).longValue()).build());
    }
    return result;
  }
}
