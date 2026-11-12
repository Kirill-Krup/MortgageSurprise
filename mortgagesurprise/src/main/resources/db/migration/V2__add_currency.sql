-- V2: Add currency column to calculation_scenarios and bank_offers

ALTER TABLE calculation_scenarios ADD COLUMN IF NOT EXISTS currency VARCHAR(10) NOT NULL DEFAULT 'RUB';
ALTER TABLE bank_offers ADD COLUMN IF NOT EXISTS currency VARCHAR(10) NOT NULL DEFAULT 'RUB';
