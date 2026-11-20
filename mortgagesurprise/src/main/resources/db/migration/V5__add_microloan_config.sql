-- V5: Add microloan configuration table for admin settings

CREATE TABLE IF NOT EXISTS microloan_configs (
    id VARCHAR(50) PRIMARY KEY,
    bank_interest NUMERIC(10, 4) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

INSERT INTO microloan_configs (id, bank_interest, created_at, updated_at)
VALUES ('DEFAULT', 2000.0000, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;