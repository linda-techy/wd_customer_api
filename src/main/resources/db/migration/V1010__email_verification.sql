-- Email verification support for customer registration.

-- Verification tokens
CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES customer_users(id),
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_email_verification_token ON email_verification_tokens(token);

-- Track verification status on customer_users
ALTER TABLE customer_users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN DEFAULT TRUE;
-- Default TRUE so existing users are grandfathered in
