package com.abdaemon.domain;

/**
 * Represents one treatment branch in an experiment with its allocation weight.
 * Immutable, validated, and self-contained.
 */
public record Variant(String name, double weight) {

  public Variant {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Variant name cannot be null or blank");
    }
    if (weight <= 0.0 || weight > 1.0) {
      throw new IllegalArgumentException("Variant weight must be within (0, 1]");
    }
  }

  @Override
  public String toString() {
    return name + " (" + weight + ")";
  }
}
