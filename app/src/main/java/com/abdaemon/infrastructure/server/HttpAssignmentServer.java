package com.abdaemon.infrastructure.server;

import com.abdaemon.application.AssignTreatmentUseCase;
import com.abdaemon.application.Bucketer;
import com.abdaemon.domain.*;
import com.abdaemon.ports.outbound.ConfigRepository;
import com.abdaemon.ports.outbound.EventSink;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public final class HttpAssignmentServer implements AutoCloseable {
    private final HttpServer server;
    private final AssignTreatmentUseCase assign;
    private final EventSink sink;
    private final ObjectMapper mapper = new ObjectMapper();

    public HttpAssignmentServer(int port, ConfigRepository cfg, EventSink sink) throws IOException {
        this.assign = new AssignTreatmentUseCase(cfg, new Bucketer());
        this.sink = sink;
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 50);

        server.createContext("/health", this::health);
        server.createContext("/assign", this::assignHandler);
    }

    public void start() { server.start(); }
    @Override public void close() { server.stop(0); }

    private void health(HttpExchange ex) throws IOException {
        byte[] body = "{\"status\":\"SERVING\"}".getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(200, body.length);
        ex.getResponseBody().write(body);
        ex.close();
    }

    private void assignHandler(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(405, -1); ex.close(); return;
        }
        Map<String, String> q = parseQuery(ex.getRequestURI().getRawQuery());
        String exp = q.get("exp");
        if (exp == null || exp.isBlank()) {
            respondJson(ex, 400, Map.of("error", "missing exp"));
            return;
        }
        var subject = Subject.of(
                q.containsKey("user") ? new UserId(q.get("user")) : null,
                q.containsKey("device") ? new DeviceId(q.get("device")) : null,
                q.containsKey("req") ? new RequestId(q.get("req")) : null
        );
        var ctx = new HashMap<String,String>();
        if (q.containsKey("country")) ctx.put("country", q.get("country"));
        if (q.containsKey("app_ver")) ctx.put("app_ver", q.get("app_ver"));

        var decision = assign.assign(new ExperimentKey(exp), subject, ctx);

        // Log exposure (durably) via WAL
        sink.enqueueExposure(exp, decision.treatment(),
                Bucketer.stableSubjectKey(subject), Instant.now(), ctx);

        respondJson(ex, 200, Map.of(
                "experiment", decision.experiment().value(),
                "treatment", decision.treatment(),
                "decision", decision.decision().name(),
                "reasons", decision.reasons(),
                "configVersion", decision.configVersion(),
                "ts", Instant.now().toString()
        ));
    }

    private void respondJson(HttpExchange ex, int code, Object obj) throws IOException {
        byte[] body = mapper.writeValueAsBytes(obj);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(code, body.length);
        ex.getResponseBody().write(body);
        ex.close();
    }

    private static Map<String,String> parseQuery(String raw) {
        var map = new HashMap<String,String>();
        if (raw == null || raw.isBlank()) return map;
        for (String part : raw.split("&")) {
            int i = part.indexOf('=');
            String k = i > -1 ? part.substring(0,i) : part;
            String v = i > -1 ? part.substring(i+1) : "";
            map.put(urlDecode(k), urlDecode(v));
        }
        return map;
    }
    private static String urlDecode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }
}
