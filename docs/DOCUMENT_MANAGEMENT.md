# Nadoumi — Document Management

Status: **BASELINE** · **EXISTING** · **PLANNED** · **OPEN**.

> Reconciled with Rev 3 (D5, D11 approved) and **P1** (media/file-storage build,
> `docs/superpowers/specs/2026-09-03-media-storage-and-application-engine-design.md`
> Part I — **DM1 supersedes D5's provider choice**, D5's SPI *principle* is
> retained). Draft DDL: `docs/ddl/nad_core.draft.sql` §7 (dependency stub — the
> `nad_document` slice itself is still **PLANNED**, Step 7; the storage/media
> layer it will sit on is **EXISTING**, built in P1).

---

## 1. What exists today (EXISTING)

RuoYi ships **file handling**, not document management. Nadoumi's own P1 build
replaced the interim catalog-imagery approach with a real storage layer —
`nad_document` itself is not built yet (Step 7).

- `CommonController` `/common/**`: `upload`, `uploads`, `download`,
  `download/resource`. Local filesystem under `ruoyi.profile`
  (`application.yml` → `D:/ruoyi/uploadPath`, a Windows path — broken on macOS/Linux;
  never exercised). Files at `${profile}/upload/yyyy/MM/dd/<uuid>.<ext>`, served
  **anonymously** under `/profile/**`. Still used by RuoYi's own admin-avatar upload
  (out of Nadoumi's scope) — **not** used by any Nadoumi media/document path.
- `FileUploadUtils` / `MimeTypeUtils`: `DEFAULT_MAX_SIZE = 50 MB` but Spring multipart
  caps at 10 MB / 20 MB; extension allow-list (`.html` blocked); filename ≤ 100.
- **SUPERSEDED (P1).** Nadoumi's own interim catalog-imagery approach — `V21`
  `*_image_url` string columns populated via RuoYi's `/common/upload`, served from
  `/profile/...` — is superseded by the Cloudinary-backed media layer (§3.2).
  Those columns are kept for exactly one release as a deprecated read-only
  fallback (`docs/DATABASE_DESIGN.md` V21/V27 rows); `nadoumi-web` keeps
  `app/utils/media.ts` as a shim and the `server/routes/media/[...path].ts`
  Nitro proxy only for legacy relative paths — both tracked for removal.
- **No** document entity, versioning, verification, reviewer, expiry, audit, or
  business-level access control yet — `nad_document` (Step 7) is still **PLANNED**.
  Media *storage* (bytes, access classes, signed delivery, audit log) **is** built
  (P1, §3.2) and is what Step 7 will sit on.

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
nad_document_version         immutable: version_no, media_asset_id (-> nad_media_asset,
                             P1; supersedes the originally-planned storage_key),
                             content_type, size_bytes, checksum_sha256, uploaded_by/at,
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

### 3.2 Storage — DM1 (supersedes D5's provider choice; EXISTING, P1)

**Cloudinary is the managed storage layer** for all Nadoumi-owned uploaded media
and documents, in **every** environment including dev and test. MinIO / AWS S3
are **not** used; there is **no local-filesystem production path**. D5's
*principle* — a provider-agnostic SPI, bytes never in MySQL, downloads only
through an authorized API — is retained; the provider choice and the `local`/`s3`
impl split are dropped.

| Layer | What it is | Where |
| --- | --- | --- |
| `MediaStorageService` | Low-level provider SPI: `put` / `replace` / `publicUrl` / `signedUrl` / `openStream` / `purge` / `find` / `softDelete`. Pure JDK types, no Cloudinary type in the signature. | interface in `nadoumi-common` (`com.nadoumi.common.media`); impl `CloudinaryMediaStorage` in the new `nadoumi-media` module (foundational, peer of `nadoumi-identity`) |
| `MediaGateway` | Domain-facing façade: `upload` / `find` / `publicUrl` / `issueSignedUrl` / `openProxyStream` / `denyAndLog` / `softDelete`. This is what `nadoumi-document` (Step 7) and every catalog module call — never `MediaStorageService` directly. | interface in `nadoumi-common`; impl `MediaService` in `nadoumi-media` |
| `nad_media_asset` | The metadata + provider-reference registry. `Document ≠ File ≠ Cloudinary Asset` (DM4) — a future `nad_document_version.media_asset_id` FK points here, never at raw storage_key/URL bytes. | `docs/DATABASE_DESIGN.md` §5.6a |

No `application.yml` default for the credential — `CLOUDINARY_URL` must be
supplied per environment; `CloudinaryMediaStorage`'s constructor fails fast at
startup if it is absent or malformed. `NADOUMI_MEDIA_ENV` (`dev`/`staging`/`prod`,
default `dev`) prefixes the Cloudinary folder so one cloud hosts every
environment without collision. Bytes **never** in MySQL. Tests that must run
offline use an in-memory `FakeMediaStorage` test double — **no** test hits
Cloudinary or a real filesystem.

### 3.2a Access classes & delivery modes (EXISTING, P1)

Every `nad_media_asset` carries one of three access classes (`MediaAccessClass`),
set from a per-category default (`MediaCategoryPolicy`) with a doc-type override
for Step 7:

| Access class | Cloudinary `delivery_type` | Delivery | Used today for |
| --- | --- | --- | --- |
| **PUBLIC** | `upload` | `secure_url` stored on `nad_media_asset` and returned directly in catalog DTOs, or via `GET /api/media/{id}` (`@Anonymous`, 302 to `secure_url` for PUBLIC assets, 404 otherwise) | university logo/banner/gallery, scholarship hero/cover, programme image |
| **PROTECTED** | `authenticated` | **No URL ever stored.** Per-request short-TTL signed URL, issued only after Nadoumi authorization: `mediaStorageService.signedUrl(assetId, ttl)`, `ttl` from `nadoumi.media.signed-url-ttl-seconds` (default **180s**, clamped **[60, 600]**). `302` redirect, or `200 {url, expiresAt}` for `Accept: application/json` / `?json=1` | applicant profile photo (`APPLICANT_PHOTO`) today; passport/visa/financial/transcript-adjacent documents by default once Step 7 lands |
| **SENSITIVE** | `authenticated` (or `private` for `raw`) | **The client never receives a Cloudinary URL, signed or otherwise.** Nadoumi backend fetches the object server-side (`mediaStorageService.openStream`) and pipes it to the response (`Content-Type`, `Content-Length`, `Content-Disposition: attachment`, `Cache-Control: no-store`, `X-Content-Type-Options: nosniff`), bounded-buffer streamed, never buffered whole in memory | none live today (no SENSITIVE category is uploaded before Step 7); reserved for `JW202` and the Step 7 doc-type overrides `PASSPORT` / `VISA` / `FINANCIAL_PROOF` / `TRANSCRIPT` / `POLICE_CLEARANCE` |

**SENSITIVE proxy list (Step 7 doc types, category default PROTECTED overridden
to SENSITIVE):** `PASSPORT`, `VISA`, `FINANCIAL_PROOF`, `TRANSCRIPT`,
`POLICE_CLEARANCE`, plus the `JW202` media category (SENSITIVE by category
default, not an override). The override lives in the Step 7 doc-type dictionary;
`nad_media_asset.access_class` stores the resolved value.

Access-flow diagram: `docs/ARCHITECTURE.md` §7.1.

### 3.3 Access control

**Media/storage layer — EXISTING (P1).** Enforced **server-side, on every
request** (DM7); no reliance on URL possession, frontend guards, or "was granted
before". Every PROTECTED/SENSITIVE access writes one append-only
`nad_media_access_log` row (`docs/DATABASE_DESIGN.md` §5.6a): `access_kind`
(`SIGNED_URL_ISSUED` / `STREAM_PROXY` / `METADATA`), `result` (`GRANTED` /
`DENIED`), `deny_reason` on denial, `ttl_seconds` for signed-URL issues. The log
write is `REQUIRES_NEW` so a later `403` cannot roll it back. Public
`GET /api/media/{id}` (PUBLIC assets) is **not** logged.

**Revocation is immediate.** Authorization is re-resolved from source
(`NadoumiAccessService`, `sys_user_role`) on **every** request, never from a
cached decision. A signed URL already handed out has at most `TTL` seconds
(≤ 600s) of residual validity; SENSITIVE media has **no** residual window — every
byte goes through a fresh authorization. Deactivating an access grant, changing a
role, or archiving an applicant takes effect on the caller's next call.

**Student / on-behalf-of access matrix (BASELINE — Step 7 target; today's built
surface is the applicant-photo slice only):**

| Action | Allowed when |
| --- | --- |
| View an application's document list + statuses | applicant `A`'s application, and `U` holds `VIEW_APPLICATION` for `A` |
| Preview / download a document on that application | as above, **and** `U` holds `VIEW_DOCUMENT` for `A` |
| See a `REJECTED` document's `rejection_reason` (visibility `SHARED`) | same as download |
| Upload / replace a document (Step 7) | `U` holds `UPLOAD_DOCUMENT` for `A`, application in an upload-accepting stage |
| Access a document of a **different** applicant | **never** — `403`, logged `DENIED / NO_APPLICANT_GRANT` |
| View own profile photo (**built, P1**) | `GET /api/staff/applicants/{id}/photo` — requires `VIEW_PROFILE`, else `denyAndLog` + `403` |

**Staff access matrix (BASELINE):**

| Action | Allowed when |
| --- | --- |
| View documents on an application | `nad:application:view` **and** in scope — `ops_manager`/`nadoumi_super_admin`: all; `case_officer`/`document_reviewer`: assignee or department queue (`PERMISSION_CATALOGUE.md` §4) |
| Preview / download (PROTECTED signed URL or SENSITIVE proxy) | as above; every access logged |
| Verify / reject / request replacement (Step 7) | `nad:document:verify` + application in scope |
| Upload/replace an applicant photo (**built, P1**) | `nad:applicant:edit` |
| Export | `nad:application:export` |

**No anonymous file URLs** — every protected/sensitive access is server-mediated
(§3.2a). `logo_document_id` / `agreement_document_id`-style fields resolve through
`MediaGateway.publicUrl` (PUBLIC assets only) — no public passthrough for
anything protected or sensitive.

**Document lifecycle (Step 7, PLANNED — not yet built):** once `nad_document`
exists, `GET /api/{student|staff}/documents/{id}/versions/{v}/content` will apply
the same authorization + log + delivery pattern above, additionally: block
download unless `scan_status='CLEAN'`; write a `nad_document_event`
(`DOWNLOADED`) alongside the `nad_media_access_log` row.

### 3.4 Upload hardening

**Media layer — EXISTING (P1).** Server-side proxied upload (DM6) — bytes transit
the app server and are fully validated in `MediaValidation` **before** any
Cloudinary call:

1. **Auth** — the endpoint's own `@PreAuthorize`/capability check for the owner
   (e.g. `nad:university:edit`, `EDIT_PROFILE` for the applicant photo).
2. **Size** — reject over the category's `maxBytes` before buffering the whole
   payload (streamed count; also a hard Spring multipart cap).
3. **Filename** — strip path separators/control chars/leading dots, collapse to
   `[A-Za-z0-9._-]`, max **100 chars**; empty → generated `upload-<uuid>`.
4. **Declared-type allow-list** — the part's `Content-Type` must be in the
   category's allow-list.
5. **Magic-byte sniff** — Apache Tika on the first 8 KiB; sniffed type must be in
   the allow-list **and** match the declared type's family (image↔image,
   pdf↔pdf); mismatch → `422`.
