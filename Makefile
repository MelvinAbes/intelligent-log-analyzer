GRADLE := ./gradlew

.PHONY: format format-check lint test test-unit test-integration check build run audit secret-scan dependencies-lock

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

check:
	$(GRADLE) spotlessCheck check

build:
	$(GRADLE) clean build

run:
	$(GRADLE) bootRun

audit:
	$(GRADLE) dependencyInsight --dependency spring-core --configuration runtimeClasspath

secret-scan:
	gitleaks dir . --redact --no-banner

dependencies-lock:
	$(GRADLE) dependencies --write-locks
