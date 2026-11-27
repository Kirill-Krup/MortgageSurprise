CREATE TABLE IF NOT EXISTS exchange_rates (
  id UUID PRIMARY KEY,
  base_currency VARCHAR(10) NOT NULL,
    target_currency VARCHAR(10) NOT NULL,
    rate NUMERIC(12, 4) NOT NULL,
    source VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
                             );

CREATE INDEX IF NOT EXISTS idx_exchange_rates_target ON exchange_rates(target_currency);
CREATE INDEX IF NOT EXISTS idx_exchange_rates_updated_at ON exchange_rates(updated_at DESC);

INSERT INTO exchange_rates (id, base_currency, target_currency, rate, source, updated_at)
VALUES
    (gen_random_uuid(), 'RUB', 'USD', 92.5000, 'SEED', NOW()),
    (gen_random_uuid(), 'RUB', 'EUR', 100.2000, 'SEED', NOW()),
    (gen_random_uuid(), 'RUB', 'BYN', 28.4000, 'SEED', NOW())
    ON CONFLICT (id) DO NOTHING;