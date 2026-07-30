# API Examples

The interactive API reference is available at `/swagger-ui.html`; the machine-readable description
is at `/v3/api-docs`.

## Batch ingestion with partial acceptance

```http
POST /api/v1/log-events
Content-Type: application/json
X-Request-ID: example-request-17

{
  "format": "AUTO",
  "source": "api-1",
  "service": "docs-example",
  "entries": [
    {"line": "2026-07-30T18:15:01Z ERROR event_type=payment.failed order=17"},
    {"line": "unsupported line"}
  ]
}
```

Response:

```json
{
  "acceptedCount": 1,
  "rejectedCount": 1,
  "events": [
    {
      "id": "daea3345-b35f-4113-afb6-fda86523dbca",
      "occurredAt": "2026-07-30T18:15:01Z",
      "source": "api-1",
      "service": "docs-example",
      "severity": "ERROR",
      "eventType": "payment.failed",
      "message": "event_type=payment.failed order=17",
      "traceId": null,
      "fingerprint": "b7cf8e0844b7db801f60e4179d58bd12f2d4a36b0f919bdf2582e275dc597ffc",
      "attributes": {
        "order": "17",
        "event_type": "payment.failed"
      }
    }
  ],
  "rejections": [
    {
      "position": 2,
      "code": "unknown_format",
      "message": "Log format could not be detected.",
      "format": "AUTO"
    }
  ]
}
```

The response status is `201 Created`. Rejected entries do not roll back valid entries in the same
bounded batch.

## File import

```bash
curl --request POST http://127.0.0.1:8080/api/v1/log-imports \
  --form file=@samples/logs/security-device.log \
  --form format=SYSLOG
```

The submission returns `202 Accepted` and a job identifier:

```json
{
  "id": "8aec0285-3f1a-4dc9-a9c4-7033f56f10b6",
  "originalFilename": "security-device.log",
  "format": "SYSLOG",
  "status": "QUEUED",
  "totalLines": 0,
  "acceptedLines": 0,
  "rejectedLines": 0,
  "failureCode": null,
  "rejectionSamples": [],
  "createdAt": "2026-07-30T21:32:56.205792846Z",
  "startedAt": null,
  "completedAt": null
}
```

Read progress with `GET /api/v1/log-imports/{id}`. A completed import reports line totals and at
most ten safe rejection samples; it does not return rejected raw log content.

## Event search

```http
GET /api/v1/log-events?from=2026-07-30T18:00:00Z&to=2026-07-30T19:00:00Z&service=payments&severity=ERROR&q=charge%20failed&size=20
```

Search uses half-open time bounds: `from` is inclusive and `to` is exclusive. Page size must be
between 1 and 100.

## Incident investigation

```http
GET /api/v1/incidents?status=OPEN&severity=CRITICAL
GET /api/v1/incidents/{id}
GET /api/v1/incidents/{id}/timeline
```

An incident contains the stable rule code, grouping key, severity, deterministic summary, observed
window, event count, and rule-specific evidence.

Resolve or reopen an incident:

```http
PATCH /api/v1/incidents/{id}
Content-Type: application/json

{"status": "RESOLVED"}
```

Request a configured summary:

```http
POST /api/v1/incidents/{id}/summary
```

With the default configuration, the endpoint explicitly reports the deterministic fallback:

```json
{
  "mode": "deterministic",
  "provider": "disabled",
  "summary": "The stored deterministic incident summary.",
  "fallbackReason": "provider_disabled"
}
```

## Statistics

```http
GET /api/v1/statistics?from=2026-07-30T18:00:00Z&to=2026-07-30T19:00:00Z&bucket=MINUTE
```

Supported buckets are `MINUTE`, `HOUR`, and `DAY`. The maximum range is 31 days.

## Error response

```json
{
  "type": "urn:log-analysis:problem:invalid_query",
  "title": "Bad Request",
  "status": 400,
  "detail": "Page size must be between 1 and 100.",
  "instance": "/api/v1/log-events",
  "code": "invalid_query",
  "requestId": "example-request-17"
}
```
