-- V8: Add user A/B test variant tracking

CREATE TABLE IF NOT EXISTS user_ab_test_variants
(
    id         UUID PRIMARY KEY,
    user_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    variant_id VARCHAR(100) NOT NULL REFERENCES ab_test_variants (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_variant_ab_test_variants_user ON user_ab_test_variants (variant_id);
