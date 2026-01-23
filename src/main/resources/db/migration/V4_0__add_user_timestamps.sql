-- Add timestamp columns to app_user table
ALTER TABLE app_user ADD COLUMN created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL;
ALTER TABLE app_user ADD COLUMN last_login TIMESTAMP WITH TIME ZONE;

-- Set created_at for existing users (in case of null values from previous runs)
UPDATE app_user SET created_at = NOW() WHERE created_at IS NULL;

-- Add indexes for performance
CREATE INDEX IF NOT EXISTS idx_app_user_created_at ON app_user(created_at);
CREATE INDEX IF NOT EXISTS idx_app_user_last_login ON app_user(last_login);
CREATE INDEX IF NOT EXISTS idx_app_user_email ON app_user(email);
