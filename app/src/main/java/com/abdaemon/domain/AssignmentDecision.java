package com.abdaemon.domain;

import java.util.List;

/** Result of assignment for a subject. */
public record AssignmentDecision(
        ExperimentKey experiment,
        String treatment,
        Decision decision,
        List<String> reasons,
        String configVersion
) {
    public enum Decision {
        ASSIGNED, INELIGIBLE, TRAFFIC_GATE, FALLBACK, WEIGHTS_ERROR
    }
}
