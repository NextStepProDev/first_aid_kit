-- Add account lockout fields to app_user table
ALTER TABLE app_user
    ADD COLUMN failed_login_attempts INTEGER DEFAULT 0 NOT NULL,
    ADD COLUMN locked_until TIMESTAMP WITH TIME ZONE;
