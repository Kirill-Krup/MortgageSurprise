CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE subject (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trust_score INT NOT NULL DEFAULT 700 CHECK (trust_score >= 0 AND trust_score <= 1000),
    hard_block BOOLEAN NOT NULL DEFAULT FALSE,
    block_reason VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE subject_document (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_id UUID NOT NULL REFERENCES subject(id) ON DELETE CASCADE,
    document_type VARCHAR(32) NOT NULL,
    document_hash VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_subject_document_subject_id ON subject_document(subject_id);

CREATE TABLE trust_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_id UUID NOT NULL REFERENCES subject(id) ON DELETE RESTRICT,
    event_key VARCHAR(128) NOT NULL UNIQUE,
    event_type VARCHAR(50) NOT NULL,
    delta INT NOT NULL,
    score_after INT NOT NULL CHECK (score_after >= 0 AND score_after <= 1000),
    source VARCHAR(64) NOT NULL,
    details JSONB,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_trust_event_subject_created ON trust_event(subject_id, created_at DESC);