6. **Hard denylist regardless of category:** `text/html`, `image/svg+xml`,
   `application/xhtml+xml`, `application/x-msdownload`, `application/x-sh`, `zip`,
   `java-archive` — anything executable.
7. **Checksum** — SHA-256 streamed for document-shaped categories (dedupe +
   tamper evidence); optional for images.
8. `MediaStorageService.put(cmd)` → Cloudinary upload with the resolved
   `delivery_type` / `resource_type` / folder → persist `nad_media_asset` → return
   `{mediaId, url?}`.

**Per-category policy (`MediaCategoryPolicy`):**

| Category | Default access class | Allowed MIME | Max size | `resource_type` |
| --- | --- | --- | --- | --- |
| `UNIVERSITY_LOGO` | PUBLIC | jpeg, png, webp | 4 MB | image |
| `UNIVERSITY_BANNER` | PUBLIC | jpeg, png, webp | 8 MB | image |
| `UNIVERSITY_GALLERY` | PUBLIC | jpeg, png, webp | 8 MB | image |
| `SCHOLARSHIP_HERO` | PUBLIC | jpeg, png, webp | 8 MB | image |
| `SCHOLARSHIP_COVER` | PUBLIC | jpeg, png, webp | 6 MB | image |
| `PROGRAM_IMAGE` | PUBLIC | jpeg, png, webp | 6 MB | image |
| `APPLICANT_PHOTO` | PROTECTED | jpeg, png, webp | 5 MB | image |
| `APPLICANT_DOCUMENT` | PROTECTED | pdf, jpeg, png | 20 MB | raw / image |
| `APPLICATION_DOCUMENT` | PROTECTED | pdf, jpeg, png | 20 MB | raw / image |
| `ADMISSION_DOCUMENT` | PROTECTED | pdf, jpeg, png | 20 MB | raw / image |
| `JW202` | **SENSITIVE** | pdf, jpeg, png | 20 MB | raw / image |
| `OTHER_ATTACHMENT` | PROTECTED | pdf, jpeg, png | 20 MB | raw / image |

