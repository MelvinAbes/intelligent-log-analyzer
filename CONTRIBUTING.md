# Contributing

Contributions should keep detections explainable and preserve the separation between parsing,
analysis, persistence, queries, and presentation.

## Local workflow

1. Create a focused branch.
2. Add or update tests with behavioural changes.
3. Use a Flyway migration for schema changes.
4. Run the relevant checks.

```bash
make format
make check
make audit
make secret-scan
```

Container changes also require:

```bash
docker compose build
docker compose up --detach --wait
make smoke
make image-scan
```

## Test data

Use synthetic events with reserved example addresses and fictional identifiers. Never add
production logs, credentials, tokens, customer names, email addresses, or other personal data to
fixtures, issues, or screenshots.

New detection rules should include:

- a stable rule code and grouping key;
- explicit thresholds and stored evidence;
- a deterministic severity decision;
- positive and boundary tests;
- at least one labelled evaluation example when suitable.

Keep commits small enough to review and use imperative, specific messages.
