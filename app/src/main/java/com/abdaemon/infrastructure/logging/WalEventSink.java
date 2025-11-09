package com.abdaemon.infrastructure.logging;

import com.abdaemon.ports.outbound.EventSink;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import static java.nio.file.StandardOpenOption.*;

/**
 * Durable, thread-safe Write-Ahead Log for A/B events.
 * - NDJSON (one JSON per line)
 * - size-based rotation (e.g., 32MB)
 * - optional fsync per write
 *
 * File layout:
 *   <dir>/events-YYYYMMDD-HHMMSS-<seq>.wal
 */
public final class WalEventSink implements EventSink, AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(WalEventSink.class);
    private static final String PREFIX = "events-";
    private static final String SUFFIX = ".wal";

    private final Path dir;
    private final long maxBytes;
    private final boolean fsync;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ReentrantLock lock = new ReentrantLock();

    private FileChannel ch;          // guarded by lock
    private Path current;            // guarded by lock
    private long writtenBytes = 0;   // guarded by lock
    private long seq = 0;            // guarded by lock

    public WalEventSink(Path dir, long maxBytes, boolean fsync) {
        this.dir = dir;
        this.maxBytes = maxBytes;
        this.fsync = fsync;
        try {
            Files.createDirectories(dir);
            openNewSegment();
        } catch (IOException e) {
            throw new RuntimeException("Failed to init WAL dir: " + dir, e);
        }
    }

    @Override
    public void enqueueExposure(String experiment, String treatment, String subjectKey,
                                Instant ts, Map<String, String> ctx) {
        var evt = Map.of(
                "type", "exposure",
                "ts", ts.toString(),
                "experiment", experiment,
                "treatment", treatment,
                "subject", subjectKey,
                "ctx", ctx
        );
        append(evt);
    }

    @Override
    public void enqueueGoal(String experiment, String treatment, String subjectKey,
                            String goal, Double value, Instant ts, Map<String, String> ctx) {
        var evt = Map.of(
                "type", "goal",
                "ts", ts.toString(),
                "experiment", experiment,
                "treatment", treatment,
                "subject", subjectKey,
                "goal", goal,
                "value", value,
                "ctx", ctx
        );
        append(evt);
    }

    private void append(Object event) {
        byte[] json;
        try {
            json = mapper.writeValueAsBytes(event);
        } catch (Exception e) {
            log.warn("WAL: failed to serialize event: {}", e.toString());
            return;
        }
        // JSON + newline
        byte[] line = withNewline(json);

        lock.lock();
        try {
            rotateIfNeeded(line.length);
            write(line);
        } catch (IOException e) {
            log.error("WAL write error: {}", e.toString());
        } finally {
            lock.unlock();
        }
    }

    private static byte[] withNewline(byte[] json) {
        byte[] line = new byte[json.length + 1];
        System.arraycopy(json, 0, line, 0, json.length);
        line[line.length - 1] = (byte) '\n';
        return line;
    }

    private void rotateIfNeeded(int incomingLen) throws IOException {
        if (ch == null) { openNewSegment(); return; }
        if (writtenBytes + incomingLen <= maxBytes) return;
        ch.force(true);
        ch.close();
        openNewSegment();
    }

    private void openNewSegment() throws IOException {
        String stamp = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                .withZone(java.time.ZoneOffset.UTC).format(Instant.now());
        String fname = PREFIX + stamp + "-" + (seq++) + SUFFIX;
        current = dir.resolve(fname);
        ch = FileChannel.open(current, CREATE, WRITE, APPEND);
        writtenBytes = Files.exists(current) ? Files.size(current) : 0;
        log.info("WAL opened segment {}", current.getFileName());
    }

    private void write(byte[] line) throws IOException {
        var buf = ByteBuffer.wrap(line);
        while (buf.hasRemaining()) ch.write(buf);
        writtenBytes += line.length;
        if (fsync) ch.force(true);
    }

    @Override
    public void close() {
        lock.lock();
        try {
            if (ch != null && ch.isOpen()) {
                try { ch.force(true); } catch (IOException ignored) {}
                try { ch.close(); } catch (IOException ignored) {}
            }
        } finally {
            lock.unlock();
        }
    }

    /* Optional helper to inspect current segment name (useful for tests) */
    public String currentSegment() {
        lock.lock();
        try { return current == null ? "" : current.getFileName().toString(); }
        finally { lock.unlock(); }
    }
}
