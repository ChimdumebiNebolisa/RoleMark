RoleMark PRD (Step-by-Step, Milestone-Gated)
0) One-line product definition

RoleMark is a deterministic, explainable, role-scoped resume scoring and comparison system that uses an explicit weighted rubric and produces evidence-backed score breakdowns — no AI in MVP.

1) Non-negotiable execution constraints (THIS IS THE FIX)

These rules override all other instructions.

1.1 Milestone gating (no big-bang builds)

The system must be built only one milestone at a time.
If a request would implement work outside the current milestone, it must be rejected and deferred.

1.2 “Green build” rule (always shippable)

At the end of every milestone:

mvn -DskipTests clean package must pass

required tests for that milestone must pass

only then: commit + push using the milestone commit message

If build/tests fail: stop, fix, or revert. No “continue anyway”.

1.3 Single working tree rule (no split-brain)

All development must happen in one repo folder only (pick WSL Linux filesystem OR Windows). Cursor must open that exact folder. No second clone, no duplicate working copy.

1.4 Scope control

During any milestone, only files related to that milestone may be created/modified. No “future-proofing,” no extra endpoints, no extra UI.

2) Naming

Project name: RoleMark
Use RoleMark consistently in package names, README, env vars, and docs.

3) Problem

Hiring decisions often rely on inconsistent heuristics or opaque ATS filters. Recruiters need a transparent rubric-driven way to compare candidates for a specific role and understand why a resume scored higher.

4) Solution

RoleMark lets a user:

create a Role (title + job description)

create weighted Criteria for that role

upload Resumes tied to that role

run a deterministic Evaluation

view ranked results and compare candidates side-by-side using score breakdowns + evidence

RoleMark supports decisions; it does not replace human judgment.

5) MVP goals (non-negotiable)

Fully functional deployed system.

Deterministic, explainable scoring (exact formulas below).

No AI anywhere in MVP.

Backend correctness and auditability > UI polish.

All data is user-owned and access-controlled.

6) Explicit non-goals (MVP)

AI scoring/parsing/suggestions

Collaboration / sharing / org multi-tenancy

ATS exports

Notifications, analytics dashboards

Redis/queues/observability stacks

Kubernetes/microservices

7) Core functional requirements
7.1 Authentication & ownership

Email + password signup/login

BCrypt password hashing

JWT access tokens (no refresh tokens required)

All data user-scoped enforced in service layer

Non-owned resources must return 404 (not 403)

7.2 Role & criteria management

Role:

title (required, <= 120)

jobDescription (required, <= 20,000)

Criterion:

name (required, <= 80)

description (optional, <= 500)

weight (integer 0–100)

type enum:

KEYWORD_SKILL

CUSTOM_KEYWORDS

EXPERIENCE_YEARS

EDUCATION_LEVEL

config JSON (schema depends on type)

Validation:

criteria count max 15 per role

weights must sum to exactly 100 to run evaluation

role title unique per user (recommended)

Criterion config schema:

KEYWORD_SKILL:

requiredKeywords: string[] (1..50)

matchMode: ANY | ALL (default ANY)

CUSTOM_KEYWORDS:

keywords: string[] (1..50)

matchMode: ANY | ALL (default ANY)

EXPERIENCE_YEARS:

requiredYears: number (0..50)

targetTitles?: string[] optional

EDUCATION_LEVEL:

minimumLevel: HS | ASSOCIATE | BACHELOR | MASTER | PHD

Config validation must be server-side using Bean Validation + custom validators.

7.3 Resume upload & deterministic parsing

Upload PDF only:

max size 2.5MB

max pages 5

Storage: store PDF bytes in DB (bytea preferred).

Extract text deterministically (no AI) using PDFBox.

Persist:

rawExtractedText

parsing metadata (page count, extraction success)

extracted signals

ExtractedSignal:

id, resumeId

type: KEYWORD_MATCH | JOB_TITLE | COMPANY | DATE_RANGE | EXPERIENCE_YEARS_ESTIMATE | EDUCATION_LEVEL_ESTIMATE

value string

evidenceSnippet <= 300

confidence LOW | MEDIUM | HIGH

sourcePage nullable

createdAt

Deterministic parsing rules:

keyword normalization: lowercase, strip punctuation, collapse whitespace

evidence snippet: 40 chars before/after first match

experience estimation: regex date ranges, month counts, merge overlaps deterministically

if no ranges: EXPERIENCE_YEARS_ESTIMATE=0, confidence LOW

education estimation: highest token wins, else UNKNOWN LOW

7.4 Evaluation & scoring engine (deterministic)

