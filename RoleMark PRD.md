Build a fully functional, deployed decision-support web app named
RoleMark. RoleMark helps a recruiter/hiring manager compare candidates
for a specific role using an explicit weighted rubric and deterministic,
explainable scoring. NO AI in MVP. Backend correctness \> UI polish. The
system must never state "Candidate X is better"; it must say "Candidate
X scored higher due to these criteria" and show evidence.

0\) Naming

Project name: RoleMark Other acceptable names RoleMark Use RoleMark
everywhere in codebase (package names, README title, env vars).

1\. Product Requirements Document (PRD) 1.1 Problem

Hiring decisions often rely on inconsistent heuristics or opaque ATS
filters. Recruiters/hiring managers need a transparent, criteria-driven
way to compare candidates for a specific role and understand why one
candidate scored higher.

1.2 Solution

RoleMark is a decision-support system that lets a user:

define a Role (title + job description text)

define an explicit weighted rubric (Criteria)

upload candidate resumes (PDF)

extract deterministic signals (no AI)

run an Evaluation that computes explainable scores and evidence

compare candidates side-by-side

RoleMark supports decisions; it does not replace human judgment.

1.3 Primary User

Recruiter / Hiring Manager (design center)

Secondary incidental users:

Students comparing resume versions

Career advisors

2\. MVP Goals (Non-Negotiable)

Fully functional deployed system.

Deterministic, explainable scoring (formulas defined below).

No AI anywhere in MVP.

Backend correctness and auditability \> UI polish.

A single user can get full value alone (no network effects).

All data is user-owned and access controlled.

3\. Explicit Non-Goals (Out of Scope MVP)

AI scoring, AI parsing, AI suggestions, resume rewriting

Collaboration, sharing, comments, org/team multi-tenancy

ATS export formats

Notifications, analytics dashboards

Redis/queues/observability stacks (Prometheus/Grafana/PagerDuty)

Kubernetes/microservices complexity

4\. Core Functional Requirements 4.1 Authentication & Ownership

Email + password signup/login.

Passwords hashed with BCrypt.

JWT access tokens (no refresh token required for MVP).

All data is user-scoped and enforced in the service layer (not only in
controllers).

Avoid data leakage: requests for resources not owned by user should
return 404 (not 403).

4.2 Role & Rubric Management

User can create Role:

title (string, required, \<= 120)

jobDescription (free text, required, \<= 20,000)

User can create/edit Criteria per role.

Criterion fields:

name (required, \<= 80)

description (optional, \<= 500)

weight (integer 0--100)

type (enum):

KEYWORD_SKILL

CUSTOM_KEYWORDS

EXPERIENCE_YEARS

EDUCATION_LEVEL

config (JSON) --- required fields depend on type (defined below)

Validation:

Criteria weights for a role must sum to exactly 100 to run an
evaluation.

Criteria count limit: max 15 per role (MVP guardrail).

Role title must be unique per user (optional but recommended).

Criterion config schema (deterministic)

KEYWORD_SKILL config:

requiredKeywords: string\[\] (1..50) (e.g., \[\"java\", \"spring boot\",
\"postgres\"\])

matchMode: \"ANY\" \| \"ALL\" (default ANY)

CUSTOM_KEYWORDS config:

keywords: string\[\] (1..50)

matchMode: \"ANY\" \| \"ALL\" (default ANY)

EXPERIENCE_YEARS config:

requiredYears: number (\>=0, \<= 50)

targetTitles?: string\[\] (optional; if present, attempt to estimate
years matching these titles; if not possible, fallback to total
experience)

EDUCATION_LEVEL config:

minimumLevel: \"HS\" \| \"ASSOCIATE\" \| \"BACHELOR\" \| \"MASTER\" \|
\"PHD\"

Config must be validated server-side using Bean Validation + custom
validators.

4.3 Resume Upload & Deterministic Parsing

Upload resume as PDF only.

PDF constraints:

max size: 2.5 MB

max pages: 5 (reject over limit)

Store original PDF bytes in DB (bytea) for MVP simplicity OR store in
local disk with path (choose one; DB storage preferred for
single-container deploy).

Extract raw text deterministically (no AI). Use Apache PDFBox.

Persist:

rawExtractedText

parsing metadata (page count, extraction success)

extracted signals (see below)

Extracted signals (persisted for auditability)

ExtractedSignal model:

id

resumeId

type (enum):

KEYWORD_MATCH

JOB_TITLE

COMPANY

DATE_RANGE

EXPERIENCE_YEARS_ESTIMATE

EDUCATION_LEVEL_ESTIMATE

value (string)

evidenceSnippet (string, \<= 300) --- exact substring or near-substring
from extracted text

confidence (\"LOW\" \| \"MEDIUM\" \| \"HIGH\")

sourcePage (integer nullable) --- if available

createdAt

Parsing rules (best-effort but deterministic):

Keyword normalization:

Lowercase

Strip punctuation

Collapse whitespace

Simple alias map (global + user-defined per criterion) supported in
config later; for MVP, implement a static alias map file in code
(documented in README) and allow per-criterion keywords to include
variants.

Keyword match evidence:

For each matched keyword, store one snippet: 40 chars before and after
the match index from raw text.

If multiple matches, store first occurrence only (deterministic).

Experience years estimation:

Deterministic regex-based date range extraction:

patterns like "MMM YYYY -- MMM YYYY", "MM/YYYY - MM/YYYY", "YYYY -
YYYY", "MMM YYYY -- Present"

Convert to month counts; sum across ranges with overlap handling:

MVP rule: do NOT attempt perfect overlap resolution; instead:

sort ranges by start date

merge overlapping ranges

compute total months

If no date ranges found, store EXPERIENCE_YEARS_ESTIMATE = "0" with
confidence LOW.

Education level estimation:

Search for tokens: "PhD", "Doctor", "Master", "M.S.", "B.S.",
"Bachelor", "Associate", "High School"

Highest detected level wins.

If none detected, level = "UNKNOWN" with confidence LOW.

Everything above must be deterministic and testable.

4.4 Evaluation & Scoring Engine (Deterministic)

User can:

create an Evaluation for a role

select 2..10 resumes

run scoring

view results and breakdown with evidence

4.4.1 Scoring formulas (MUST IMPLEMENT EXACTLY)

All criterion scores are normalized to \[0.0, 1.0\]. Then weighted.

Let weightW = criterion.weight (0..100). Let weightedScore = score \*
(weightW / 100.0).

TotalScore = sum(weightedScore across criteria), reported as:

normalized 0.0..1.0

and percentage 0..100 (round to 1 decimal)

Criterion types:

KEYWORD_SKILL

required = config.requiredKeywords

matchedCount = count of required keywords present in resume text (match
is substring on normalized text)

rawScore = matchedCount / len(required)

if matchMode == \"ALL\" and matchedCount \< len(required), then rawScore
= matchedCount / len(required) (still partial credit; do not hard-zero)

score = clamp(rawScore, 0, 1)

Evidence:

store up to 3 matched keyword snippets (deterministically: first 3 by
keyword order in config)

CUSTOM_KEYWORDS

same as KEYWORD_SKILL, using config.keywords

EXPERIENCE_YEARS

requiredYears = config.requiredYears

candidateYears = parsed EXPERIENCE_YEARS_ESTIMATE (months/12)

score = 1.0 if requiredYears == 0 else min(candidateYears /
requiredYears, 1.0) Evidence:

include one snippet per the date range extraction that contributed to
estimate (cap at 3 snippets)

EDUCATION_LEVEL Map detected level to numeric:

UNKNOWN=0.0

HS=0.25

ASSOCIATE=0.45

BACHELOR=0.65

MASTER=0.85

PHD=1.0

Minimum required level maps similarly (HS=0.25, etc.). If
candidateLevelValue \>= minRequiredValue:

score = 1.0 else:

score = candidateLevelValue / minRequiredValue (partial credit)
Evidence:

snippet where the education token was found, else "No education token
detected" (still deterministic text).

4.4.2 Explanation rules (Non-negotiable)

System must not output "better candidate". System outputs:

"Resume A scored higher due to: \[top 2 criteria deltas\]"

"Resume B scored higher due to: ..." Include per-criterion delta:
(A_score - B_score) and show evidence.

4.4.3 Immutability / auditability (MUST)

Evaluations are immutable snapshots. When an evaluation is run, persist:

roleSnapshot (title + jobDescription at time)

criteriaSnapshot (name/type/weight/config at time)

resumeSnapshot references (resumeId + checksum/hash + extracted signals
as-of time)

computed breakdowns and total scores If role/criteria/resume is later
edited, existing evaluations do not change.

4.5 Comparison View (UI)

Server-rendered minimal UI (Thymeleaf). User flow:

Login

Create Role

Add Criteria (weights sum to 100)

Upload Resumes

Create Evaluation selecting 2+ resumes

View results list (ranked by total score)

Compare any two resumes side-by-side:

total score

per-criterion scores, weights

evidence snippets

explanation text based on deltas

UI must be simple, functional, and correct; styling optional.

5\. Landing Page & Waitlist (Pre-Product)

Purpose:

validate interest

collect early users

do not expose app until ready

Requirements:

Public landing page at / (or separate route) that does NOT require auth

One clear value proposition + waitlist email capture form

Store waitlist emails in DB (WaitlistSignup table)

Landing page must not link to app routes except maybe "Admin login"
hidden path or /app behind auth

When ready later, landing page can redirect to app; for MVP, keep
separate.

6\. Technical Architecture (MVP Non-Negotiable)

Backend:

Java 21

Spring Boot 3

Spring Security with JWT

JPA/Hibernate

PostgreSQL

Flyway migrations

Global exception handling with consistent error JSON

Validation annotations + custom validators for criterion config

Service/Repository separation

Manual DTO mapping acceptable

Minimal service-layer tests required (scoring + parsing + auth
ownership)

Frontend:

Thymeleaf server-rendered UI

REST APIs still required (UI is thin)

Dev & Deploy:

Dockerfile

docker-compose for local dev (app + postgres)

Environment-based config

GitHub Actions: build + test on PR/push

Health check endpoint: GET /api/health

7\. Data Model (Explicit)

Entities (all user-owned except waitlist):

User(id, email unique, passwordHash, createdAt)

Role(id, userId, title, jobDescription, createdAt, updatedAt)

Criterion(id, roleId, name, description, weight, type, configJson,
createdAt, updatedAt)

Resume(id, userId, filename, contentType, pdfBytes OR storagePath,
textExtract, checksumSha256, createdAt)

ExtractedSignal(id, resumeId, type, value, evidenceSnippet, confidence,
sourcePage, createdAt)

Evaluation(id, userId, roleId, createdAt, status enum:
CREATED/RUNNING/COMPLETED/FAILED)

EvaluationSnapshot(id, evaluationId, roleTitle, roleJobDescription,
criteriaJson, createdAt) (or embed into Evaluation as JSON fields)

EvaluationCandidate(id, evaluationId, resumeId, resumeChecksum,
createdAt)

ScoreBreakdown(id, evaluationId, resumeId, totalScore, totalScorePct,
breakdownJson, explanationText)

WaitlistSignup(id, email, createdAt, userAgent optional, referrer
optional)

Indexes:

user.email unique

role.userId

criterion.roleId

resume.userId

evaluation.userId

evaluationCandidate.evaluationId

scoreBreakdown.evaluationId

All ownership checks:

Role belongs to user

Criteria belong to Role

Resume belongs to user

Evaluation belongs to user and only references the user's Role/Resumes

8\. REST API Endpoints (Minimum)

Auth:

POST /api/auth/signup

POST /api/auth/login

Health:

GET /api/health -\> {status:\"ok\"}

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

POST /api/resumes (multipart PDF)

GET /api/resumes

GET /api/resumes/{resumeId}

DELETE /api/resumes/{resumeId}

Evaluations:

POST /api/evaluations (roleId + resumeIds\[\])

POST /api/evaluations/{evaluationId}/run

GET /api/evaluations

GET /api/evaluations/{evaluationId}

GET /api/evaluations/{evaluationId}/results

GET
/api/evaluations/{evaluationId}/compare?leftResumeId=\...&rightResumeId=\...

Waitlist:

POST /waitlist (public) OR POST /api/waitlist (public)

Error format (consistent): { \"timestamp\": \"\...\", \"status\": 400,
\"error\": \"VALIDATION_ERROR\", \"message\": \"Criteria weights must
sum to 100\", \"path\": \"/api/evaluations\", \"correlationId\":
\"uuid\" }

9\. Failure Modes & System Behavior (MUST)

If PDF extraction fails: reject upload with 422 and message; do not
store partial resume.

If criteria weights != 100: evaluation run rejected with 400.

If fewer than 2 resumes selected: reject evaluation create with 400.

If experience date parsing finds no ranges: candidateYears=0, confidence
LOW; score computed normally.

If education token not found: candidateLevel=UNKNOWN; score computed
normally.

If evaluation run fails: status=FAILED and store error summary; do not
partially write breakdowns.

10\. README (Cursor must generate)

README must include:

Problem statement (1 paragraph)

What RoleMark does (MVP scope)

Deterministic approach + "No AI in v1"

Architecture overview (layers)

Data model explanation

Parsing rules summary

Scoring formulas (exact)

Tradeoffs & limitations (regex parsing limits, overlap simplification,
PDF text quality)

How to run locally (docker-compose)

How to run tests

Deployment instructions

Roadmap (v1.1 AI behind feature flag)

Also include a short "Why deterministic first?" section to justify
product thesis.

11\. Execution Checklist (Dependency-Ordered)

Phase 1 --- Backend Skeleton

Spring Boot scaffold

Postgres + Flyway migrations

Health endpoint

CI pipeline

Phase 2 --- Auth + Ownership + Core Domain

JWT auth (signup/login)

User-scoped ownership enforcement

Role CRUD

Criterion CRUD + validation + config validators

Global error handling

Phase 3 --- Resume Pipeline

PDF upload + constraints

PDFBox text extraction

Deterministic parsing + persistence

Tests for parsing + edge cases

Phase 4 --- Evaluation + Scoring Engine

Evaluation creation (2..10 resumes)

Snapshot persistence (immutability)

Scoring + breakdown + explanation text

Tests for scoring formulas

Phase 5 --- Minimal Server-Rendered UI

Thymeleaf pages for role/criteria/resume/evaluation flows

Results view + compare view

Keep styling minimal

Phase 6 --- Landing Page + Waitlist

Public landing page

Waitlist email capture + DB persistence

Ensure it does not expose app routes

Phase 7 --- README + Polish

Complete README sections

Clean up configs, env vars, docker

Final deploy validation

12\. Git Milestones & Commands (commit after each milestone)

Milestone 1 git add . git commit -m \"chore: bootstrap Spring Boot
project with PostgreSQL, Flyway, and CI\" git push

Milestone 2 git add . git commit -m \"feat: implement JWT auth,
ownership enforcement, and role/criteria CRUD\" git push

Milestone 3 git add . git commit -m \"feat: add resume upload with
deterministic PDF parsing and persisted signals\" git push

Milestone 4 git add . git commit -m \"feat: implement immutable
evaluations with deterministic scoring and breakdowns\" git push

Milestone 5 git add . git commit -m \"feat: add minimal server-rendered
UI for evaluation and comparison views\" git push

Milestone 6 git add . git commit -m \"feat: add public landing page and
waitlist signup persistence\" git push

Milestone 7 git add . git commit -m \"docs: write README with
architecture, parsing rules, scoring formulas, and roadmap\" git push
