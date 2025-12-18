# RoleMark

**Transparent, criteria-driven candidate comparison for recruiters and hiring managers.**

RoleMark is a decision-support system that enables recruiters to compare candidates using explicit weighted rubrics with deterministic, explainable scoring. No AI black boxes. Just clear, evidence-based decisions.

## Problem Statement

Hiring decisions often rely on inconsistent heuristics or opaque ATS filters. Recruiters and hiring managers need a transparent, criteria-driven way to compare candidates for a specific role and understand why one candidate scored higher than another. RoleMark addresses this by providing explicit scoring formulas, evidence snippets, and deterministic parsing—no AI required.

## MVP Scope

RoleMark MVP is a fully functional, deployed decision-support web application with the following features:

- **Role Management**: Define roles with job descriptions
- **Weighted Rubrics**: Create explicit criteria with configurable weights (must sum to 100)
- **Resume Upload**: Upload PDF resumes (max 2.5MB, 5 pages)
- **Deterministic Parsing**: Extract keywords, experience years, and education levels using regex-based parsing
- **Immutable Evaluations**: Run evaluations that create snapshots, ensuring results never change even if source data is edited
- **Explainable Scoring**: View per-criterion scores with evidence snippets
- **Side-by-Side Comparison**: Compare any two candidates with delta explanations

### No AI in v1

RoleMark MVP explicitly does **not** use AI anywhere. All parsing and scoring is deterministic and rule-based. This ensures:
- **Transparency**: Every score can be explained with exact formulas
- **Reproducibility**: Same inputs always produce same outputs
- **Auditability**: All evidence is stored and traceable
- **No surprises**: No black-box behavior

## Architecture Overview

RoleMark is built as a Spring Boot 3 application with the following architecture:

```
┌─────────────┐
│   Browser   │
└──────┬──────┘
       │ HTTP
       ▼
┌─────────────────────────────────┐
│   Spring Boot Application       │
│  ┌───────────────────────────┐  │
│  │  Thymeleaf UI (Server)    │  │
│  └───────────────────────────┘  │
│  ┌───────────────────────────┐  │
│  │  REST API Controllers     │  │
│  └───────────────────────────┘  │
│  ┌───────────────────────────┐  │
│  │  Service Layer            │  │
│  │  - AuthService            │  │
│  │  - RoleService            │  │
│  │  - ResumeService          │  │
│  │  - ScoringService         │  │
│  │  - EvaluationService      │  │
│  └───────────────────────────┘  │
│  ┌───────────────────────────┐  │
│  │  Repository Layer (JPA)   │  │
│  └───────────────────────────┘  │
└──────────────┬──────────────────┘
               │ JDBC
               ▼
        ┌─────────────┐
        │ PostgreSQL  │
        └─────────────┘
```

### Technology Stack

- **Backend**: Java 21, Spring Boot 3.2.0
- **Security**: Spring Security with JWT authentication
- **Database**: PostgreSQL 16
- **Migrations**: Flyway
- **PDF Processing**: Apache PDFBox 3.0.1
- **Frontend**: Thymeleaf (server-rendered)
- **Build**: Maven
- **Containerization**: Docker (multi-stage build)

## Data Model

### Core Entities

- **User**: Email, password hash, timestamps
- **Role**: Title, job description, user ownership
- **Criterion**: Name, description, weight (0-100), type, config JSON
- **Resume**: Metadata (filename, size, checksum), extracted text, optional storage path
- **ExtractedSignal**: Type, value, evidence snippet, confidence level
- **Evaluation**: Status (CREATED/RUNNING/COMPLETED/FAILED), user/role references
- **EvaluationSnapshot**: Immutable snapshot of role and criteria at evaluation time
- **EvaluationCandidate**: Links evaluation to resumes with checksums
- **ScoreBreakdown**: Total score, per-criterion breakdown JSON, explanation text
- **WaitlistSignup**: Email, user agent, referrer (public, no user ownership)

### Ownership Model

All data is user-scoped. Service layer enforces ownership checks, returning 404 (not 403) for unauthorized resources to prevent information leakage.

## Parsing Rules

### Keyword Normalization

1. Convert to lowercase
2. Strip punctuation
3. Collapse whitespace
4. Substring matching on normalized text

### Date Range Extraction

Deterministic regex patterns:
- `MMM YYYY -- MMM YYYY` (e.g., "January 2020 -- March 2022")
- `MM/YYYY - MM/YYYY` (e.g., "01/2020 - 03/2022")
- `YYYY - YYYY` (e.g., "2020 - 2022")
- `MMM YYYY -- Present` (current date)

**Overlap Handling**: Ranges are sorted by start date, overlapping ranges are merged, then total months are calculated. Simplified approach: no perfect overlap resolution, just merge and sum.

