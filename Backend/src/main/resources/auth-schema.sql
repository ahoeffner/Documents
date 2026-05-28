CREATE SCHEMA IF NOT EXISTS documents;

CREATE TABLE IF NOT EXISTS documents.users (
    id       BIGSERIAL PRIMARY KEY,
    username TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS documents.user_tenants (
    user_id BIGINT  NOT NULL REFERENCES documents.users(id) ON DELETE CASCADE,
    tenant  TEXT    NOT NULL,
    admin   BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (user_id, tenant)
);

-- Example (password must be SHA-256 hex of the plaintext):
-- INSERT INTO documents.users (username, password) VALUES ('alice', '<sha256hex>');
-- INSERT INTO documents.user_tenants (user_id, tenant, admin) VALUES (1, 'alex', true);
