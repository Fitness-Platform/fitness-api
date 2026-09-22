CREATE TABLE users (
     id UUID PRIMARY KEY,
     email VARCHAR(320) NOT NULL,
     password_hash VARCHAR(255) NOT NULL,
     role VARCHAR(20) NOT NULL,
     created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

     CONSTRAINT uk_users_email UNIQUE (email),
     CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'USER'))
);