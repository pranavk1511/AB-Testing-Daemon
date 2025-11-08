package com.abdaemon.domain;

import java.time.Instant;
import java.util.List;

/** Aggregate root for an experiment. Immutable + validated. */
public record Experiment(
        ExperimentKey key,
        String status,           // "running" | "paused" | "draft"
        String salt,             // reshuffle seed; change only to re-bucket everyone
        double traffic,          // [0..1] exposure cap
        double holdout,          // [0..1) global holdout
        List<Variant> variants,  // weights must sum to 1.0
        Targets targets,         // optional eligibility
        Instant start,           // inclusive
        Instant end              // inclusive
) {
    public Experiment {
        if (key == null) throw new IllegalArgumentException("Experiment.key is required");
        if (status == null || status.isBlank()) throw new IllegalArgumentException("Experiment.status is required");
        if (salt == null || salt.isBlank()) throw new IllegalArgumentException("Experiment.salt is required");
        if (traffic < 0.0 || traffic > 1.0) throw new IllegalArgumentException("Experiment.traffic must be in [0,1]");
        if (holdout < 0.0 || holdout >= 1.0) throw new IllegalArgumentException("Experiment.holdout must be in [0,1)");
        if (variants == null || variants.isEmpty()) throw new IllegalArgumentException("Experiment.variants must not be empty");
        double sum = variants.stream().mapToDouble(Variant::weight).sum();
        if (Math.abs(sum - 1.0) > 1e-6) throw new IllegalArgumentException("Experiment.variants weights must sum to 1.0");
        targets = (targets == null) ? Targets.none() : targets;
        if (start == null || end == null) throw new IllegalArgumentException("Experiment.start/end required");
        if (start.isAfter(end)) throw new IllegalArgumentException("Experiment.start must be <= end");
    }

    public boolean isRunning() {
        return "running".equalsIgnoreCase(status);
    }
}
