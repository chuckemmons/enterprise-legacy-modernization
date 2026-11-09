package com.example.legacy.unit.service.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.legacy.model.Account;
import com.example.legacy.service.dto.AccountDto;
import com.example.legacy.service.mapper.AccountMapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.Test;

/**
 * Behavioral contracts for AccountMapper.
 *
 * @author Chuck Emmons
 */
public class AccountMapperTest {

  private final AccountMapper mapper = new AccountMapper();

  /**
   * Snapshots account without exposing mutable dates.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void snapshotsAccountWithoutExposingMutableDates() {
    Account account = new Account();
    account.setId(4L);
    account.setUsername("worker");
    account.setDisplayName("Worker");
    account.setRole("WORKER");
    account.setEmail("worker@example.test");
    account.setDepartment("Operations");
    account.setEnabled(true);
    account.setCreatedAt(new Date(1234));
    AccountDto dto = mapper.mapToDto(account);
    account.getCreatedAt().setTime(0);
    assertEquals(Long.valueOf(4), dto.getId());
    assertEquals("Worker", dto.getDisplayName());
    assertEquals("WORKER", dto.getRole());
    assertEquals("worker@example.test", dto.getEmail());
    assertEquals(1234, dto.getCreatedAt().getTime());
    assertTrue(dto.isEnabled());
    Account created = mapper.mapToEntity(dto);
    assertEquals("worker", created.getUsername());
    assertEquals("Operations", created.getDepartment());
    assertNull(created.getId());
    assertNull(created.getPasswordHash());
  }

  /**
   * Preserves order and null elements.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void preservesOrderAndNullElements() {
    Account first = new Account();
    first.setUsername("first");
    Account second = new Account();
    second.setUsername("second");
    List<AccountDto> result = mapper.mapToDtos(Arrays.asList(first, null, second));
    assertEquals("first", result.get(0).getUsername());
    assertNull(result.get(1));
    assertEquals("second", result.get(2).getUsername());
    assertNull(mapper.mapToEntity(null));
    assertNull(mapper.mapToDto(null));
    assertTrue(mapper.mapToDtos(Collections.emptyList()).isEmpty());
  }

  /**
   * Updates only role and enabled flags.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void updatesOnlyRoleAndEnabledFlags() {
    Account target = new Account();
    target.setUsername("preserved");
    target.setPasswordHash("hash");
    AccountDto change =
        AccountDto.builder().role("AUDITOR").enabled(false).username("ignored").build();
    mapper.updateRole(change, target);
    mapper.updateEnabled(change, target);
    assertEquals("AUDITOR", target.getRole());
    assertFalse(target.isEnabled());
    assertEquals("preserved", target.getUsername());
    assertEquals("hash", target.getPasswordHash());
  }
}
