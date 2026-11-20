-- V9: Seed default microloan rates

INSERT INTO microloan_rates (name, daily_rate, min_day_required, max_day_required, created_at, updated_at)
VALUES
    ('Турбо', 2, 0, 14, NOW(), NOW()),
    ('На взлет', 1.5, 14, 30, NOW(), NOW())
ON CONFLICT DO NOTHING;
