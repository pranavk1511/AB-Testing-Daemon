package com.abdaemon.application;

import com.abdaemon.domain.Subject;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/** Deterministic 0..9999 bucketer using SHA-256 + salt. */
public final class Bucketer {

    public static String stableSubjectKey(Subject s) {
        return s.userId().map(u -> "u:" + u.value())
                .or(() -> s.deviceId().map(d -> "d:" + d.value()))
                .or(() -> s.requestId().map(r -> "r:" + r.value()))
                .orElse("anon:" + UUID.randomUUID());
    }

    public int bucket(String salt, String key) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            md.update((byte) ':');
            md.update(key.getBytes(StandardCharsets.UTF_8));
            byte[] h = md.digest();
            long v = ByteBuffer.wrap(h, 0, 8).getLong();
            if (v < 0) v = ~v;
            return (int)(v % 10000);
        } catch (Exception e) {
            return Math.abs((salt + key).hashCode()) % 10000;
        }
    }
}
