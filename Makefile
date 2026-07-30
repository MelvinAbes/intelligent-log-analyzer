GRADLE := ./gradlew

.PHONY: format format-check lint test test-unit test-integration evaluate check build run compose-up compose-down smoke demo audit image-scan secret-scan dependencies-lock

format:
	$(GRADLE) spotlessApply

format-check:
	$(GRADLE) spotlessCheck

lint:
	$(GRADLE) checkstyleMain checkstyleTest

test:
	$(GRADLE) test integrationTest

test-unit:
	$(GRADLE) test

test-integration:
	$(GRADLE) integrationTest

evaluate:
	$(GRADLE) evaluationTest

check:
	$(GRADLE) spotlessCheck check

build:
	$(GRADLE) clean build

run:
	$(GRADLE) bootRun

compose-up:
	docker compose up --detach --build --wait

compose-down:
	docker compose down

smoke:
	./scripts/smoke-test.sh

demo:
	./scripts/load-demo-data.sh

audit:
	docker run --rm -v trivy_cache:/root/.cache -v "$(CURDIR):/workspace" -w /workspace \
		aquasec/trivy:0.72.0@sha256:cffe3f5161a47a6823fbd23d985795b3ed72a4c806da4c4df16266c02accdd6f \
		fs --scanners vuln --severity HIGH,CRITICAL --exit-code 1 .

image-scan:
	docker run --rm -v trivy_cache:/root/.cache -v /var/run/docker.sock:/var/run/docker.sock \
		aquasec/trivy:0.72.0@sha256:cffe3f5161a47a6823fbd23d985795b3ed72a4c806da4c4df16266c02accdd6f \
		image --severity HIGH,CRITICAL --exit-code 1 intelligent-log-analyzer-application
	docker run --rm -v trivy_cache:/root/.cache -v /var/run/docker.sock:/var/run/docker.sock \
		aquasec/trivy:0.72.0@sha256:cffe3f5161a47a6823fbd23d985795b3ed72a4c806da4c4df16266c02accdd6f \
		image --severity HIGH,CRITICAL --exit-code 1 intelligent-log-analyzer-postgres:local

secret-scan:
	gitleaks dir . --redact --no-banner

dependencies-lock:
	$(GRADLE) dependencies --write-locks
