CREATE TABLE email_verification_token (
    token_id SERIAL PRIMARY KEY,
    token VARCHAR(64) NOT NULL UNIQUE,
    user_id INTEGER NOT NULL REFERENCES app_user(user_id) ON DELETE CASCADE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    verified_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_email_verification_token_token ON email_verification_token(token);
CREATE INDEX idx_email_verification_token_user_id ON email_verification_token(user_id);
CREATE INDEX idx_email_verification_token_expires_at ON email_verification_token(expires_at);
