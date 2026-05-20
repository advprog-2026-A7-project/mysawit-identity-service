# Monitoring — Prometheus + Grafana (local dev)

A beginner-friendly observability stack for `mysawit-identity-service`.
The goal is to learn the **mental model** of metrics, not to build a
production system.

---

## Mental model in 60 seconds

```
+--------------------+     scrape every 10s      +-------------+
| Spring Boot app    | <----------------------- | Prometheus  |
| :8081              |    GET /actuator/prom... | :9090       |
| (Micrometer)       | ------------------------> |  (TSDB)     |
+--------------------+    text-format metrics    +------+------+
                                                        |
                                                        | PromQL queries
                                                        v
                                                 +-------------+
                                                 |   Grafana   |
                                                 |   :3000     |
                                                 +-------------+
```

1. **Micrometer** (already in Spring Boot) measures things: HTTP
   requests, JVM memory, GC pauses, datasource pool size, etc.
2. The `micrometer-registry-prometheus` library turns those numbers
   into a plain-text format at `/actuator/prometheus`.
3. **Prometheus** *pulls* that endpoint every N seconds and stores
   each sample with a timestamp in its own time-series database.
4. **Grafana** queries Prometheus with **PromQL** to draw graphs.

You don't push metrics anywhere. The app just exposes them; the
scraper does the rest. That's the core idea.

---

## How to start the monitoring stack

```bash
# 1) Start your Spring Boot app FIRST so Prometheus has something to scrape.
#    From the repo root:
./gradlew bootRun
# (leave that terminal running)

# 2) In a second terminal, start Prometheus + Grafana:
docker compose -f docker-compose.monitoring.yml up -d

# 3) Confirm both containers are healthy:
docker compose -f docker-compose.monitoring.yml ps
```

Open in your browser:

| Tool       | URL                       | Login          |
|------------|---------------------------|----------------|
| App        | http://localhost:8081     | n/a            |
| Prometheus | http://localhost:9090     | n/a            |
| Grafana    | http://localhost:3000     | admin / admin  |

To stop everything (keeps data):

```bash
docker compose -f docker-compose.monitoring.yml down
```

To stop **and wipe** stored metrics + Grafana state:

```bash
docker compose -f docker-compose.monitoring.yml down -v
```

---

## How to test it (step by step)

### A. Is the app exposing metrics at all?

```bash
curl http://localhost:8081/actuator/health
# expected: {"status":"UP",...}

curl http://localhost:8081/actuator/prometheus | head -30
# expected: many lines like
#   # HELP jvm_memory_used_bytes ...
#   # TYPE jvm_memory_used_bytes gauge
#   jvm_memory_used_bytes{area="heap",id="Eden Space",...} 12345678.0
```

If `/actuator/prometheus` returns 404 → the
`micrometer-registry-prometheus` dependency isn't on the classpath, or
`prometheus` isn't in `management.endpoints.web.exposure.include`.

### B. Is Prometheus scraping correctly?

Open **http://localhost:9090/targets**.

You should see two jobs:

| Job                          | State   | Meaning                              |
|------------------------------|---------|--------------------------------------|
| `prometheus`                 | **UP**  | Prometheus is scraping itself ✅     |
| `mysawit-identity-service`   | **UP**  | Your Spring Boot app is reachable ✅ |

If the identity service is **DOWN**:

- Click the error message — common causes:
  - `connection refused` → Spring Boot app isn't running on 8081
  - `404` on `/actuator/prometheus` → endpoint not exposed (see step A)
  - Hostname error → on Linux without Docker Desktop, confirm
    `host.docker.internal:host-gateway` is in `extra_hosts`

### C. Query metrics manually in Prometheus

Open **http://localhost:9090/graph** and try these queries one by one
(paste into the "Expression" box, click **Execute**, then **Graph**):

```promql
# 1. Are services alive? (1 = up, 0 = down)
up

# 2. Total HTTP requests handled, by URI
http_server_requests_seconds_count

# 3. Requests per second over the last minute
rate(http_server_requests_seconds_count[1m])

# 4. CPU usage of the JVM process (0.0–1.0)
process_cpu_usage

# 5. Heap memory currently used
jvm_memory_used_bytes{area="heap"}

# 6. Approximate p95 latency for HTTP requests (needs traffic to be meaningful)
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))
```

You won't see much movement until you generate traffic. That's next.

### D. Generate fake traffic and watch graphs move

Pick any endpoint your app actually responds to. The actuator health
endpoint always works:

