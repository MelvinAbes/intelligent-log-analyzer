# Dependencies and Licences

Exact resolved versions are recorded by Gradle dependency locking.

| Dependency | Purpose | Licence |
| --- | --- | --- |
| Java 21 runtime | Application runtime | GPL-2.0 with Classpath Exception |
| Spring Boot and Spring Framework | HTTP, validation, configuration, operations | Apache-2.0 |
| Hibernate ORM | Relational mapping | LGPL-2.1-or-later |
| PostgreSQL | Authoritative data store and full-text search | PostgreSQL Licence |
| PostgreSQL JDBC driver 42.7.12 | Database connectivity | BSD-2-Clause |
| Flyway Community | Schema migrations | Apache-2.0 |
| springdoc | OpenAPI document and browser | Apache-2.0 |
| Thymeleaf | Server-rendered dashboard | Apache-2.0 |
| Micrometer | Metrics | Apache-2.0 |
| Testcontainers | PostgreSQL integration tests | MIT |
| JUnit, AssertJ, and ArchUnit | Verification | EPL-2.0 / Apache-2.0 |
| Gradle and Spotless | Build and formatting | Apache-2.0 |

The project and its original synthetic demonstration data use Apache-2.0.

Build and runtime container images are pinned by digest. The runtime image applies available Alpine
security upgrades during the build. `make audit` scans the locked application dependencies and
`make image-scan` scans the final OS packages and application archive.

The database image replaces the upstream `gosu` executable with version 1.19 compiled from its Go
module using the pinned Go 1.26.5 toolchain. This retains the official entrypoint behaviour while
avoiding known issues in the standard library used by the upstream binary at the time of the scan.
