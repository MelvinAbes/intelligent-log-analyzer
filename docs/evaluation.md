# Incident Detection Evaluation

The evaluation is a deterministic regression check for the included detection rules. It is not a
claim about general anomaly-detection accuracy.

## Dataset

Thirty original synthetic events are split across four parser formats:

- five matching application errors;
- fifteen access events containing a quiet five-minute baseline and a ten-event minute;
- five syslog security events forming a failed-authentication, success, privilege-change sequence;
- five routine JSON Lines events.

`samples/evaluation/expected-incidents.json` contains three labels. Each label identifies a rule
code and one expected evidence value. This avoids matching generated incident identifiers or prose.

## Method

`make evaluate`:

1. starts an isolated PostgreSQL container through Testcontainers;
2. applies the real Flyway migrations;
3. parses and persists each feed through `LogEventIngestionService`;
4. runs the configured production detectors and incident reconciler;
5. matches actual incidents against the labels;
6. calculates precision, recall, and F1;
7. writes `build/reports/evaluation/incident-detection.json`.

An unexpected incident is a false positive. A label without a matching rule and evidence value is a
false negative.

## Reproduced result

The local run on 2026-07-30 produced:

| Measure | Result |
| --- | ---: |
| Accepted events | 30 |
| Expected incidents | 3 |
| Detected incidents | 3 |
| True positives | 3 |
| False positives | 0 |
| False negatives | 0 |
| Precision | 1.0 |
| Recall | 1.0 |
| F1 | 1.0 |

The test asserts these values, so a rule or parser regression fails the build.

## Threats to validity

- The dataset is small and designed around the three implemented rules.
- It does not cover seasonal traffic, clock drift, missing fields, or adversarial input.
- Rule thresholds are the repository defaults.
- A perfect result on this dataset does not predict performance on unrelated logs.

A broader evaluation would require independently labelled, legally usable data and separate
threshold tuning and holdout sets.
