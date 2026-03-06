-- Seed template for ADMIN login-only account.
-- IMPORTANT: This is a template. Replace placeholder values before running.
-- Password must be stored as BCrypt hash.

INSERT INTO users (
    id,
    username,
    email,
    name,
    password,
    role,
    account_non_expired,
    account_non_locked,
    credentials_non_expired,
    enabled,
    created_at,
    updated_at
) VALUES (
    'replace-with-admin-id',
    'admin',
    'admin@example.com',
    'Administrator',
    '$2a$10$replaceWithBcryptHash',
    'ADMIN',
    TRUE,
    TRUE,
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO admins (id)
VALUES ('replace-with-admin-id');
