-- V9: Seed default microloan rates

INSERT INTO microloan_rates (name, daily_rate, min_day_required, max_day_required, created_at, updated_at)
VALUES
    ('Стандарт', 0.8000, 7, 30, NOW(), NOW()),
    ('Лайт', 0.6000, 14, 30, NOW(), NOW()),
    ('Срочный', 1.0000, 3, 14, NOW(), NOW())
ON CONFLICT DO NOTHING;
