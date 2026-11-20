-- V10: Create user passport data table with AES-256 encrypted fields and SHA-256 hash

CREATE TABLE IF NOT EXISTS user_passport_data (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    full_name VARCHAR(255) NOT NULL,
    series_number_encrypted VARCHAR(512) NOT NULL,
    passport_hash VARCHAR(64) NOT NULL,
    issue_date VARCHAR(32),
    department_code VARCHAR(32),
    issued_by VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_user_passport_user UNIQUE (user_id),
    CONSTRAINT uq_user_passport_hash UNIQUE (passport_hash)
);

CREATE INDEX IF NOT EXISTS idx_user_passport_user ON user_passport_data(user_id);
CREATE INDEX IF NOT EXISTS idx_user_passport_hash ON user_passport_data(passport_hash);