User can:

create evaluation for role

select 2..10 resumes

run scoring

view results, breakdowns, evidence

7.4.1 Exact scoring formulas (must implement exactly)

Each criterion produces score in [0.0, 1.0].

weightedScore = score * (weight / 100.0)
TotalScore = sum(weightedScore)
Report TotalScore normalized (0..1) and percentage (0..100, round 1 decimal)

Criterion types:

KEYWORD_SKILL / CUSTOM_KEYWORDS:

matchedCount / requiredCount

substring match on normalized text

evidence: first 3 matched keyword snippets by keyword order

EXPERIENCE_YEARS:

score = 1.0 if requiredYears==0 else min(candidateYears/requiredYears, 1.0)

evidence: up to 3 snippets from date ranges used

EDUCATION_LEVEL:

map: UNKNOWN=0, HS=0.25, ASSOCIATE=0.45, BACHELOR=0.65, MASTER=0.85, PHD=1.0

if candidate >= minimum => 1.0 else candidate/minimum

evidence: snippet with token or “No education token detected”

7.4.2 Explanation rules (non-negotiable)

Never output “better candidate”.
Instead output:

“Resume A scored higher due to: [top 2 criteria deltas]”
Include deltas per criterion and evidence snippets.

7.4.3 Immutability / auditability (must)

Evaluations are immutable snapshots.
Persist:

role snapshot

criteria snapshot

resume snapshot refs (resumeId + checksum + extracted signals as-of time)

computed breakdowns + explanations
Edits to roles/criteria/resumes must not change past evaluations.

8) REST API (minimum)

Auth:

POST /api/auth/signup

POST /api/auth/login

Health:

GET /api/health -> {status:"ok"}

Roles:

POST /api/roles

GET /api/roles

GET /api/roles/{roleId}

PUT /api/roles/{roleId}

DELETE /api/roles/{roleId}

Criteria:

POST /api/roles/{roleId}/criteria

GET /api/roles/{roleId}/criteria

PUT /api/roles/{roleId}/criteria/{criterionId}

DELETE /api/roles/{roleId}/criteria/{criterionId}

Resumes:

POST /api/roles/{roleId}/resumes (multipart PDF) (role-scoped)

GET /api/roles/{roleId}/resumes

GET /api/resumes/{resumeId}

DELETE /api/resumes/{resumeId}

Evaluations:

POST /api/evaluations (roleId + resumeIds[])

POST /api/evaluations/{evaluationId}/run

GET /api/evaluations

GET /api/evaluations/{evaluationId}

GET /api/evaluations/{evaluationId}/results

GET /api/evaluations/{evaluationId}/compare?leftResumeId=...&rightResumeId=...

Waitlist (post-MVP):

POST /waitlist OR POST /api/waitlist (public)

Error format (consistent):
{ "timestamp": "...", "status": 400, "error": "VALIDATION_ERROR", "message": "...", "path": "...", "correlationId": "uuid" }

9) Failure modes (must)

PDF extraction fails: reject 422, store nothing

weights != 100: evaluation run rejected 400

<2 resumes: rejected 400

no experience ranges: 0 years LOW

no education token: UNKNOWN LOW

evaluation fails: status FAILED + error summary; no partial breakdown writes

10) Tech requirements

Java 21, Spring Boot 3, Maven

PostgreSQL + Flyway

Spring Security + JWT

GitHub Actions build+test on push/PR

Dockerfile + docker-compose

Minimal service-layer tests for auth ownership, parsing, scoring

11) Milestones (dependency-ordered, no skipping)

Milestone 1: Backend skeleton (boot + db + flyway + health + CI)
Milestone 2: Auth + ownership + role/criteria CRUD + error handling
Milestone 3: Role-scoped resume upload + PDF parsing + persisted signals + tests
Milestone 4: Evaluation creation/run + immutable snapshots + scoring + tests
Milestone 5: Minimal UI (Thymeleaf)
Milestone 6: Landing + waitlist
Milestone 7: README + polish

Required commit messages (exact)

M1: chore: bootstrap Spring Boot project with PostgreSQL, Flyway, and CI

M2: feat: implement JWT auth, ownership enforcement, and role/criteria CRUD

M3: feat: add role-scoped resume upload with deterministic PDF parsing and persisted signals

M4: feat: implement immutable evaluations with deterministic scoring and breakdowns

M5: feat: add minimal server-rendered UI for evaluation and comparison views

M6: feat: add public landing page and waitlist signup persistence

M7: docs: write README with architecture, parsing rules, scoring formulas, and roadmap