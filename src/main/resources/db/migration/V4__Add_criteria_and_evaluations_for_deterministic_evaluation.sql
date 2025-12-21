-- V4: Add Criteria and Evaluation persistence for deterministic evaluation
-- This migration adds user ownership to criteria and creates evaluation results table
--
-- NAMING CONVENTION:
--   - evaluations: Evaluation run metadata/status (pipeline job tracking with CREATED/RUNNING/COMPLETED/FAILED status)
--   - evaluation_results: Deterministic scoring output (total_score + breakdown JSONB per resume)
-- These serve different purposes: evaluations tracks the pipeline, evaluation_results stores computed scores.

-- Step 1: Add user_id to criteria table as NULLABLE first (safe for existing data)
ALTER TABLE criteria ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES users(id) ON DELETE CASCADE;

-- Step 2: Backfill user_id for all existing criteria rows from their parent roles
-- This is safe because criteria.role_id references roles.id, and roles.user_id exists (from V2 migration)
UPDATE criteria c 
SET user_id = r.user_id 
FROM roles r 
WHERE c.role_id = r.id AND c.user_id IS NULL;

-- Step 3: Enforce NOT NULL constraint after backfill is complete
ALTER TABLE criteria ALTER COLUMN user_id SET NOT NULL;

-- Add keywords JSONB column to criteria (for simplified keyword storage)
ALTER TABLE criteria ADD COLUMN IF NOT EXISTS keywords JSONB NOT NULL DEFAULT '[]'::jsonb;

-- Create index for criteria user_id and role_id lookup
CREATE INDEX IF NOT EXISTS idx_criteria_user_role ON criteria(user_id, role_id);

-- Add unique constraint to prevent duplicate criteria names per role per user
CREATE UNIQUE INDEX IF NOT EXISTS idx_criteria_user_role_name_unique ON criteria(user_id, role_id, name);

-- Create evaluation_results table for deterministic scoring output
-- NAMING: This table stores computed scores (total_score + breakdown) per resume.
-- The existing "evaluations" table (from V1) tracks evaluation pipeline runs (status: CREATED/RUNNING/COMPLETED/FAILED).
-- These are separate concerns: evaluations = job tracking, evaluation_results = scoring output.
CREATE TABLE evaluation_results (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    resume_id BIGINT NOT NULL REFERENCES resumes(id) ON DELETE CASCADE,
    total_score INT NOT NULL CHECK (total_score >= 0 AND total_score <= 100),
    breakdown JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Create indexes for evaluation_results
CREATE INDEX idx_eval_user_role ON evaluation_results(user_id, role_id);
CREATE INDEX idx_eval_user_resume ON evaluation_results(user_id, resume_id);

-- Add unique constraint to prevent duplicate evaluations
CREATE UNIQUE INDEX idx_eval_user_role_resume_unique ON evaluation_results(user_id, role_id, resume_id);

