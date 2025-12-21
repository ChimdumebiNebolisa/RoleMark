-- V2: Change users table id from BIGSERIAL to UUID
-- This migration handles the transition from BIGINT to UUID for user IDs

-- Step 1: Drop foreign key constraints that reference users.id
ALTER TABLE roles DROP CONSTRAINT IF EXISTS roles_user_id_fkey;
ALTER TABLE resumes DROP CONSTRAINT IF EXISTS resumes_user_id_fkey;
ALTER TABLE evaluations DROP CONSTRAINT IF EXISTS evaluations_user_id_fkey;

-- Step 2: Add new UUID column to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS id_uuid UUID DEFAULT gen_random_uuid();

-- Step 3: Generate UUIDs for existing users (if any)
UPDATE users SET id_uuid = gen_random_uuid() WHERE id_uuid IS NULL;

-- Step 4: Make id_uuid NOT NULL
ALTER TABLE users ALTER COLUMN id_uuid SET NOT NULL;

-- Step 5: Add UUID columns to referencing tables
ALTER TABLE roles ADD COLUMN IF NOT EXISTS user_id_uuid UUID;
ALTER TABLE resumes ADD COLUMN IF NOT EXISTS user_id_uuid UUID;
ALTER TABLE evaluations ADD COLUMN IF NOT EXISTS user_id_uuid UUID;

-- Step 6: Populate UUID foreign keys from old BIGINT references
UPDATE roles r SET user_id_uuid = u.id_uuid FROM users u WHERE r.user_id = u.id;
UPDATE resumes r SET user_id_uuid = u.id_uuid FROM users u WHERE r.user_id = u.id;
UPDATE evaluations e SET user_id_uuid = u.id_uuid FROM users u WHERE e.user_id = u.id;

-- Step 7: Drop old columns from referencing tables
ALTER TABLE roles DROP COLUMN IF EXISTS user_id;
ALTER TABLE resumes DROP COLUMN IF EXISTS user_id;
ALTER TABLE evaluations DROP COLUMN IF EXISTS user_id;

-- Step 8: Rename UUID columns to original names
ALTER TABLE roles RENAME COLUMN user_id_uuid TO user_id;
ALTER TABLE resumes RENAME COLUMN user_id_uuid TO user_id;
ALTER TABLE evaluations RENAME COLUMN user_id_uuid TO user_id;

-- Step 9: Make foreign key columns NOT NULL
ALTER TABLE roles ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE resumes ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE evaluations ALTER COLUMN user_id SET NOT NULL;

-- Step 10: Drop old id column from users and rename id_uuid
ALTER TABLE users DROP COLUMN IF EXISTS id;
ALTER TABLE users RENAME COLUMN id_uuid TO id;

-- Step 11: Set id as PRIMARY KEY
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_pkey;
ALTER TABLE users ADD PRIMARY KEY (id);

-- Step 12: Recreate foreign key constraints with UUID
ALTER TABLE roles ADD CONSTRAINT roles_user_id_fkey 
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE resumes ADD CONSTRAINT resumes_user_id_fkey 
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE evaluations ADD CONSTRAINT evaluations_user_id_fkey 
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Step 13: Recreate indexes
DROP INDEX IF EXISTS idx_roles_user_id;
DROP INDEX IF EXISTS idx_resumes_user_id;
DROP INDEX IF EXISTS idx_evaluations_user_id;

CREATE INDEX idx_roles_user_id ON roles(user_id);
CREATE INDEX idx_resumes_user_id ON resumes(user_id);
CREATE INDEX idx_evaluations_user_id ON evaluations(user_id);

