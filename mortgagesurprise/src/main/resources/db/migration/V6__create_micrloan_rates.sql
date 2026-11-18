-- V6: Add microloan rates table

CREATE TABLE IF NOT EXISTS microloan_rates (
    id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name VARCHAR(15) NOT NULL,
    daily_rate NUMERIC(6, 4) NOT NULL,
    min_day_required INTEGER NOT NULL,
    max_day_required INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