### Experience Years Estimation

1. Extract all date ranges matching patterns above
2. Merge overlapping ranges
3. Calculate total months
4. Convert to years: `months / 12.0`
5. If no ranges found: `0` years with LOW confidence

### Education Level Detection

Token search (case-insensitive) in order of precedence:
1. "PhD", "Ph.D.", "Doctor", "Doctorate" → PHD
2. "Master", "M.S.", "M.A.", "MS", "MA" → MASTER
3. "Bachelor", "B.S.", "B.A.", "BS", "BA" → BACHELOR
4. "Associate", "A.S.", "AA" → ASSOCIATE
5. "High School", "HS", "H.S." → HS

Highest detected level wins. If none found: UNKNOWN with LOW confidence.

## Scoring Formulas

All criterion scores are normalized to [0.0, 1.0], then weighted. Total score is the sum of weighted scores.

### Formula

```
weightedScore = score × (weight / 100.0)
totalScore = sum(weightedScore across all criteria)
totalScorePct = totalScore × 100 (rounded to 1 decimal)
```

### Criterion Types

#### KEYWORD_SKILL

```
requiredKeywords = config.requiredKeywords
matchedCount = count of required keywords present in resume text
rawScore = matchedCount / len(requiredKeywords)
score = clamp(rawScore, 0, 1)
```

**Evidence**: Up to 3 matched keyword snippets (first 3 by keyword order in config)

#### CUSTOM_KEYWORDS

Same as KEYWORD_SKILL, using `config.keywords`

#### EXPERIENCE_YEARS

```
requiredYears = config.requiredYears
candidateYears = parsed EXPERIENCE_YEARS_ESTIMATE (months/12)
score = 1.0 if requiredYears == 0 else min(candidateYears / requiredYears, 1.0)
```

**Evidence**: Up to 3 date range snippets that contributed to estimate

#### EDUCATION_LEVEL

```
Level mapping:
UNKNOWN = 0.0
HS = 0.25
ASSOCIATE = 0.45
BACHELOR = 0.65
MASTER = 0.85
PHD = 1.0

candidateLevelValue = map(detected level)
minRequiredValue = map(config.minimumLevel)

if candidateLevelValue >= minRequiredValue:
    score = 1.0
else:
    score = candidateLevelValue / minRequiredValue  # Partial credit
```

**Evidence**: Snippet where education token was found, or "No education token detected"

### Explanation Rules

The system **never** states "Candidate X is better". Instead, it outputs:

- "Resume A scored higher due to: [top 2 criteria deltas]"
- Per-criterion delta: `(A_score - B_score)` with evidence

## Tradeoffs & Limitations

### Parsing Limitations

- **Regex-based date parsing**: May miss non-standard date formats
- **Overlap simplification**: Does not attempt perfect overlap resolution; simple merge-and-sum approach
- **PDF text quality**: Depends on PDF text extraction quality; scanned PDFs may not work well
- **Education detection**: Simple token matching; may miss variations or abbreviations

### Design Decisions

- **Metadata-first storage**: PDF files not stored in DB; only extracted text. System works correctly even if PDF file is lost.
- **Immutability**: Evaluations are snapshots. Source data changes do not affect existing evaluations.
- **No AI**: All parsing and scoring is deterministic. This limits sophistication but ensures transparency.

## Local Setup

### Prerequisites

- Docker and Docker Compose
- Java 21 (for local development without Docker)

### Running with Docker Compose

1. **Create `.env` file** from `.env.example`:
   ```bash
   cp .env.example .env
   ```

2. **Edit `.env`** and set secure values:
   ```env
   POSTGRES_DB=rolemark
   POSTGRES_USER=rolemark_user
   POSTGRES_PASSWORD=your_secure_password
   SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/rolemark
   SPRING_DATASOURCE_USERNAME=rolemark_user
   SPRING_DATASOURCE_PASSWORD=your_secure_password
   JWT_SECRET=your_secure_random_string
   ```

3. **Build and start services**:
   ```bash
   docker compose up --build
   ```

4. **Access the application**:
   - Application: http://localhost:8080
   - Health endpoint: http://localhost:8080/api/health

5. **Stop services**:
   ```bash
   docker compose down
   ```

6. **Stop and remove volumes** (clean slate):
   ```bash
   docker compose down -v
   ```

### Running Locally (Without Docker)

