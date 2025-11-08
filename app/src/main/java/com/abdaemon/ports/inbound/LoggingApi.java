package com.abdaemon.ports.inbound;

import com.abdaemon.domain.*;
import java.time.Instant;
import java.util.Map;

/** Logging endpoints for exposures and goals. */
public interface LoggingApi {
    void logExposure(ExperimentKey experiment, String treatment, Subject subject, Instant ts, Map<String,String> ctx);
    void logGoal(ExperimentKey experiment, String treatment, Subject subject, String goal, Double value, Instant ts, Map<String,String> ctx);
}
