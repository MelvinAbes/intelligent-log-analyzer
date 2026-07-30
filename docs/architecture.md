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

## Import sequence

```mermaid
sequenceDiagram
    participant Client
    participant API
    participant Database
    participant Worker
    participant Storage
    participant Analysis

    Client->>API: Multipart log file
    API->>Storage: Save generated storage key
    API->>Database: Create QUEUED import
    API-->>Client: 202 Accepted and import ID
    Worker->>Database: Claim job and lease row
    loop bounded line batches
        Worker->>Storage: Read next lines
        Worker->>Analysis: Parse, redact, persist, detect
        Analysis->>Database: Events, incidents, timeline links
        Worker->>Database: Update progress and lease
    end
    Worker->>Database: Mark COMPLETED
```

Detection windows are half-open for repeated errors and spikes. This prevents an event on an exact
boundary from contributing to two windows. Sequence detection includes the privilege-change event
at the end of its evidence interval.
