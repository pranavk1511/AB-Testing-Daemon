package com.abdaemon.infrastructure;

import com.abdaemon.infrastructure.config.RefreshingFileConfigRepository;
import com.abdaemon.infrastructure.logging.WalEventSink;
import com.abdaemon.infrastructure.server.HttpAssignmentServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;

/** Starts the config reloader + HTTP endpoint + WAL sink. */
public final class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        String cfgPath = System.getenv().getOrDefault("AB_CFG", "config.json");
        int port = Integer.parseInt(System.getenv().getOrDefault("AB_PORT", "8080"));
        var period = Duration.ofSeconds(Long.parseLong(System.getenv().getOrDefault("AB_CFG_PERIOD_SEC", "5")));

        // WAL settings
        String walDir = System.getenv().getOrDefault("AB_WAL_DIR", "wal");
        long maxMb = Long.parseLong(System.getenv().getOrDefault("AB_WAL_MAX_MB", "32"));
        boolean fsync = Boolean.parseBoolean(System.getenv().getOrDefault("AB_WAL_FSYNC", "false"));

        try (var repo = new RefreshingFileConfigRepository(Path.of(cfgPath), period);
             var sink = new WalEventSink(Path.of(walDir), maxMb * 1024L * 1024L, fsync);
             var http = new HttpAssignmentServer(port, repo, sink)) {

            http.start();
            log.info("AB daemon up: http=127.0.0.1:{} | cfg={} | version={} | wal_dir={} max={}MB fsync={}",
                    port, cfgPath, repo.version(), walDir, maxMb, fsync);
            Thread.currentThread().join();
        }
    }
}
