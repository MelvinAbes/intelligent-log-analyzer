#!/usr/bin/env sh
set -eu

base_url="${ILA_BASE_URL:-http://127.0.0.1:8080}"

wait_for_readiness() {
  attempts=0
  while [ "$attempts" -lt 40 ]; do
    if curl --fail --silent "${base_url}/actuator/health/readiness" >/dev/null 2>&1; then
      return 0
    fi
    attempts=$((attempts + 1))
    sleep 1
  done
  printf 'Service did not become ready at %s.\n' "$base_url" >&2
  return 1
}

assert_contains() {
  response="$1"
  expected="$2"
  description="$3"
  case "$response" in
    *"$expected"*) ;;
    *)
      printf '%s did not contain %s.\n' "$description" "$expected" >&2
      return 1
      ;;
  esac
}

wait_for_readiness
status_response="$(curl --fail --silent --show-error "${base_url}/api/v1/status")"
assert_contains "$status_response" '"status":"ok"' "Status response"

openapi_response="$(curl --fail --silent --show-error "${base_url}/v3/api-docs")"
assert_contains "$openapi_response" '"openapi":"' "API description"

headers="$(curl --fail --silent --show-error --head "${base_url}/")"
assert_contains "$headers" "X-Frame-Options: DENY" "Dashboard headers"

printf 'Smoke test passed for %s.\n' "$base_url"
