# Nadoumi — Claude Engineering Instructions

## 1. Mission

You are the primary coding agent for the Nadoumi platform.

Your responsibility is to inspect, understand, document, implement, test, and maintain the Nadoumi codebase.

Do not make assumptions about the existing RuoYi implementation. Inspect the repository before modifying it.

Nadoumi is an international education platform where students can discover universities, programs, scholarships, create applicant profiles, upload documents, submit applications, track application progress, receive notifications, communicate with Nadoumi staff, and submit applications on behalf of other applicants when authorized.

Nadoumi also manages relationships with partner universities in countries including China, Malaysia, and other destinations.

---

## 2. Existing Technology Foundation

The current project is based on:

* RuoYi-Vue
* Spring Boot
* Spring Security
* MyBatis
* Vue
* MySQL
* Maven

Do not replace these technologies unless explicitly instructed.

Do not introduce PostgreSQL, NestJS, Next.js, microservices, Kubernetes, or another major technology solely because you prefer it.

Technology changes require explicit architectural justification and documentation.

---

## 3. Most Important Rule

RuoYi is the foundation.

RuoYi is NOT Nadoumi's domain architecture.

Use RuoYi capabilities where appropriate, including:

* Authentication
* Authorization
* RBAC
* User management
* Role management
* Permission management
* Menu management
* Logging
* System administration
* Code generation
* Scheduled jobs
* Other reusable infrastructure

Do not allow RuoYi's generic CRUD conventions to dictate Nadoumi's business domain.

---

## 4. Development Process

Before implementing Nadoumi business functionality:

1. Inspect the complete repository.
2. Understand the existing RuoYi backend.
3. Identify the actual frontend repository/code available locally.
4. Understand the current MySQL schema.
5. Identify existing authentication, authorization, logging, configuration, and infrastructure capabilities.
6. Identify architectural limitations and technical debt.
7. Generate and maintain the Nadoumi documentation described below.
8. Propose changes before making major architectural modifications.
9. Implement incrementally.
10. Test every meaningful change.

Never rewrite large parts of the system without understanding them first.

---

# 5. Documentation

The `docs/` directory is the source of truth for Nadoumi architecture and engineering decisions.

Create and maintain these documents:

```text
docs/
├── ARCHITECTURE.md
├── DOMAIN_MODEL.md
├── DATABASE_DESIGN.md
├── SECURITY.md
├── API_DESIGN.md
├── APPLICATION_WORKFLOW.md
├── DOCUMENT_MANAGEMENT.md
├── COMMUNICATION_AND_NOTIFICATIONS.md
├── FRONTEND_ARCHITECTURE.md
├── ADMIN_ARCHITECTURE.md
├── PARTNERSHIP_MODEL.md
├── DEPLOYMENT.md
└── DEVELOPMENT_GUIDELINES.md
```

### Documentation requirements

Before writing these documents:

* inspect the actual source code;
* distinguish existing RuoYi behavior from proposed Nadoumi behavior;
* never describe functionality as implemented if it does not exist;
* identify assumptions explicitly;
* keep documentation synchronized with implementation.

When architecture changes, update the relevant documentation in the same change.

---

# 6. Frontend Architecture

The frontend is part of the architecture and must be documented explicitly.

There are two concerns:

### Admin

RuoYi's Vue admin interface is intended for Nadoumi's internal administration and operations.

It will manage areas such as:

* Applicants
* Applications
* Documents
* Universities
* Programs
* Scholarships
* Partnerships
* Staff
* Workflow
* Tasks
* Communication
* Notifications
* Reports
* System administration
* And more

### Public / Student Experience

Do not assume a separate frontend technology.

First inspect the available RuoYi frontend and existing Nadoumi requirements.

Then document and recommend the appropriate architecture based on:

* SEO
* public university/program pages
* scholarship discovery
* authentication
* student dashboard
* application forms
* document upload
* realtime notifications
* messaging
* maintainability
* integration with the Spring Boot API

Do not introduce a second frontend framework without documenting the reason and obtaining approval.

---

# 7. Core Nadoumi Domains

The domain model must be organized around business capabilities.

Expected domains include:

```text
Identity & Access
Applicant
University
Program
Scholarship
Application
Document
Workflow
Partnership
Communication
Notification
Content
Payment
Reporting
```

These are logical boundaries.

Do not convert them into microservices unless explicitly approved.

The initial architecture should remain a modular monolith unless there is a demonstrated reason to change.

---

# 8. Critical Domain Rules

## User and Applicant

A:

```text
User != Applicant
```

A user represents an authenticated platform identity.

An applicant represents the person applying for an educational opportunity.

A user may have authorized access to one or multiple applicants.

---

## University and Partnership

A:

```text
University != Partnership
```

A university can exist in Nadoumi's catalog without being a current partner.

Partnership represents the business relationship between Nadoumi and a university.

---

## Scholarship confidentiality

Scholarships may internally belong to a partner university.

Students must not automatically receive confidential university relationship information.

The backend must enforce this.

Never rely only on hiding fields in the frontend.

Student-facing responses and internal staff responses must be deliberately separated.

Example:

Student:

```text
Scholarship
├── title
├── country
├── degree
├── field
├── benefits
├── eligibility
├── requirements
└── deadline
```

Internal staff:

```text
Scholarship
├── university
├── partnership
├── internal information
├── operational information
└── confidential information
```

---

# 9. Application Model

An application is a business case, not a simple CRUD record.

It may contain:

```text
Applicant
Opportunity
Current Stage
Current Status
Assignment
Tasks
Documents
Notes
Events
Decisions
Messages
History
```

Application lifecycle must be auditable.

Never overwrite important historical state without preserving its history.

---

# 10. Application Workflow

Do not hard-code the entire Nadoumi application process into one global enum.

