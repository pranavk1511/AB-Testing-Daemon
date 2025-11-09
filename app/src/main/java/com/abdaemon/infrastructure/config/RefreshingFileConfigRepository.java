package com.abdaemon.infrastructure.config;

import com.abdaemon.domain.Experiment;
import com.abdaemon.domain.ExperimentKey;
import com.abdaemon.ports.outbound.ConfigRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Periodically reloads experiments from a JSON file when the file mtime changes.
 * - Thread-safe snapshot (immutable map)
 * - Exposes a monotonically increasing version (mtime millis)
 */
public final class RefreshingFileConfigRepository implements ConfigRepository, AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(RefreshingFileConfigRepository.class);

    private final Path filePath;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ScheduledExecutorService exec;
    private final Duration period;

    private volatile Map<String, Experiment> current = Map.of();
    private volatile String version = "0";
    private volatile long lastMtime = -1L;

    public RefreshingFileConfigRepository(Path filePath, Duration period) {
        this.filePath = filePath;
        this.period = period;
        this.exec = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "config-reloader");
            t.setDaemon(true);
            return t;
        });
        initialLoad();
        exec.scheduleAtFixedRate(this::maybeReload, period.toMillis(), period.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void initialLoad() {
        try {
            reload();
            log.info("Loaded config: version={} experiments={}", version, current.size());
        } catch (Exception e) {
            log.warn("Initial config load failed: {}", e.toString());
        }
    }

    private void maybeReload() {
        try {
            long mt = Files.getLastModifiedTime(filePath).toMillis();
            if (mt != lastMtime) {
                reload();
                log.info("Config hot-reloaded: version={} experiments={}", version, current.size());
            }
        } catch (Exception e) {
            log.warn("Config reload error: {}", e.toString());
        }
    }

    private void reload() throws IOException {
        byte[] bytes = Files.readAllBytes(filePath);
        List<Experiment> list = mapper.readValue(bytes, new TypeReference<List<Experiment>>() {});
        Map<String, Experiment> byKey = list.stream().collect(Collectors.toUnmodifiableMap(
                e -> e.key().value(), e -> e
        ));
        long mt = Files.getLastModifiedTime(filePath).toMillis();
        this.current = byKey;
        this.version = String.valueOf(mt);
        this.lastMtime = mt;
    }

    @Override public String version() { return version; }
    @Override public List<Experiment> all() { return List.copyOf(current.values()); }
    @Override public Optional<Experiment> find(ExperimentKey key) {
        return Optional.ofNullable(current.get(key.value()));
    }

    @Override public void close() {
        exec.shutdownNow();
    }
}