Global ceiling `NADOUMI_MEDIA_MAX_UPLOAD_MB` (default **20**) also sets
`spring.servlet.multipart.max-file-size` (20MB) / `max-request-size` (22MB). Only
`UNIVERSITY_LOGO`/`_BANNER`/`_GALLERY`, `SCHOLARSHIP_HERO`/`_COVER`,
`PROGRAM_IMAGE`, and `APPLICANT_PHOTO` are live today (catalog retrofit +
applicant photo); the `*_DOCUMENT` / `JW202` / `OTHER_ATTACHMENT` rows are
reserved for Step 7.

**Not yet built:** malware scanning (`scan_status` column reserved on
`nad_document_version`, Step 7 concern per the P1 spec's explicit scope
exclusion) and per-`doc_type` overrides beyond the category table above.

- `@RepeatSubmit` on upload endpoints; `@RateLimiter` per user (existing RuoYi AOP,
  applied to the new upload controllers).

### 3.4a Versioning, replacement, deletion (EXISTING — media layer, P1)

- **Replace** (`MediaStorageService.replace`) uploads a new Cloudinary object,
  inserts a new `nad_media_asset` row, sets the old row `status='SUPERSEDED'` +
  `superseded_by=<new id>`. The owning entity's `*_media_id` is repointed by the
  domain service in the same transaction. Documents (Step 7): a "new version" is
  always a new `nad_document_version` + new asset — assets behind a version are
  **never** replaced in place.
- **Soft delete** (`softDelete`) sets `status='DELETED'` + `deleted_at`/`deleted_by`;
  the caller nulls the domain `*_media_id`. `MediaReconciliationJob` (Quartz,
  seeded **paused** at `V29`) issues the Cloudinary `destroy` after a grace period
  and hard-deletes the row — unless a `nad_media_access_log` / future
  `nad_document_version` still references it, in which case only the Cloudinary
  object is destroyed and the row moves to `status='PURGED'`.
- **Retention policy by category:** documents — **never** auto-purge superseded
  assets; catalog images — purge `SUPERSEDED` rows after 30 days.

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
