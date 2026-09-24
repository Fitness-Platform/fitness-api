CREATE TABLE password_reset_tokens (
     id UUID PRIMARY KEY,
     user_id UUID NOT NULL,
     token_hash VARCHAR(64) NOT NULL,
     expires_at TIMESTAMPTZ NOT NULL,
     created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

     CONSTRAINT fk_password_reset_tokens_user
         FOREIGN KEY (user_id)
         REFERENCES users(id)
         ON DELETE CASCADE,

         CONSTRAINT uk_password_reset_tokens_token_hash
             UNIQUE (token_hash),

         CONSTRAINT uk_password_reset_tokens_user
             UNIQUE (user_id)
);