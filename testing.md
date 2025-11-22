# How to Test This A/B Testing Daemon

This guide explains how to run the daemon locally, verify assignment
logic, check WAL event logging, and test hot-reloading of the experiment
configuration.

## 1. Start the Daemon

From the project root:

``` bash
./gradlew run
```

If successful, you should see logs such as:

    INFO  RefreshingFileConfigRepository -- Loaded config: version=...
    INFO  Main -- AB daemon up: http=127.0.0.1:8080 | cfg=config.json | wal_dir=wal ...

If you want to override defaults:

``` bash
AB_PORT=9090 AB_CFG=config.json AB_CFG_PERIOD_SEC=5 ./gradlew run
```

------------------------------------------------------------------------

## 2. Health Check

``` bash
curl "http://127.0.0.1:8080/health"
```

Expected output:

``` json
{"status":"SERVING"}
```

------------------------------------------------------------------------

## 3. Test Assignments

Find your experiment key in `config.json`:

``` json
"key": { "value": "checkout_color" }
```

Then test assignment:

``` bash
curl "http://127.0.0.1:8080/assign?exp=checkout_color&user=u123&country=US&app_ver=42"
```

Example response:

``` json
{
  "experiment": "checkout_color",
  "treatment": "control",
  "decision": "ASSIGNED",
  "reasons": [],
  "configVersion": "1732332992000",
  "ts": "2025-11-22T18:50:01Z"
}
```

### Additional Tests

**Wrong country:**

``` bash
curl "http://127.0.0.1:8080/assign?exp=checkout_color&user=u123&country=IN&app_ver=42"
```

**Too low app version:**

``` bash
curl "http://127.0.0.1:8080/assign?exp=checkout_color&user=u123&country=US&app_ver=1"
```

------------------------------------------------------------------------

## 4. Verify WAL Logging

``` bash
ls -lh wal/
tail -n 5 wal/*.wal
```

Example WAL entry:

``` json
{"type":"exposure","ts":"2025-11-22T18:50:01Z","experiment":"checkout_color","treatment":"control","subject":"u:u123","ctx":{"country":"US","app_ver":"42"}}
```

------------------------------------------------------------------------

## 5. Test Hot Reloading

1.  Edit and save `config.json`, e.g.:

``` json
"variants": [
  { "name": "control", "weight": 0.5 },
  { "name": "blue",    "weight": 0.5 }
]
```

2.  Look for:

```{=html}
<!-- -->
```
    INFO RefreshingFileConfigRepository -- Config hot-reloaded: version=...

3.  Request assignment with a new user:

``` bash
curl "http://127.0.0.1:8080/assign?exp=checkout_color&user=u999&country=US&app_ver=42"
```
------------------------------------------------------------------------

## 6. Stopping the Daemon

    Ctrl + C

If port stays in use:

``` bash
lsof -i :8080
kill -9 <PID>
```

------------------------------------------------------------------------

## 7. Troubleshooting

### Config fails to load

Check terminal for:

    Config reload error: ...

### Address already in use

``` bash
lsof -i :8080
kill -9 <PID>
```

Or use:

``` bash
AB_PORT=9090 ./gradlew run
```

------------------------------------------------------------------------

Enjoy experimenting! :) 
