-- V11: Add loan application fields (age, country, city, gender) to user_passport_data

ALTER TABLE user_passport_data
    ADD COLUMN IF NOT EXISTS age INTEGER,
    ADD COLUMN IF NOT EXISTS country VARCHAR(100),
    ADD COLUMN IF NOT EXISTS city VARCHAR(100),
    ADD COLUMN IF NOT EXISTS gender VARCHAR(32);
