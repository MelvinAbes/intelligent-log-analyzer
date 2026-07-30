# Implementation Checklist

## Foundation

- [x] Confirm Java, Docker, Git, and GitHub tooling.
- [x] Record requirements, architecture, trade-offs, and licence considerations.
- [x] Initialize Java 21 and Spring Boot configuration.
- [x] Configure the Gradle wrapper, dependency locking, formatting, linting, and tests.
- [x] Add shared API error handling, request IDs, and security headers.

## Ingestion and persistence

- [x] Add import, event, incident, and timeline models.
- [x] Add Flyway migrations and PostgreSQL repositories.
- [x] Implement supported parsers, redaction, normalization, and fingerprinting.
- [x] Add bounded REST ingestion and recoverable file imports.

## Analysis and queries

- [x] Implement grouping, repeated-error, spike, and suspicious-sequence rules.
- [x] Add deterministic severity and summaries.
- [x] Add filtered full-text search, timelines, and statistics.
- [x] Add the optional disabled-by-default summary adapter.

## Delivery

- [x] Add the incident dashboard and original synthetic data.
- [x] Add reproducible detector evaluation.
- [x] Add Docker Compose, smoke tests, and continuous integration.
- [x] Run all quality, dependency, secret, and container checks.
- [x] Capture functioning desktop and mobile screenshots.
- [x] Complete repository documentation.
- [x] Complete private interview notes and final publication review.
