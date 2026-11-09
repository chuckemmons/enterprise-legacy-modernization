package com.example.characterization;

import java.util.Map;
import java.util.Properties;

/**
 * Owns an isolated fixture environment and exposes snapshots without generated IDs.
 *
 * @author Chuck Emmons
 */
public interface ImplementationAdapter extends AutoCloseable {
  /**
   * Creates an independent, empty environment and seeds the fixture accounts.
   *
   * @param fixture the account definitions and scenario data
   * @throws Exception if environment creation or account seeding fails
   */
  void initialize(Properties fixture) throws Exception;

  /**
   * Executes a command in {@code OPERATION|actor|argument} format.
   *
   * @param command the operation, actor, and optional argument separated by pipes
   * @return the operation outcome and full normalized durable snapshot
   * @throws Exception if command execution or snapshot collection fails
   */
  Map<String, String> execute(String command) throws Exception;

  /**
   * Releases the resources owned by this environment, including after failed initialization.
   *
   * @throws Exception if resource cleanup fails
   */
  @Override
  void close() throws Exception;
}
