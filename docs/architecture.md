# Architecture

The service is a modular Spring Boot application. PostgreSQL is authoritative for import jobs,
normalized events, incidents, rule evidence, and timelines.

```mermaid
flowchart LR
    Client["API client or dashboard"] --> API["Spring MVC API"]
    API --> Validate["Validate and redact"]
    Validate --> Parse["Parser registry"]
    Parse --> Normalize["Normalize events"]
    Normalize --> PostgreSQL["PostgreSQL"]
    File["Multipart file"] --> Job["Persistent import job"]
    Job --> Worker["Leased import worker"]
    Worker --> Parse
    PostgreSQL --> Analysis["Incident analysis"]
    Analysis --> Rules["Repeated, spike, and sequence rules"]
    Rules --> Incidents["Incident reconciliation"]
    Incidents --> PostgreSQL
    PostgreSQL --> Search["Full-text and filtered search"]
    PostgreSQL --> Stats["SQL statistics"]
    PostgreSQL --> Summary["Deterministic summary"]
    Summary --> Provider["Optional summary provider"]
    Search --> API
    Stats --> API
    Summary --> API
```

REST batches are intentionally bounded and synchronous. File imports are asynchronous: a
database-backed worker claims jobs using a lease, streams lines in batches, and records accepted
and rejected counts. This avoids adding queue infrastructure while retaining recoverable work.

Detectors return immutable candidates containing a rule code, grouping key, event identifiers,
observed values, and thresholds. Incident reconciliation persists that evidence and links the
timeline events. Provider-assisted text cannot change rule evidence or severity.
