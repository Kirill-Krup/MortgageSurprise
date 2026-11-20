-- V4: Seed default administrator account

INSERT INTO users (
    id, email, password, first_name, last_name, role, enabled, email_verified, created_at, updated_at
) VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin@mortgagesurprise.com',
    '$2a$10$P5RrtuVsv5M7zR/DZxMRqOH73VBzao3iX6/.tuDjfFDqyO5xsZBdO',
    'Admin',
    'MortgageSurprise',
    'ROLE_ADMIN',
    true,
    true,
    NOW(),
    NOW()
) ON CONFLICT (email) DO UPDATE SET
    role = 'ROLE_ADMIN',
    enabled = true,
    email_verified = true;
