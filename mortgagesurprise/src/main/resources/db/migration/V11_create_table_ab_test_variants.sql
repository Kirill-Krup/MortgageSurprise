-- 11: Add A/B test variant

CREATE TABLE IF NOT EXISTS ab_test_variants
(
    id                  INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    experiment_name     VARCHAR(100)             NOT NULL,
    variant_name        VARCHAR(50)              NOT NULL,
    variant_probability INT                      NOT NULL,
    version             VARCHAR(50)              NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_variant_name_ab_experiment UNIQUE (variant_name, experiment_name),
    CONSTRAINT uq_version_ab_experiment UNIQUE (version, experiment_name)
);

CREATE INDEX IF NOT EXISTS idx_user_ab_test_variants_exp ON user_ab_test_variants (experiment_name, version);
