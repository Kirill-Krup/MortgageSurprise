-- V8: Add user A/B test variant tracking

CREATE TABLE IF NOT EXISTS user_ab_test_variants (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    experiment_name VARCHAR(100) NOT NULL,
    color VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_user_ab_experiment UNIQUE (user_id, experiment_name)
);

CREATE INDEX IF NOT EXISTS idx_user_ab_test_variants_user ON user_ab_test_variants(user_id);
CREATE INDEX IF NOT EXISTS idx_user_ab_test_variants_exp ON user_ab_test_variants(experiment_name);
