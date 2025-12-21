-- V3: Add role_id column to resumes table
-- This allows resumes to be associated with a specific role (optional)

ALTER TABLE resumes ADD COLUMN IF NOT EXISTS role_id BIGINT REFERENCES roles(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_resumes_role_id ON resumes(role_id);