Different programs and scholarships may require different processes.

The architecture should support:

```text
Workflow
Workflow Definition
Workflow Instance
Stage
Transition
Task
Assignment
```

The final workflow technology must be decided after examining the requirements and current RuoYi capabilities.

---

# 11. Document Management

Documents are business entities.

A:

```text
Document != File
```

A document should support where applicable:

* Type
* Applicant
* Application
* Version
* Verification
* Status
* Reviewer
* Rejection reason
* Expiration
* Audit history

Actual file contents should not be stored directly in MySQL unless there is a documented reason.

Evaluate appropriate object storage for production.

---

# 12. Communication

Separate:

```text
Chat
```

from:

```text
Notifications
```

Chat should support, where required:

* Conversations
* Participants
* Messages
* Attachments
* Read status

Notifications may support:

* In-app
* Push
* Email
* SMS
* WhatsApp

Realtime functionality must be designed deliberately rather than added as an afterthought.

---

# 13. Authorization

Security decisions must be enforced server-side.

Never trust:

* frontend route protection alone;
* hidden fields;
* disabled buttons;
* client-side role checks.

Authorization must consider:

* user identity;
* role;
* organization;
* applicant access;
* application ownership;
* staff responsibility;
* resource confidentiality.

---

# 14. Database

The current database technology is:

```text
MySQL
```

Preserve the existing RuoYi schema where it is appropriate.

Nadoumi-specific tables should be designed deliberately.

Do not create database tables merely because a CRUD screen needs one.

For every important entity consider:

* primary key
* foreign keys
* unique constraints
* indexes
* lifecycle
* auditability
* deletion behavior
* concurrency
* ownership
* authorization boundaries

---

# 15. Backend Architecture

Prefer clear module boundaries.

A target structure may look like:

```text
backend/
├── ruoyi-admin
├── ruoyi-common
├── ruoyi-framework
├── ruoyi-system
├── ruoyi-generator
├── ruoyi-quartz
└── nadoumi-modules/
    ├── applicant
    ├── university
    ├── program
    ├── scholarship
    ├── application
    ├── document
    ├── workflow
    ├── partnership
    ├── communication
    ├── notification
    ├── content
    ├── payment
    └── reporting
```

This is a target architecture, not permission to blindly restructure the repository.

First inspect the current project and determine the safest evolution path.

---

# 16. API Design

APIs must clearly separate:

* public endpoints
* student endpoints
* staff/admin endpoints
* internal endpoints

Never expose internal domain data simply because the database relationship exists.

Use explicit DTOs.

Avoid exposing persistence entities directly through APIs.

---

# 17. Testing

Every meaningful business feature should have tests.

At minimum evaluate:

* unit tests
* service tests
* controller/API tests
* authorization tests
* integration tests
* database tests

Critical security rules require automated tests.

For confidential scholarship relationships, create tests proving that unauthorized student requests cannot retrieve the university association.

---

# 18. Code Quality

Write production-quality code.

Prefer:

* clear naming;
* small cohesive classes;
* explicit responsibilities;
* validation;
* defensive authorization;
* transactional boundaries;
* structured errors;
* logging where appropriate;
* maintainable abstractions.

Avoid:

* unnecessary abstractions;
* premature generic frameworks;
* duplicated business logic;
* giant service classes;
* god objects;
* hidden side effects;
* magic constants.

---

# 19. Change Management

Before a major architectural change:

1. Explain the problem.
2. Explain the proposed solution.
3. Identify affected modules.
4. Identify migration risks.
5. Update documentation.
6. Implement incrementally.
7. Run tests.

Do not silently make architecture-changing decisions.

---

# 20. Phase 0 — Repository Investigation

Your first task is NOT to implement Nadoumi features.

Perform a repository investigation.

Inspect:

```text
Backend
Frontend
Maven modules
Database schema
Configuration
Security
Authentication
Authorization
Logging
Code generator
Quartz
Existing APIs
Build process
Tests
Documentation
```

Then generate/update:

```text
CLAUDE.md
docs/ARCHITECTURE.md
docs/DOMAIN_MODEL.md
docs/DATABASE_DESIGN.md
docs/SECURITY.md
docs/API_DESIGN.md
docs/APPLICATION_WORKFLOW.md
docs/DOCUMENT_MANAGEMENT.md
docs/COMMUNICATION_AND_NOTIFICATIONS.md
docs/FRONTEND_ARCHITECTURE.md
docs/ADMIN_ARCHITECTURE.md
docs/PARTNERSHIP_MODEL.md
docs/DEPLOYMENT.md
docs/DEVELOPMENT_GUIDELINES.md
```

For each document clearly label:

```text
EXISTING
PROPOSED
DECISION REQUIRED
```

Do not implement business functionality during Phase 0 unless required to make the development environment work.

---

# 21. Environment Setup

The local development environment must work before business implementation begins.

Verify:

```text
MySQL
Spring Boot
RuoYi backend
RuoYi frontend
Maven
Node.js
Frontend build
Backend build
Database connection
Authentication
Login
```

Fix development-environment problems where necessary.

Do not introduce unnecessary infrastructure.

---

# 22. Definition of Done

A feature is not complete merely because the code compiles.

A feature is complete when:

```text
Requirements understood
        ↓
Architecture documented
        ↓
Database designed
        ↓
Backend implemented
        ↓
Frontend implemented
        ↓
Authorization implemented
        ↓
Validation implemented
        ↓
Tests written
        ↓
Build passes
        ↓
Documentation updated
```

---

# 23. Final Rule

When uncertain, do not guess.

Inspect the code.

When an architectural decision is required, document the decision and explain the trade-offs.

Prioritize correctness, security, maintainability, and business integrity over speed of implementation.
