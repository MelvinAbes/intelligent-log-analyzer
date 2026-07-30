ALTER TABLE log_imports
    ADD COLUMN rejection_samples JSONB NOT NULL DEFAULT '[]'::jsonb;
