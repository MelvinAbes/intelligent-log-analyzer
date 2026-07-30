CREATE TABLE log_imports (
    id UUID PRIMARY KEY,
    original_filename VARCHAR(255) NOT NULL,
    format VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    storage_key VARCHAR(255) NOT NULL UNIQUE,
    source_hint VARCHAR(128),
    service_hint VARCHAR(128),
    total_lines INTEGER NOT NULL DEFAULT 0,
    accepted_lines INTEGER NOT NULL DEFAULT 0,
    rejected_lines INTEGER NOT NULL DEFAULT 0,
    failure_code VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    lease_until TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_log_imports_format
        CHECK (format IN ('AUTO', 'JSON_LINES', 'SYSLOG', 'APPLICATION', 'ACCESS')),
    CONSTRAINT ck_log_imports_status
        CHECK (status IN ('QUEUED', 'PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT ck_log_imports_counts
        CHECK (
            total_lines >= 0
            AND accepted_lines >= 0
            AND rejected_lines >= 0
            AND accepted_lines + rejected_lines <= total_lines
        )
);

CREATE INDEX ix_log_imports_claim
    ON log_imports (status, lease_until, created_at);

CREATE TABLE log_events (
    id UUID PRIMARY KEY,
    import_id UUID REFERENCES log_imports(id) ON DELETE SET NULL,
    line_number INTEGER,
    occurred_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    source VARCHAR(128) NOT NULL,
    service VARCHAR(128) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    event_type VARCHAR(128),
    message VARCHAR(4096) NOT NULL,
    trace_id VARCHAR(128),
    subject_id VARCHAR(128),
    external_id VARCHAR(128),
    fingerprint VARCHAR(64) NOT NULL,
    attributes JSONB NOT NULL DEFAULT '{}'::jsonb,
    raw_line VARCHAR(16384),
    search_vector TSVECTOR GENERATED ALWAYS AS (
        to_tsvector(
            'simple'::regconfig,
            coalesce(service, '') || ' ' ||
            coalesce(source, '') || ' ' ||
            coalesce(event_type, '') || ' ' ||
            coalesce(message, '')
        )
    ) STORED,
    CONSTRAINT ck_log_events_severity
        CHECK (severity IN ('TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR', 'FATAL')),
    CONSTRAINT ck_log_events_line_number
        CHECK (line_number IS NULL OR line_number > 0)
);

CREATE UNIQUE INDEX uq_log_events_import_line
    ON log_events (import_id, line_number)
    WHERE import_id IS NOT NULL AND line_number IS NOT NULL;

CREATE UNIQUE INDEX uq_log_events_source_external
    ON log_events (source, external_id)
    WHERE external_id IS NOT NULL;

CREATE INDEX ix_log_events_occurred_at ON log_events (occurred_at DESC);
CREATE INDEX ix_log_events_service_time ON log_events (service, occurred_at DESC);
CREATE INDEX ix_log_events_source_time ON log_events (source, occurred_at DESC);
CREATE INDEX ix_log_events_severity_time ON log_events (severity, occurred_at DESC);
CREATE INDEX ix_log_events_fingerprint_time ON log_events (fingerprint, occurred_at DESC);
CREATE INDEX ix_log_events_trace_id ON log_events (trace_id) WHERE trace_id IS NOT NULL;
CREATE INDEX ix_log_events_subject_time
    ON log_events (subject_id, occurred_at DESC)
    WHERE subject_id IS NOT NULL;
CREATE INDEX ix_log_events_search ON log_events USING GIN (search_vector);
CREATE INDEX ix_log_events_attributes ON log_events USING GIN (attributes);

CREATE TABLE incidents (
    id UUID PRIMARY KEY,
    rule_code VARCHAR(64) NOT NULL,
    grouping_key VARCHAR(255) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary VARCHAR(4096) NOT NULL,
    window_start TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ NOT NULL,
    detected_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    event_count INTEGER NOT NULL,
    evidence JSONB NOT NULL DEFAULT '{}'::jsonb,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_incidents_rule_group_window
        UNIQUE (rule_code, grouping_key, window_start),
    CONSTRAINT ck_incidents_severity
        CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_incidents_status
        CHECK (status IN ('OPEN', 'RESOLVED')),
    CONSTRAINT ck_incidents_event_count CHECK (event_count > 0),
    CONSTRAINT ck_incidents_time_order CHECK (started_at <= ended_at)
);

CREATE INDEX ix_incidents_status_severity_time
    ON incidents (status, severity, started_at DESC);
CREATE INDEX ix_incidents_rule_time
    ON incidents (rule_code, started_at DESC);
CREATE INDEX ix_incidents_evidence ON incidents USING GIN (evidence);

CREATE TABLE incident_events (
    id UUID PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    event_id UUID NOT NULL REFERENCES log_events(id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL,
    CONSTRAINT uq_incident_events_incident_event UNIQUE (incident_id, event_id)
);

CREATE INDEX ix_incident_events_event ON incident_events (event_id);
