package com.example.legacy.bootstrap;

/**
 * Explicit CLI job names accepted by the batch entry point.
 *
 * @author Chuck Emmons
 */
public enum JobType {
  /** The overdue job type. */
  OVERDUE("overdue"),
  /** The email job type. */
  EMAIL("email");

  private final String argument;

  /**
   * Stores the scheduler-facing argument for this job.
   *
   * @param argument the command-line argument to compare
   */
  JobType(String argument) {
    this.argument = argument;
  }

  /**
   * Returns whether a command-line argument selects this job.
   *
   * @param value the external code to compare
   * @return {@code true} if the supplied code matches this value; {@code false} otherwise
   */
  public boolean matchesArgument(String value) {
    return argument.equals(value);
  }
}
