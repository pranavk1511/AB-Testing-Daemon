package com.abdaemon.infrastructure.logging;

import com.abdaemon.ports.outbound.EventSink;
import java.time.Instant;
import java.util.Map;

/** No-op sink: simply prints or ignores events. Useful for local testing. */
public final class NoopEventSink implements EventSink {

    @Override
    public void enqueueExposure(String experiment, String treatment, String subjectKey,
                                Instant ts, Map<String, String> ctx) {
        System.out.printf("[EXPOSURE] exp=%s treatment=%s subject=%s%n",
                experiment, treatment, subjectKey);
    }

    @Override
    public void enqueueGoal(String experiment, String treatment, String subjectKey,
                            String goal, Double value, Instant ts, Map<String, String> ctx) {
        System.out.printf("[GOAL] exp=%s treatment=%s goal=%s value=%s subject=%s%n",
                experiment, treatment, goal, value, subjectKey);
    }
}
