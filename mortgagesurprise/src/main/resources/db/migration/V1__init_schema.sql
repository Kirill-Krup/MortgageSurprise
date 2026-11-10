-- V1: Initial Database Schema for MortgageSurprise

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    oauth_provider VARCHAR(50),
    oauth_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

CREATE TABLE IF NOT EXISTS bank_offers (
    id UUID PRIMARY KEY,
    bank_name VARCHAR(255) NOT NULL,
    offer_name VARCHAR(255) NOT NULL,
    annual_interest_rate NUMERIC(6, 2) NOT NULL,
    min_down_payment_percent NUMERIC(5, 2) NOT NULL,
    max_term_months INT NOT NULL,
    min_loan_amount NUMERIC(15, 2),
    max_loan_amount NUMERIC(15, 2),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_bank_offers_active ON bank_offers(active);

CREATE TABLE IF NOT EXISTS calculation_scenarios (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    property_price NUMERIC(15, 2) NOT NULL,
    down_payment NUMERIC(15, 2) NOT NULL,
    loan_amount NUMERIC(15, 2) NOT NULL,
    term_months INT NOT NULL,
    annual_interest_rate NUMERIC(6, 2) NOT NULL,
    payment_type VARCHAR(50) NOT NULL,
    monthly_payment NUMERIC(15, 2) NOT NULL,
    total_interest NUMERIC(15, 2) NOT NULL,
    total_payment NUMERIC(15, 2) NOT NULL,
    mongo_schedule_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_calculation_scenarios_user_id ON calculation_scenarios(user_id);
