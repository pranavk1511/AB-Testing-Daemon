package com.abdaemon.application;

import com.abdaemon.domain.*;
import com.abdaemon.ports.outbound.ConfigRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Pure use-case: decides a treatment; no I/O or logging yet. */
public final class AssignTreatmentUseCase {
    private final ConfigRepository config;
    private final Bucketer bucketer;

    public AssignTreatmentUseCase(ConfigRepository config, Bucketer bucketer) {
        this.config = config; this.bucketer = bucketer;
    }

    public AssignmentDecision assign(ExperimentKey key, Subject subject, Map<String,String> ctx) {
        var expOpt = config.find(key);
        if (expOpt.isEmpty() || !expOpt.get().isRunning())
            return new AssignmentDecision(key, "control", AssignmentDecision.Decision.FALLBACK, List.of("not_running_or_missing"), config.version());

        var exp = expOpt.get();
        var now = Instant.now();
        if (now.isBefore(exp.start()) || now.isAfter(exp.end()))
            return new AssignmentDecision(key, "control", AssignmentDecision.Decision.INELIGIBLE, List.of("out_of_window"), config.version());

        if (!exp.targets().countries().isEmpty()) {
            var c = ctx.getOrDefault("country", "");
            if (!exp.targets().countries().contains(c))
                return new AssignmentDecision(key, "control", AssignmentDecision.Decision.INELIGIBLE, List.of("country_targeting"), config.version());
        }
        if (exp.targets().minAppVersion() != null) {
            int v = Integer.parseInt(ctx.getOrDefault("app_ver", "0"));
            if (v < exp.targets().minAppVersion())
                return new AssignmentDecision(key, "control", AssignmentDecision.Decision.INELIGIBLE, List.of("min_app_version"), config.version());
        }

        var stableKey = Bucketer.stableSubjectKey(subject);
        int gate = bucketer.bucket(exp.salt(), "gate:" + stableKey);
        if (gate / 10000.0 > exp.traffic())
            return new AssignmentDecision(key, "control", AssignmentDecision.Decision.TRAFFIC_GATE, List.of(), config.version());

        int b = bucketer.bucket(exp.salt(), stableKey);
        int acc = 0, bound = 10000;
        for (var v : exp.variants()) {
            acc += (int)Math.round(v.weight() * bound);
            if (b < acc)
                return new AssignmentDecision(key, v.name(), AssignmentDecision.Decision.ASSIGNED, List.of(), config.version());
        }
        return new AssignmentDecision(key, "control", AssignmentDecision.Decision.WEIGHTS_ERROR, List.of(), config.version());
    }
}
