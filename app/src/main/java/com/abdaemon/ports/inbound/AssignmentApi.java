package com.abdaemon.ports.inbound;

import com.abdaemon.domain.*;
import java.util.Map;

/** Entry point for external clients requesting assignments. */
public interface AssignmentApi {
    AssignmentDecision assign(ExperimentKey experiment, Subject subject, Map<String,String> context);
}
