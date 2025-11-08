package com.abdaemon.ports.outbound;

import java.time.Instant;
import java.util.Map;

/** Where exposure/goal events are sent (WAL, Kafka, etc.). */
public interface EventSink {
    void enqueueExposure(String experiment, String treatment, String subjectKey, Instant ts, Map<String,String> ctx);
    void enqueueGoal(String experiment, String treatment, String subjectKey, String goal, Double value, Instant ts, Map<String,String> ctx);
}