1. **Start PostgreSQL** (ensure it's running on localhost:5432)

2. **Set environment variables**:
   ```bash
   export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/rolemark
   export SPRING_DATASOURCE_USERNAME=rolemark_user
   export SPRING_DATASOURCE_PASSWORD=your_password
   export JWT_SECRET=your_secret
   ```

3. **Run the application**:
   ```bash
   ./mvnw spring-boot:run
   ```

## Testing

Run tests with Maven:

```bash
./mvnw test
```

Service-layer tests cover:
- Scoring formulas (exact formulas verified)
- Parsing edge cases
- Ownership enforcement

## Railway Deployment

1. **Create Railway account** and new project

2. **Add PostgreSQL service**:
   - Railway will provide connection string automatically

3. **Deploy application**:
   - Connect GitHub repository
   - Railway will detect Dockerfile and build automatically
   - Set environment variables:
     - `SPRING_DATASOURCE_URL` (from PostgreSQL service)
     - `SPRING_DATASOURCE_USERNAME`
     - `SPRING_DATASOURCE_PASSWORD`
     - `JWT_SECRET` (generate secure random string)
     - `SPRING_PROFILES_ACTIVE=prod`

4. **Deploy**: Railway will build and deploy automatically on push to main branch

## AWS Migration Guide

### Target Architecture

```
┌─────────────────────────────────────────┐
│   Application Load Balancer (ALB)      │
└──────────────┬──────────────────────────┘
               │
               ▼
    ┌──────────────────────┐
    │  ECS Fargate Service │
    │  ┌────────────────┐  │
    │  │  RoleMark App  │  │
    │  │  (Docker)      │  │
    │  └────────────────┘  │
    └──────────┬───────────┘
               │
               ▼
    ┌──────────────────────┐
    │  RDS PostgreSQL      │
    │  (Multi-AZ)          │
    └──────────────────────┘
```

### Migration Steps

1. **Create RDS PostgreSQL Instance**:
   - Engine: PostgreSQL 16
   - Multi-AZ: Enabled (for production)
   - Security group: Allow inbound from ECS security group on port 5432

2. **Create ECR Repository**:
   ```bash
   aws ecr create-repository --repository-name rolemark
   ```

3. **Build and Push Docker Image**:
   ```bash
   aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com
   docker build -t rolemark .
   docker tag rolemark:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/rolemark:latest
   docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/rolemark:latest
   ```

4. **Create ECS Cluster and Fargate Service**:
   - Task definition: Use pushed image from ECR
   - Environment variables:
     - `SPRING_DATASOURCE_URL=jdbc:postgresql://<rds-endpoint>:5432/rolemark`
     - `SPRING_DATASOURCE_USERNAME=<rds-username>`
     - `SPRING_DATASOURCE_PASSWORD=<rds-password>` (use Secrets Manager)
     - `JWT_SECRET=<secret>` (use Secrets Manager)
     - `SPRING_PROFILES_ACTIVE=prod`

5. **Create Application Load Balancer**:
   - Target group: ECS Fargate service
   - Health check: `/api/health`
   - HTTPS: Configure SSL certificate (ACM)

6. **Run Flyway Migrations**:
   - Migrations run automatically on application startup
   - Or run manually: `flyway migrate` with RDS connection string

### Configuration Changes

- **Database**: Update `SPRING_DATASOURCE_URL` to RDS endpoint
- **Secrets**: Use AWS Secrets Manager for sensitive values
- **Logging**: Configure CloudWatch Logs for application logs
- **Monitoring**: Set up CloudWatch alarms for ECS service health

### No Platform Lock-ins

RoleMark is designed to be stateless and platform-agnostic:
- **No local filesystem dependencies**: All data in PostgreSQL
- **Environment-based config**: All configuration via environment variables
- **Dockerized**: Runs anywhere Docker is supported
- **Stateless application**: Horizontal scaling ready

## Why Deterministic First?

**Transparency over sophistication.** 

In hiring, explainability is critical. Recruiters need to justify decisions to candidates, hiring managers, and legal teams. AI models, while powerful, are black boxes. RoleMark's deterministic approach ensures:

1. **Every score is explainable**: Exact formulas, no randomness
2. **Reproducible results**: Same inputs always produce same outputs
3. **Auditable decisions**: All evidence stored and traceable
4. **No bias from training data**: Rules are explicit and reviewable
5. **Legal defensibility**: Clear criteria and scoring methodology

Once the deterministic foundation is proven, AI can be added behind feature flags for enhanced parsing or suggestions—but the core scoring remains transparent and explainable.

## Roadmap

### v1.1 (Future)

- **AI-enhanced parsing** (behind feature flag): Use AI to improve date range and education detection
- **Resume storage**: AWS S3 integration for PDF file storage
- **Export functionality**: Export evaluations to PDF/CSV
- **Bulk resume upload**: Upload multiple resumes at once

### v2.0 (Future)

- **Collaboration**: Share roles and evaluations with team members
- **ATS integration**: Export to common ATS formats
- **Analytics dashboard**: Aggregate statistics across evaluations
- **Custom scoring functions**: User-defined scoring logic

## License

[Your License Here]

## Contributing

[Contributing Guidelines Here]

