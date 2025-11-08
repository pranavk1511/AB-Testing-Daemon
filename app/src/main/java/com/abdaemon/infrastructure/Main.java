package com.abdaemon.infrastructure;

import com.abdaemon.application.*;
import com.abdaemon.domain.*;
import com.abdaemon.infrastructure.config.FileConfigRepository;
import com.abdaemon.infrastructure.logging.NoopEventSink;
import java.nio.file.Path;
import java.util.Map;

/** Minimal runnable demo: loads config.json and assigns one subject. */
public final class Main {
    public static void main(String[] args) {
        var repo = new FileConfigRepository(Path.of("config.json"));
        var sink = new NoopEventSink();
        var useCase = new AssignTreatmentUseCase(repo, new Bucketer());

        var subject = Subject.of(new UserId("user123"), null, null);
        var ctx = Map.of("country", "US", "app_ver", "42");

        var key = new ExperimentKey("exp1");
        var decision = useCase.assign(key, subject, ctx);

        sink.enqueueExposure(key.value(), decision.treatment(),
                Bucketer.stableSubjectKey(subject), java.time.Instant.now(), ctx);

        System.out.println("Assignment → " + decision);
    }
}
