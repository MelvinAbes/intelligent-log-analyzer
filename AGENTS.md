# Repository Development Rules

## Scope

This repository contains a Java 21 service for deterministic log normalization, incident
detection, search, statistics, and summaries.

## Architecture

- Keep HTTP, application, domain, and persistence concerns separate.
- Keep parsing, grouping, detection, severity assignment, and summary generation independently
  testable.
- PostgreSQL is authoritative for imports, normalized events, incidents, and timelines.
- Keep optional external summary logic behind a narrow application port and disabled by default.
- Do not add a separate service unless a measured requirement justifies the operational cost.

## Implementation

- Prefer immutable records and precise domain names.
- Validate sizes, timestamps, identifiers, and line formats at system boundaries.
- Redact recognized credentials before persistence or logging.
- Keep detection decisions deterministic and store their evidence.
- Use environment variables for configuration and never commit `.env` or credentials.
- Add a Flyway migration for every schema change.
- Add focused tests with each behaviour change.

## Verification

Before a coherent commit, run the relevant subset of:

```bash
make format
make check
make audit
make secret-scan
```

Before publication, also run the container build, Compose smoke test, image scan, and
repository-language review.
