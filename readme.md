# On‑Box A/B Testing Daemon (Java) — Centralized Functional Spec

## Purpose & Scope

A lightweight Java service that runs on the same host/pod as your application to provide on‑box A/B experiment assignment, owner‑controlled traffic allocation, and reliable event logging (exposures & goals) with operational guardrails.

## Functional-Requirements

- Sub‑millisecond assignment via gRPC over Unix Domain Socket (UDS)
- Owner‑defined % allocation per treatment (branch), hot‑reloaded config
- Sticky per‑subject assignments without central storage
- Durable exposure/goal logging with batch shipping
- First‑class observability (metrics, health, SRM)
- Safe, minimal operational footprint (systemd/K8s sidecar)

## Non - Functional Requirement 

- Statistical analysis or experiment decisioning (happens downstream)
- UI for editing experiments (config produced by CI/ops)

## High Level Architecture 

- Control Plane: versioned, signed configs (JSON/Proto) in S3/GCS/HTTP.
- On‑Box Daemon (Java 21): Netty gRPC on UDS, in‑RAM config, assigner, event queue + WAL, shipper, /metrics & /health.
- Analytics Sink: Kafka/Kinesis/S3 (pluggable) for offline analysis & dashboards. 

**Data Path (Request)**: App → Assign() → Daemon → variant → App renders → App LogExposure() → later LogGoal() → Daemon batches → Sink.

## Core Functionalities 

1. Config Management

    - Periodic pull (5–30s) with ETag/version check
    - Schema + semantic validation (weights sum, time windows, targets)
    - Signature verification (optional) and atomic hot‑swap
    - Tracks and emits config_version & config_age_seconds

2. Owner‑Controlled Traffic Allocation
    - Owner sets weights per treatment and traffic cap in config
    - Optional per‑segment overrides and time‑based rollout schedule
    - QA overrides (force treatment for test users) with validation

3. Sticky Assignment 
    - Subject identity: user_id ▸ fallback device_id ▸ fallback request_id
    - Deterministic bucket via hash(salt + subject_key) → map to weighted ranges
    - Traffic gate & eligibility checks (targets, window, holdout) before assignment

4. Event Logging & Durability
    - In‑memory bounded queue + Write‑Ahead Log (WAL) on disk
    - Batch shipper to sink with exponential backoff & dead‑letter policy
    - Exactly‑once enqueue semantics within the daemon process

5. Observability & Guardrails
    - Prometheus metrics: latency histograms, per‑variant counts, queue depth, shipper status, config staleness, SRM χ²
    - Health endpoints: liveness/readiness; degraded when stale config/backlog
    - Kill‑switches: global force‑control, per‑experiment pause

6. Security & Hardening

    - Non‑root user, strict FS perms on UDS (/var/run/ab.sock), read‑only root, whitelisted state dirs
    - Optional config signing (Ed25519) and WAL at‑rest encryption
    - PII minimization: hashed IDs, redaction map for context fields

## How to test this ? 

Refer `testing.md` :)

## Workflow Diagram 

```mermaid
sequenceDiagram
    autonumber
    participant UI as Client (Frontend)
    participant HS as HttpAssignmentServer
    participant UC as AssignTreatmentUseCase
    participant CR as ConfigRepository
    participant BK as Bucketer
    participant ES as EventSink
    participant WAL as WAL Files

    Note over UI,HS: Step 1: Client sends HTTP GET /assign with query params

    UI->>HS: 1) GET /assign?exp=...&user=...&country=...&app_ver=...
    HS->>HS: 2) Parse query string\nbuild ExperimentKey, Subject, ctx

    Note over HS,UC: Step 3: Delegate to application layer

    HS->>UC: 3) assign(key, subject, ctx)
    UC->>CR: 4) find(key)
    CR-->>UC: 5) Experiment or none

    alt Experiment not found or not running
        UC-->>HS: 6) AssignmentDecision (EXP_NOT_FOUND or INELIGIBLE)
    else Experiment found and eligible
        UC->>BK: 7) bucket(salt, stableSubjectKey)
        BK-->>UC: 8) bucket in [0..9999]
        UC->>UC: 9) Apply holdout and traffic gate
        UC->>UC: 10) Choose Variant by weight
        UC-->>HS: 11) AssignmentDecision (ASSIGNED, treatment)
    end

    Note over HS,ES: Step 12: Log exposure event

    HS->>ES: 12) enqueueExposure(experiment, treatment, subjectKey, ts, ctx)
    ES->>WAL: 13) Append JSON line to WAL file

    Note over HS,UI: Step 14: Return JSON to client

    HS-->>UI: 14) 200 OK + JSON {experiment, treatment, decision, configVersion, ts}
    UI->>UI: 15) Update UI and history list
```