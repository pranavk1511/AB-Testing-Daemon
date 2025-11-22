# On‑Box A/B Testing Daemon (Java) — Centralized Functional Spec

# AB Testing Daemon

A lightweight, deterministic, file‑driven A/B testing daemon with:
- Hot‑reload configuration
- Deterministic hashing (bucketing)
- Traffic & holdout gating
- Targeting rules (country, app version)
- Durable exposure logging using WAL
- Simple HTTP API (`/health`, `/assign`)
- Minimal HTML/JS frontend
- Clean architecture (domain → application → infra)


## Purpose & Scope

A lightweight Java service that runs on the same host/pod as your application to provide on‑box A/B experiment assignment, owner‑controlled traffic allocation, and reliable event logging (exposures & goals) with operational guardrails.

# Features

## Deterministic User Assignment
Users are assigned to variants based on:
- `salt`
- stable subject identity (`userId`, `deviceId`, or `requestId`)
- weighted variants
- traffic and holdout settings

## Hot Reload Config
`RefreshingFileConfigRepository` monitors `config.json`.
When the file changes:
- reloads config
- revalidates experiments
- swaps an immutable snapshot
- increments config version

No daemon restart required.

## Targets & Eligibility
Supports optional rules:
- Allowed countries
- Minimum app version
- Start/end date
- Status: `running`, `draft`, `paused`

## Durable WAL Logging
Every exposure event is written to:
```
wal/events-YYYYMMDD-HHMMSS-#.wal
```

Append-only NDJSON lines ensure:
- crash safety
- replayability
- simple analysis

Optional `fsync` for stricter durability.

##  Simple Frontend
`frontend/index.html` + JS app:
- shows daemon health
- sends assign requests
- displays treatment, decision, configVersion
- maintains a history list


# Project Structure

```
/src
  /domain         -> Experiment, Variant, Targets, Subject, AssignmentDecision
  /application    -> AssignTreatmentUseCase, Bucketer, Ports
  /infrastructure
      /config     -> RefreshingFileConfigRepository + Jackson loader
      /logging    -> WalEventSink
      /server     -> HttpAssignmentServer
      Main.java   -> Daemon entrypoint

/frontend
  index.html
  app.js
  styles.css

config.json        -> Experiment definitions
ab-daemon.env      -> .env-style config (gitignored)
wal/               -> Write-Ahead Log output
```




# Workflow Diagram
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