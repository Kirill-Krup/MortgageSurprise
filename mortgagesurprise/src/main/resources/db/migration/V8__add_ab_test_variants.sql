CREATE TABLE IF NOT EXISTS ab_test_variants
(
    id                  INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    experiment_name     VARCHAR(100)             NOT NULL,
    variant_name        VARCHAR(50)              NOT NULL,
    variant_probability INT                      NOT NULL,
    version             INT                      NOT NULL DEFAULT 0,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_ab_variant_probability CHECK (variant_probability BETWEEN 0 AND 100),
    CONSTRAINT uq_ab_variant UNIQUE (experiment_name, version, variant_name)
);

CREATE TABLE IF NOT EXISTS user_ab_test_variants
(
    id                 UUID PRIMARY KEY,
    user_id            UUID                     NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    ab_test_variant_id INTEGER                  NOT NULL REFERENCES ab_test_variants (id) ON DELETE CASCADE,
    experiment_name    VARCHAR(100)             NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_ab_experiment UNIQUE (user_id, experiment_name)
);

CREATE INDEX IF NOT EXISTS idx_user_ab_variant ON user_ab_test_variants (ab_test_variant_id);
CREATE INDEX IF NOT EXISTS idx_user_ab_experiment ON user_ab_test_variants (experiment_name);

INSERT INTO ab_test_variants (experiment_name, variant_name, variant_probability, version)
VALUES ('mortgage_calculate_button', 'blue', 50, 0),
       ('mortgage_calculate_button', 'emerald', 50, 0)
ON CONFLICT (experiment_name, version, variant_name) DO NOTHING;