```bash
# Hit it 200 times back to back
for i in {1..200}; do curl -s http://localhost:8081/actuator/health > /dev/null; done

# Or, on Windows PowerShell:
1..200 | ForEach-Object { Invoke-WebRequest -UseBasicParsing http://localhost:8081/actuator/health | Out-Null }
```

Now re-run query #3 above. You should see a spike in
`rate(http_server_requests_seconds_count[1m])`.

Want to keep traffic going while you watch?

```bash
# Continuous traffic — Ctrl-C to stop
while true; do curl -s http://localhost:8081/actuator/health > /dev/null; sleep 0.1; done
```

### E. Connect Grafana → import a dashboard

1. Open http://localhost:3000 → log in `admin` / `admin`.
2. Left sidebar → **Connections → Data sources**. You should already
   see **Prometheus** marked as default (provisioned automatically).
   Click it → scroll to bottom → **Save & test** → green check ✅.
3. Left sidebar → **Dashboards → New → Import**.
4. Enter dashboard ID **`4701`** (the popular "JVM (Micrometer)"
   community dashboard) → click **Load**.
5. Pick **Prometheus** as the data source → **Import**.
6. At the top, set the `application` variable to
   `mysawit-identity-service`. Graphs should populate immediately.

Alternative dashboard IDs to try later:
- `12900` — Spring Boot Statistics
- `6756` — JVM dashboard (simpler)

---

## Useful metrics cheat sheet

| Metric                                  | What it tells you                          |
|-----------------------------------------|--------------------------------------------|
| `up`                                    | Is the target reachable? (1/0)             |
| `http_server_requests_seconds_count`    | Total HTTP requests handled               |
| `http_server_requests_seconds_sum`      | Total time spent handling them            |
| `http_server_requests_seconds_bucket`   | Histogram buckets → enables p95/p99       |
| `process_cpu_usage`                     | JVM process CPU 0.0–1.0                   |
| `system_cpu_usage`                      | Host CPU 0.0–1.0                          |
| `jvm_memory_used_bytes`                 | Heap / non-heap usage                     |
| `jvm_gc_pause_seconds_count`            | How often the GC paused                   |
| `jvm_threads_live_threads`              | Currently live threads                    |
| `hikaricp_connections_active`           | Active DB connections (Hikari pool)       |
| `logback_events_total`                  | Log events by level (error/warn/info...)  |

---

## Common beginner mistakes

1. **Starting Prometheus before the app.**
   Targets page will show DOWN. Not a bug — just start the app and
   wait 10–15s for the next scrape cycle.

2. **Forgetting `prometheus` in `exposure.include`.**
   Actuator endpoints are opt-in. Without it, `/actuator/prometheus`
   → 404, and Prometheus reports the target as DOWN.

3. **Using `localhost` inside the Prometheus container.**
   Inside the container, `localhost` is the container itself, not
   your host. Use `host.docker.internal` instead.

4. **Expecting `rate()` to work on a single point.**
   `rate()` and `irate()` need ≥ 2 samples in the window. Wait
   ~30 seconds after starting before querying rates.

5. **Querying `http_server_requests_seconds_count` before any HTTP
   call has been made.** The metric only appears after the first
   request. Hit your app at least once.

6. **`docker compose down -v` by accident.**
   The `-v` flag deletes named volumes — you'll lose all scraped
   history and any dashboards you saved manually. Use plain `down`
   to just stop.

7. **Editing `prometheus.yml` and expecting changes immediately.**
   Either restart the container, or hot-reload:
   ```bash
   curl -X POST http://localhost:9090/-/reload
   ```
   (This works because we set `--web.enable-lifecycle` in compose.)

8. **Scraping too aggressively.**
   A 1s scrape interval on a single app is wasteful. 10–15s is the
   sweet spot for local dev.

9. **Looking for metrics that don't exist yet.**
   Hibernate metrics, Tomcat metrics, etc., often require an extra
   line of config (e.g. `hibernate.generate_statistics=true`). Start
   with what's already there before chasing custom metrics.

---

## Where to go next (once this is comfy)

- Add **custom business metrics** with `MeterRegistry` (e.g. count
  successful logins, failed logins).
- Add **Loki** for log aggregation (same Grafana, new data source).
- Add **Tempo** or **Zipkin** for distributed tracing.
- Add **Alertmanager** for "ping me when error rate > X."

But don't add any of these until the basics feel boring. That's the
sign you've actually internalised the model.
