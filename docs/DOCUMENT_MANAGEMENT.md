# Nadoumi — Document Management

Status: **BASELINE** · **EXISTING** · **PLANNED** · **OPEN**.

> Reconciled with Rev 3 (D5, D11 approved). Draft DDL: `docs/ddl/nad_core.draft.sql`
> §7 (dependency stub — this slice is finalized here and lands as a later Flyway
> migration, `V9` region).

---

## 1. What exists today (EXISTING)

RuoYi ships **file handling**, not document management.

- `CommonController` `/common/**`: `upload`, `uploads`, `download`,
  `download/resource`. Local filesystem under `ruoyi.profile`
  (`application.yml` → `D:/ruoyi/uploadPath`, a Windows path — broken on macOS/Linux;
  never exercised). Files at `${profile}/upload/yyyy/MM/dd/<uuid>.<ext>`, served
  **anonymously** under `/profile/**`.
- `FileUploadUtils` / `MimeTypeUtils`: `DEFAULT_MAX_SIZE = 50 MB` but Spring multipart
  caps at 10 MB / 20 MB; extension allow-list (`.html` blocked); filename ≤ 100.
- **No** document entity, versioning, verification, reviewer, expiry, audit, or access
  control. Business modules store only a URL string (e.g. `sys_user.avatar`).

## 2. Requirements (CLAUDE.md §11)

`Document != File`. A document has: type, applicant, application, version,
verification, status, reviewer, rejection reason, expiration, audit history. Bytes not
in MySQL. Object storage for production.

## 3. Model (BASELINE)

### 3.1 Entities — DDL §7

```
nad_document                 id, applicant_id (ALWAYS set = owner),
                             application_id? (set only for app-specific docs),
                             doc_type (dict nad_document_type),
                             status(DRAFT|SUBMITTED|IN_REVIEW|VERIFIED|REJECTED|EXPIRED),
                             current_version_id  -> the AUTHORITATIVE version,
                             reviewer_user_id?, rejection_reason?, expires_on?
nad_document_version         immutable: version_no, storage_key, content_type,
                             size_bytes, checksum_sha256, uploaded_by/at,
                             verification_status(PENDING|VERIFIED|REJECTED),
                             verified_by/at, scan_status(PENDING|CLEAN|INFECTED)
nad_document_event           append-only audit (event_type, actor_user_id, at, detail_json)
nad_document_requirement     scope(PROGRAM|SCHOLARSHIP|WF_STAGE), ref_id, doc_type,
                             mandatory, waivable, notes  -> per-application checklist
nad_application_document     M:N attach of applicant-owned docs to an application:
                             application_id, document_id, requirement_id?, added_by/at
```

Key rules (reconciled with `DOMAIN_MODEL.md` §8 Document):
- **No `documentIds[]` array anywhere.** A document belongs to its `applicant_id`
  (owner). Application-specific docs also carry `application_id`. Reusable
  applicant-level docs (passport) are attached to applications **only** via
  `nad_application_document`.
- **`current_version_id` is authoritative.** `nad_document.status` is **derived**
  (service, optionally a generated column) from that version's `verification_status`
  plus `expires_on`. Uploading a new version creates a new immutable
  `nad_document_version`, repoints `current_version_id`, and returns the document to
  `IN_REVIEW`. Old versions are never mutated or deleted.
- **Requirements → checklist.** On entering the collection stage the engine reads
  `nad_document_requirement` (program + scholarship + stage scope) and builds the
  per-application checklist. A `waivable` item may be `SKIPPED` by a
  `case_officer` with a recorded reason (also a `nad_application_task`). Workflow
  guards `ALL_REQUIRED_DOCUMENTS_ATTACHED` / `…_VERIFIED` read this checklist.
- **Document lifecycle:** `DRAFT → SUBMITTED → IN_REVIEW → VERIFIED | REJECTED |
  EXPIRED`. `REJECTED` requires `rejection_reason` (visibility `SHARED` — external
  users with `VIEW_DOCUMENT` see it). Internal review commentary is a
  `nad_application_note` `visibility='INTERNAL'` or a non-exposed `nad_document_event`
  — never surfaced externally.
