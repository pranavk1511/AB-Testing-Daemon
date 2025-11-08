package com.abdaemon.domain;

/**
 * Strongly-typed identifier for an experiment.
 * Prevents accidental string mixups and keeps validation centralized.
 */
public record ExperimentKey(String value) {

  public ExperimentKey {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("ExperimentKey cannot be null or blank");
    }
  }

  @Override
  public String toString() {
    return value;
  }
}
