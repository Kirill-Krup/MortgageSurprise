-- V5: Add microloan configuration table for admin settings

CREATE TABLE IF NOT EXISTS microloan_configs (
    id VARCHAR(50) PRIMARY KEY,
    base_daily_rate NUMERIC(6, 4) NOT NULL,
    vanity_surcharge_rate NUMERIC(6, 4) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

INSERT INTO microloan_configs (id, base_daily_rate, vanity_surcharge_rate, updated_at)
VALUES ('DEFAULT', 0.8000, 0.4000, NOW())
ON CONFLICT (id) DO NOTHING;
