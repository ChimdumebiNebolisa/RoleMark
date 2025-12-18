-- RoleMark Initial Schema
-- All tables with user ownership enforcement

-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);

-- Roles table
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(120) NOT NULL,
    job_description TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_roles_user_id ON roles(user_id);

-- Criteria table
CREATE TABLE criteria (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(500),
    weight INTEGER NOT NULL CHECK (weight >= 0 AND weight <= 100),
    type VARCHAR(50) NOT NULL,
    config_json JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_criteria_role_id ON criteria(role_id);

-- Resumes table (metadata-first approach)
CREATE TABLE resumes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    checksum_sha256 VARCHAR(64) NOT NULL,
    raw_extracted_text TEXT NOT NULL,
    storage_path VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_resumes_user_id ON resumes(user_id);
CREATE INDEX idx_resumes_checksum ON resumes(checksum_sha256);

-- Extracted Signals table
CREATE TABLE extracted_signals (
    id BIGSERIAL PRIMARY KEY,
    resume_id BIGINT NOT NULL REFERENCES resumes(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    value TEXT NOT NULL,
    evidence_snippet VARCHAR(300),
    confidence VARCHAR(20) NOT NULL CHECK (confidence IN ('LOW', 'MEDIUM', 'HIGH')),
    source_page INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_extracted_signals_resume_id ON extracted_signals(resume_id);
CREATE INDEX idx_extracted_signals_type ON extracted_signals(type);

-- Evaluations table
CREATE TABLE evaluations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED' CHECK (status IN ('CREATED', 'RUNNING', 'COMPLETED', 'FAILED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_evaluations_user_id ON evaluations(user_id);
CREATE INDEX idx_evaluations_role_id ON evaluations(role_id);

-- Evaluation Snapshots table (immutability)
CREATE TABLE evaluation_snapshots (
    id BIGSERIAL PRIMARY KEY,
    evaluation_id BIGINT NOT NULL REFERENCES evaluations(id) ON DELETE CASCADE,
    role_title VARCHAR(120) NOT NULL,
    role_job_description TEXT NOT NULL,
    criteria_json JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_evaluation_snapshots_evaluation_id ON evaluation_snapshots(evaluation_id);

-- Evaluation Candidates table
CREATE TABLE evaluation_candidates (
    id BIGSERIAL PRIMARY KEY,
    evaluation_id BIGINT NOT NULL REFERENCES evaluations(id) ON DELETE CASCADE,
    resume_id BIGINT NOT NULL REFERENCES resumes(id) ON DELETE CASCADE,
    resume_checksum VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_evaluation_candidates_evaluation_id ON evaluation_candidates(evaluation_id);
CREATE INDEX idx_evaluation_candidates_resume_id ON evaluation_candidates(resume_id);

-- Score Breakdowns table
CREATE TABLE score_breakdowns (
    id BIGSERIAL PRIMARY KEY,
    evaluation_id BIGINT NOT NULL REFERENCES evaluations(id) ON DELETE CASCADE,
    resume_id BIGINT NOT NULL REFERENCES resumes(id) ON DELETE CASCADE,
    total_score DECIMAL(5,4) NOT NULL CHECK (total_score >= 0 AND total_score <= 1),
    total_score_pct DECIMAL(5,1) NOT NULL CHECK (total_score_pct >= 0 AND total_score_pct <= 100),
    breakdown_json JSONB NOT NULL,
    explanation_text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_score_breakdowns_evaluation_id ON score_breakdowns(evaluation_id);
CREATE INDEX idx_score_breakdowns_resume_id ON score_breakdowns(resume_id);

-- Waitlist Signups table (public, no user ownership)
CREATE TABLE waitlist_signups (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    user_agent VARCHAR(500),
    referrer VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_waitlist_signups_email ON waitlist_signups(email);
CREATE INDEX idx_waitlist_signups_created_at ON waitlist_signups(created_at);

