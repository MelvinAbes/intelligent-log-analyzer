# Synthetic demonstration data

These log files were written for this repository. They represent fictional services, hosts,
addresses, identifiers, and operational events.

| File | Parser | Intended signal |
| --- | --- | --- |
| `application-errors.log` | Application | Five matching payment errors |
| `gateway-access.log` | Combined access | A one-minute request spike after a quiet baseline |
| `security-device.log` | RFC 5424 syslog | Failed sign-ins, success, then privilege change |
| `normal-events.jsonl` | JSON Lines | Routine events that should not create an incident |

The labelled expectations in `evaluation/expected-incidents.json` match detections by rule code and
one evidence field. Run `make evaluate` to ingest all four feeds through the normal parser and
detection path. The machine-readable result is written to
`build/reports/evaluation/incident-detection.json`.
