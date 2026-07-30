# Requirements

## Problem

Application and device logs arrive in inconsistent formats and are difficult to connect into an
incident narrative. The service must preserve searchable normalized evidence while making every
detection and severity decision explainable.

## Functional requirements

- Accept bounded JSON batches and multipart file imports.
- Parse JSON Lines, RFC 5424-style syslog, common application, and HTTP access logs.
- Normalize timestamps, source, service, severity, event type, trace identifiers, messages, and
  structured attributes.
- Redact recognized credentials before persistence.
- Detect repeated errors, unusual spikes, and suspicious authentication/privilege sequences.
- Group contributing events and return ordered incident timelines.
- Assign severity using deterministic rules with stored evidence.
- Search by time, source, service, severity, and full text.
- Return time-bucketed aggregate statistics.
- Generate deterministic summaries by default.
- Keep optional external summary generation disabled by default and behind an interface.
- Provide OpenAPI documentation, a read-only dashboard, health probes, metrics, and structured
  logs.

## Non-functional requirements

- Java 21 with strict compiler warnings and reproducible Gradle builds.
- PostgreSQL schema controlled by Flyway.
- Bounded memory use for file imports.
- Idempotent import processing and recoverable job states.
- No committed credentials, private logs, or personally identifying sample data.
- Unit, persistence, API, worker, and container smoke tests.
- No throughput or scale claims without measurements.
