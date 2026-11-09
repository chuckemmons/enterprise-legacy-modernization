package com.example.legacy.service.mapper;

import com.example.legacy.model.Account;
import com.example.legacy.service.dto.AccountDto;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts Account entities and AccountDto values without exposing persistence objects to the web
 * tier.
 *
 * @author Chuck Emmons
 */
public class AccountMapper {
  /**
   * Copies an entity to transport data, excluding credentials and mutable-value aliases.
   *
   * @param source the source value to copy or map
   * @return the mapped immutable account DTO, or {@code null} if the source is {@code null}
   */
  public AccountDto mapToDto(Account source) {
    if (source == null) {
      return null;
    }
    AccountDto target =
        AccountDto.builder()
            .id(source.getId())
            .username(source.getUsername())
            .displayName(source.getDisplayName())
            .email(source.getEmail())
            .role(source.getRole())
            .department(source.getDepartment())
            .enabled(source.isEnabled())
            .createdAt(source.getCreatedAt())
            .build();
    return target;
  }

  /**
   * Maps only writable fields into a new entity; services assign identity, ownership, and lifecycle
   * state.
   *
   * @param source the source value to copy or map
   * @return the mapped account, or {@code null} if the source is {@code null}
   */
  public Account mapToEntity(AccountDto source) {
    if (source == null) {
      return null;
    }
    Account target = new Account();
    target.setUsername(source.getUsername());
    target.setDisplayName(source.getDisplayName());
    target.setEmail(source.getEmail());
    target.setRole(source.getRole());
    target.setDepartment(source.getDepartment());
    return target;
  }

  /**
   * Converts query results to an entity-free transport list.
   *
   * @param sources the source values to map
   * @return the mapped immutable values in source order
   * @throws NullPointerException if {@code sources} is {@code null}
   */
  public List<AccountDto> mapToDtos(List<Account> sources) {
    List<AccountDto> targets = new ArrayList<>();
    for (Account source : sources) {
      targets.add(mapToDto(source));
    }
    return targets;
  }

  /**
   * Maps an authorized role change without changing credentials, enabled state, or identity.
   *
   * @param source the source value to copy or map
   * @param target the persistent entity to update
   * @throws NullPointerException if {@code source} or {@code target} is {@code null}
   */
  public void updateRole(AccountDto source, Account target) {
    target.setRole(source.getRole());
  }

  /**
   * Maps an authorized enabled-state change without changing account identity or credentials.
   *
   * @param source the source value to copy or map
   * @param target the persistent entity to update
   * @throws NullPointerException if {@code source} or {@code target} is {@code null}
   */
  public void updateEnabled(AccountDto source, Account target) {
    target.setEnabled(source.isEnabled());
  }
}
