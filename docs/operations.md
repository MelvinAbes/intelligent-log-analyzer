# Operations

## Health and metrics

- `/actuator/health/liveness` reports process liveness.
- `/actuator/health/readiness` includes database readiness.
- `/actuator/prometheus` exposes HTTP, JVM, ingestion, rejection, and incident counters.
- `/api/v1/status` provides a lightweight service response for clients.

Console logs use structured JSON. `X-Request-ID` is accepted when it contains 8–64 safe characters;
otherwise the service creates a UUID. The response echoes the identifier and the logging context
includes it.

## Import recovery

The worker claims a queued or expired job with a PostgreSQL row lock and records a lease. It reads
at most the configured batch size in one transaction and updates accepted and rejected totals.
Progress is resumable from the recorded line count after a worker interruption.

The import volume must be shared when multiple application instances claim jobs. For a deployment
without shared local storage, replace it with object storage before adding instances.

## Summary provider

The default `ILA_SUMMARY_PROVIDER=disabled` path has no network dependency. Set it to `chat-http`
only with a compatible endpoint:

```bash
ILA_SUMMARY_PROVIDER=chat-http
ILA_SUMMARY_BASE_URL=http://host.docker.internal:11434/v1
ILA_SUMMARY_MODEL=qwen2.5:3b
ILA_SUMMARY_API_TOKEN=
ILA_SUMMARY_TIMEOUT=PT15S
```

Only redacted, persisted evidence and at most 30 linked events are sent. A timeout, invalid response,
or HTTP failure returns the deterministic summary with `provider_unavailable`.

## Backup and cleanup

PostgreSQL is authoritative. Back up its volume using normal PostgreSQL tooling. Import objects can
be removed after their jobs reach a terminal state and the configured retention period has passed;
automatic retention is not implemented.

For local cleanup:

```bash
docker compose down --volumes
```

This permanently removes only this Compose project's database and import volumes.
