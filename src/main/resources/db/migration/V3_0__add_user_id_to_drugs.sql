-- Add user_id column to drugs table for multi-tenancy support
-- Each drug will belong to a specific user

-- Step 1: Add the column as nullable first
ALTER TABLE drugs ADD COLUMN user_id INTEGER;

-- Step 2: Assign existing drugs to the first user (admin)
UPDATE drugs SET user_id = 1 WHERE user_id IS NULL;

-- Step 3: Make the column NOT NULL
ALTER TABLE drugs ALTER COLUMN user_id SET NOT NULL;

-- Step 4: Add foreign key constraint
ALTER TABLE drugs ADD CONSTRAINT fk_drugs_user
    FOREIGN KEY (user_id) REFERENCES app_user(user_id);

-- Step 5: Create index for efficient user-based queries
CREATE INDEX idx_drugs_user_id ON drugs(user_id);
