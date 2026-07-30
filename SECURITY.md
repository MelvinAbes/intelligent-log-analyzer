# Security Policy

## Reporting

Do not report suspected vulnerabilities in a public issue. Use the repository's private security
advisory feature. Include the affected revision, impact, minimal reproduction, and suggested
mitigation when available.

Never attach production logs, credentials, access tokens, or personal data. Use a synthetic
reproduction.

## Scope and current posture

The application is intended for local evaluation and trusted development environments. It does not
yet implement authentication or tenant isolation and should not be exposed directly to an
untrusted network.

The project:

- binds the Compose HTTP port to loopback;
- does not expose PostgreSQL to the host;
- redacts recognized credential patterns before persistence;
- limits file size, line length, batch size, and search ranges;
- runs the application container as a non-root user with a read-only root filesystem;
- drops Linux capabilities and enables `no-new-privileges`;
- scans dependencies, container packages, and repository secrets in continuous integration.

Rotate any credential immediately if it is accidentally disclosed, then remove it from the current
revision without rewriting shared history unless coordinated incident response requires otherwise.
