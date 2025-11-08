package com.abdaemon.ports.outbound;

import com.abdaemon.domain.Experiment;
import com.abdaemon.domain.ExperimentKey;
import java.util.List;
import java.util.Optional;

/** Source of experiment definitions (file, DB, etc.). */
public interface ConfigRepository {
    String version();
    List<Experiment> all();
    Optional<Experiment> find(ExperimentKey key);
}