- **Types** via `sys_dict` `nad_document_type`.

### 3.2 Storage — D5 APPROVED

**S3-compatible object storage behind a `DocumentStorage` SPI.**

| Env | Impl | Notes |
| --- | --- | --- |
| dev | `local` | hardened: real cross-platform base dir, **no** anonymous `/profile/**`, path-traversal guards, random keys. Lets devs run without MinIO. |
| staging | `s3` → **MinIO** | |
| prod | `s3` → **AWS S3** (or provider equivalent) | SSE, lifecycle rules, bucket versioning as defence in depth. |

`storage_key` layout: `nad/<applicantId>/<documentId>/<versionId>`. Bytes **never** in
MySQL. SDK: `software.amazon.awssdk:s3`. The impl is selected by Spring profile.

### 3.3 Access control (BASELINE)

- **No anonymous file URLs.** Every download goes through
  `GET /api/{student|staff}/documents/{id}/versions/{v}/content`, which:
  1. authorizes the caller — external: `nad_user_applicant_access` capability
     `VIEW_DOCUMENT` on the owning applicant (or the app's applicant); staff:
     `nad:document:download` + assignment scope;
  2. blocks download unless `scan_status='CLEAN'`;
  3. streams from storage **or** issues a short-TTL (≤ 60 s) pre-signed URL;
  4. writes a `nad_document_event` (`DOWNLOADED`).
- Students see their own documents + `rejection_reason`; never internal notes.
- `logo_document_id` / `agreement_document_id` etc. are resolved the same way — no
  public passthrough.

### 3.4 Upload hardening (BASELINE)

- Allow-list **per `doc_type`** (not one global list). Validate declared MIME against
  sniffed magic bytes.
- **Size:** default **20 MB** per file; per-type overrides (e.g. `ACADEMIC_TRANSCRIPT`
  up to 50 MB). Spring multipart limits raised to match (`max-file-size: 50MB`,
  `max-request-size: 60MB`) and enforced again in the service. *(Supersedes the RuoYi
  10 MB default.)*
- **Malware scan** (ClamAV or equivalent) → `scan_status`. `PENDING`/`INFECTED` files
  cannot be downloaded or verified.
- New random `storage_key`; client filename never used for storage paths.
- Responses: `Content-Disposition: attachment`, `X-Content-Type-Options: nosniff`;
  never served from an origin that can execute content.
- `@RepeatSubmit` on the upload endpoint; `@RateLimiter` per user.

### 3.5 Chat attachments (reconciled with COMMUNICATION §3)

A file attached in a conversation is a **lightweight blob**
(`nad_message_attachment.storage_key`, own object key) — **not** a governed
`nad_document` — unless a staff member explicitly **"promote to document"**, which
creates a `nad_document` (`doc_type='CORRESPONDENCE_ATTACHMENT'` or a chosen type)
owned by the applicant and sets `nad_message_attachment.promoted_document_id`. Keeps
the document table clean; keeps the governed set auditable.

### 3.6 Retention & expiry (PLANNED)

- `expires_on` per document (passport validity, 2-year language-test validity). A
  Quartz JDBC-store sweep flips expired docs to `EXPIRED`, notifies, and re-opens the
  relevant checklist item.
- Retention / erasure for withdrawn or archived applicants — ties to `SECURITY.md` §6
  (**OPEN**: data-residency + erasure policy).

## 4. Testing (PLANNED)

- New version keeps prior versions immutable; `current_version_id` repoints;
  status transitions legal only from allowed states; `REJECTED` without reason → 422.
- Authorization: cross-applicant fetch → 403/404; anonymous content URL → 401;
  student cannot read internal notes.
- Upload: disallowed MIME/extension, spoofed content-type, oversize, path-traversal
  filename, infected file (mock scanner) all rejected; `PENDING` scan blocks download.
- Requirements: checklist built from program+scholarship+stage requirements; guard
  `ALL_REQUIRED_DOCUMENTS_VERIFIED` reflects reality; `waivable` skip records a reason.
- Expiry sweep flips state, notifies, re-opens the checklist item.
