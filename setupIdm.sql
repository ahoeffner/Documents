CREATE SCHEMA IF NOT EXISTS idm;

CREATE TABLE idm.users
(
    id       BIGSERIAL    PRIMARY KEY,
    username TEXT         NOT NULL UNIQUE,
    password TEXT         NOT NULL
);

CREATE TABLE idm.user_tenants
(
    user_id  BIGINT       NOT NULL REFERENCES idm.users(id) ON DELETE CASCADE,
    tenant   TEXT         NOT NULL,
    admin    BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (user_id, tenant)
);