#!/usr/bin/env sh
set -eu

base_url="${ILA_BASE_URL:-http://127.0.0.1:8080}"
sample_root="${1:-samples/logs}"

submit_import() {
  file_path="$1"
  format="$2"
  service="${3:-}"
  if [ -n "$service" ]; then
    response="$(curl --fail --silent --show-error \
      -F "file=@${file_path}" -F "format=${format}" -F "service=${service}" \
      "${base_url}/api/v1/log-imports")"
  else
    response="$(curl --fail --silent --show-error \
      -F "file=@${file_path}" -F "format=${format}" \
      "${base_url}/api/v1/log-imports")"
  fi
  identifier="$(printf '%s' "$response" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')"
  if [ -z "$identifier" ]; then
    printf 'Could not read import identifier from response: %s\n' "$response" >&2
    exit 1
  fi
  printf '%s\n' "$identifier"
}

wait_for_import() {
  identifier="$1"
  attempts=0
  while [ "$attempts" -lt 40 ]; do
    response="$(curl --fail --silent --show-error "${base_url}/api/v1/log-imports/${identifier}")"
    case "$response" in
      *'"status":"COMPLETED"'*)
        return 0
        ;;
      *'"status":"FAILED"'*)
        printf 'Import %s failed: %s\n' "$identifier" "$response" >&2
        return 1
        ;;
    esac
    attempts=$((attempts + 1))
    sleep 1
  done
  printf 'Import %s did not finish within 40 seconds.\n' "$identifier" >&2
  return 1
}

application_id="$(submit_import "${sample_root}/application-errors.log" APPLICATION)"
gateway_id="$(submit_import "${sample_root}/gateway-access.log" ACCESS edge-gateway)"
security_id="$(submit_import "${sample_root}/security-device.log" SYSLOG)"
normal_id="$(submit_import "${sample_root}/normal-events.jsonl" JSON_LINES)"

wait_for_import "$application_id"
wait_for_import "$gateway_id"
wait_for_import "$security_id"
wait_for_import "$normal_id"

printf 'Loaded all demonstration feeds. Incident response:\n'
curl --fail --silent --show-error "${base_url}/api/v1/incidents?size=10"
printf '\n'
