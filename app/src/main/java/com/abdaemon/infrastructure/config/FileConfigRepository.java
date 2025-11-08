package com.abdaemon.infrastructure.config;

import com.abdaemon.domain.Experiment;
import com.abdaemon.domain.ExperimentKey;
import com.abdaemon.ports.outbound.ConfigRepository;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Loads experiments from a JSON file once at startup.
 * Expected structure: an array of Experiment objects.
 */
public final class FileConfigRepository implements ConfigRepository {
    private final String version;
    private final Map<String, Experiment> experiments;

    public FileConfigRepository(Path filePath) {
        this.version = String.valueOf(System.currentTimeMillis());
        this.experiments = load(filePath);
    }

    private Map<String, Experiment> load(Path path) {
        try {
            var mapper = new ObjectMapper();
            var list = mapper.readValue(Files.readAllBytes(path),
                    new TypeReference<List<Experiment>>() {});
            return list.stream().collect(Collectors.toMap(e -> e.key().value(), e -> e));
        } catch (IOException e) {
            System.err.println("⚠️ Could not load config: " + e.getMessage());
            return Map.of();
        }
    }

    @Override public String version() { return version; }
    @Override public List<Experiment> all() { return List.copyOf(experiments.values()); }
    @Override public Optional<Experiment> find(ExperimentKey key) {
        return Optional.ofNullable(experiments.get(key.value()));
    }
}
