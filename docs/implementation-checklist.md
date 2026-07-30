# Implementation Checklist

## Foundation

- [x] Confirm Java, Docker, Git, and GitHub tooling.
- [x] Record requirements, architecture, trade-offs, and licence considerations.
- [x] Initialize Java 21 and Spring Boot configuration.
- [x] Configure the Gradle wrapper, dependency locking, formatting, linting, and tests.
- [ ] Add shared API error handling, request IDs, and security headers.

## Ingestion and persistence

- [ ] Add import, event, incident, and timeline models.
- [ ] Add Flyway migrations and PostgreSQL repositories.
- [ ] Implement supported parsers, redaction, normalization, and fingerprinting.
- [ ] Add bounded REST ingestion and recoverable file imports.

## Analysis and queries

- [ ] Implement grouping, repeated-error, spike, and suspicious-sequence rules.
- [ ] Add deterministic severity and summaries.
- [ ] Add filtered full-text search, timelines, and statistics.
- [ ] Add the optional disabled-by-default summary adapter.

## Delivery

- [ ] Add the incident dashboard and original synthetic data.
- [ ] Add reproducible detector evaluation.
- [ ] Add Docker Compose, smoke tests, and continuous integration.
- [ ] Run all quality, dependency, secret, and container checks.
- [ ] Capture functioning desktop and mobile screenshots.
- [ ] Complete documentation and private interview notes.
