# Design Decisions

## Modular monolith

Parsing, detection, search, persistence, and presentation remain separate modules inside one
deployable service. The workload does not justify a distributed topology or a separate analysis
service.

## PostgreSQL-backed file jobs

File imports use leased relational jobs and bounded batches. This supports restart recovery and
multi-instance claiming with fewer operational dependencies than a broker.

## Deterministic analysis first

Rules assign severity and build summaries from stored evidence. Optional provider-assisted prose
is disabled by default and cannot alter the incident facts.

## Native PostgreSQL search

Messages use a generated `tsvector` with a GIN index. Structured filters and aggregations stay in
SQL, keeping the initial system reproducible without a second search store.

## Store normalized evidence

Raw lines are bounded and redacted before storage. Variable values are canonicalized only for the
fingerprint; the sanitized message remains available for investigation.
