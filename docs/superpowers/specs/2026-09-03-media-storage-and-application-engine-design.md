# Nadoumi — Media/File Storage + Application Engine — Engineering Specification

Status: **DRAFT — for review**. Author: Claude Sonnet 5. Date: 2026-09-03.
Session: https://claude.ai/code/session_01ERVUQU6PwTfB1Rwo84ycrW

> This spec covers two coupled changes agreed in the design conversation:
>
> - **Part I — Media & File Storage.** A provider-agnostic `MediaStorageService`
>   abstraction with **Cloudinary** as the sole production implementation. No binary
>   in MySQL, no local-filesystem production fallback. Public media and
>   private/protected documents are separated and delivered differently.
> - **Part II — Application Engine & Workflow (platform Step 6).** The
>   `nadoumi-application` module: the data-driven workflow engine (D4), two seeded
>   workflow definitions, profile/requirement snapshots, and the public / student /
>   staff surfaces.
>
> Part I lands first because Part II's document handling (application / admission /
> JW202 attachments) consumes it. Part II keeps **manual staff-override guards** for
> documents and payment until the `nadoumi-document` (Step 7) and `nadoumi-payment`
> (Step 9) modules exist.

---

## 0. Scope

**In scope**

1. `nadoumi-media` module + `MediaStorageService` SPI (interface in `nadoumi-common`)
   + `CloudinaryMediaStorage` implementation.
2. `nad_media_asset` registry + `nad_media_access_log` audit table.
3. Server-side proxied upload with full boundary validation (MIME allow-list,
   magic-byte sniffing, per-category size caps, filename sanitisation).
4. Three delivery modes: PUBLIC direct URL, PROTECTED short-TTL signed URL, SENSITIVE
   Nadoumi stream-proxy.
5. Retrofit of existing catalog imagery (university logo/banner/gallery, scholarship
   hero/cover, program images) onto `nad_media_asset` / Cloudinary; removal of the
   interim `/profile/...` path approach (commit `36a5273f`).
6. Applicant profile photo (`APPLICANT_PHOTO`, PROTECTED) — the first protected media
   use, proving the signed-URL path end to end in Part I.
7. `nadoumi-application` module: `nad_wf_*` definition tables, `nad_application*`
   instance tables, `WorkflowService`, two seeded definitions
   (`PROGRAM_WITH_SCHOLARSHIP_V1`, `PROGRAM_ONLY_V1`), definition-validity checks.
8. Public **Apply Now** flow (self + on-behalf-of), student "My applications" portal,
   admin application workbench.
9. Documentation updates: `DATABASE_DESIGN.md`, `DOCUMENT_MANAGEMENT.md`,
   `ARCHITECTURE.md`, `API_DESIGN.md`, `SECURITY.md`, `DEPLOYMENT.md`,
   `APPLICATION_WORKFLOW.md`, `WORKFLOW_PROGRAM_WITH_SCHOLARSHIP.md`,
   `PLATFORM_ARCHITECTURE.md §8`, `FRONTEND_ARCHITECTURE.md`, `ADMIN_ARCHITECTURE.md`,
   `PERMISSION_CATALOGUE.md`.

**Explicitly out of scope (deferred, tracked)**

- The full `nadoumi-document` module — `nad_document` / `nad_document_version` /
  verification / reviewer / expiry (platform Step 7). The **document-access
  authorization architecture** is specified here (§I.7) so Step 7 implements against
  it; the `nad_document` tables and the applicant-facing document upload UI are not
  built in this spec.
- `nadoumi-payment` and a real `PAYMENT_SETTLED` guard (Step 9).
- Signed direct browser→Cloudinary upload (a later optimisation; the schema does not
  need to change for it).
- Virus/malware scanning of uploads (`scan_status` column is reserved;
  integration is a Step 7 concern).
- Migrating RuoYi `sys_user.avatar` to Cloudinary (RuoYi-owned; separate change).

---

## 1. Decisions recorded

| # | Decision | Supersedes / relates to |
| --- | --- | --- |
| **DM1** | **Cloudinary is the managed storage layer** for all Nadoumi-owned uploaded media and documents. MinIO/AWS S3 are **not** used. | **Supersedes D5's provider choice.** D5's *principle* (provider-agnostic SPI, bytes never in MySQL, downloads only through an authorized API) is retained. |
| **DM2** | The storage abstraction is `MediaStorageService` (interface + value types in `nadoumi-common`; `CloudinaryMediaStorage` impl in a new `nadoumi-media` module). The domain never references a Cloudinary type. | D5 (SPI in `nadoumi-common`) |
| **DM3** | **No local-filesystem storage as a production path.** Dev and test also use Cloudinary (a dedicated unsigned-disabled dev cloud / folder prefix per environment). Tests that must run offline use a `FakeMediaStorage` in-memory test double, never a real filesystem impl. | A3, D5 `local` impl |
| **DM4** | `Document ≠ File ≠ Cloudinary Asset`. `nad_media_asset` is a **metadata + provider-reference registry**, owned by `nadoumi-media`. Business entities (`nad_university`, `nad_scholarship`, `nad_document` …) hold a `*_media_id` FK, never Cloudinary fields. | CLAUDE.md §11 |
| **DM5** | Three **access classes** on every asset: `PUBLIC` (direct `secure_url`), `PROTECTED` (short-TTL signed URL, issued only after Nadoumi authz), `SENSITIVE` (Nadoumi backend stream-proxy only — no Cloudinary URL ever reaches the client). | SECURITY.md §; user requirement |
| **DM6** | **Server-side proxied upload.** Bytes transit the app server and are validated (allow-list + magic bytes + size + filename) before the Cloudinary call. | user decision |
| **DM7** | Every PROTECTED / SENSITIVE access (URL issued *or* proxied) writes an append-only `nad_media_access_log` row (`GRANTED` / `DENIED`). Authorization is re-checked on **every** request — a revoked grant or role denies immediately, regardless of prior access. | SECURITY.md §13 |
| **DM8** | New module **`nadoumi-modules/nadoumi-media`** — a foundational infra module (peer of `nadoumi-identity`), not in the CLAUDE.md §15 domain list. This spec is its decision record. | architecture.md "module per domain" |
| **DA1** | `nadoumi-application` uses the **data-driven workflow engine** (config tables + `WorkflowService`), not a BPMN engine. | **D4 (already approved)** — restated |
| **DA2** | Two definitions seeded: `PROGRAM_WITH_SCHOLARSHIP_V1` (13 stages, per `WORKFLOW_PROGRAM_WITH_SCHOLARSHIP.md`) and `PROGRAM_ONLY_V1` (a scholarship-free sibling). | user decision |
| **DA3** | The guard evaluator implements every predicate name in the fixed set. `ALL_REQUIRED_DOCUMENTS_ATTACHED` / `…_VERIFIED` and `PAYMENT_SETTLED(kind)` dispatch to **provider interfaces with no implementation yet**; `activate` **rejects** any definition referencing an unbacked predicate (fail-closed). The seeded V1 definitions instead gate those steps on `DECISION_RECORDED(DOCUMENTS_COMPLETE,{CONFIRMED})` / `DECISION_RECORDED(FEE_SETTLED,{CONFIRMED})` — append-only decisions a `case_officer` / `finance` records with a mandatory rationale. | user decision; APPLICATION_WORKFLOW.md §3.1 |
| **DA4** | On `submit`, an **immutable** `nad_application_snapshot` (`PROFILE` + `REQUIREMENTS`) is written. The Apply Now form edits the **live** applicant profile up to submit; the snapshot freezes it. | user "snapshot strategy unchanged" |
| **DA5** | Apply Now works for the caller's own applicant profile **and** any applicant they hold a `nad_user_applicant_access` grant carrying `SUBMIT_APPLICATION` / `CREATE_APPLICATION` for. Authorization is server-side, per request. | user decision |
| **DA6** | Application types in this build: `PROGRAM_WITH_SCHOLARSHIP` and `PROGRAM_ONLY`. `SCHOLARSHIP_ONLY` is deferred. | user decision |

---

# PART I — Media & File Storage

## I.1 Concept model

```
Business entity            Registry                     Physical object
─────────────────          ────────────────────         ────────────────────
nad_university.logo_media_id ─┐
nad_scholarship.hero_media_id ┤
nad_applicant.photo_media_id  ┼──►  nad_media_asset  ───►  Cloudinary asset
nad_document_version          ┘     (metadata + refs)      (bytes; upload |
  .media_asset_id (Step 7)                                  authenticated | private)
```

- A **Document** (Step 7) is a Nadoumi business entity: type, owner applicant,
  application link, verification, reviewer, expiry, lifecycle, audit. It points at
  one or more immutable **versions**, each of which points at exactly one
  **media asset**.
- A **media asset** (`nad_media_asset`) is the Nadoumi record of one stored object:
  provider refs + metadata + access class + owner + audit columns. It is **not** the
  file and **not** Cloudinary's own DB row.
- A **Cloudinary asset** is the bytes plus Cloudinary's delivery semantics. Nadoumi
  code touches it only through `MediaStorageService`.

## I.2 `nadoumi-media` module & the SPI

**`nadoumi-common`** (no new dependencies) gains the abstraction:

```java
package com.nadoumi.common.media;

public interface MediaStorageService {
    /** Upload already-validated bytes and persist a nad_media_asset row. */
    StoredAsset put(MediaUploadCommand cmd);

    /** Upload a replacement object for an existing asset; the old asset row is
     *  marked SUPERSEDED and retained (audit). Returns the new asset. */
    StoredAsset replace(long assetId, MediaUploadCommand cmd);

    /** PUBLIC assets only — the stored secure_url. Throws for PROTECTED/SENSITIVE. */
    String publicUrl(long assetId);

    /** PROTECTED assets only — a freshly signed, short-TTL delivery URL.
     *  Throws for PUBLIC (use publicUrl) and SENSITIVE (use openStream). */
    SignedUrl signedUrl(long assetId, Duration ttl);

    /** PROTECTED or SENSITIVE — opens the provider object for backend proxying.
     *  The caller is responsible for having authorized the request. */
    ProxyStream openStream(long assetId);

    Optional<StoredAsset> find(long assetId);

    /** Soft-delete: row status=DELETED + deleted_at/by; provider destroy is async. */
    void softDelete(long assetId, long actorUserId);
}

public record MediaUploadCommand(
        java.io.InputStream source,          // consumed once
        String originalFilename,
        String declaredContentType,
        long byteSize,
        MediaCategory category,
        MediaAccessClass accessClass,        // may be null → category default
        MediaOwnerRef owner,                 // {kind, id}
        long uploadedBy,
        String checksumSha256                // nullable; computed by caller if required
) {}

public record StoredAsset(
        long id, String provider, MediaAccessClass accessClass, MediaCategory category,
        String resourceType, String deliveryType, String publicId, String assetId,
        Long cloudVersion, String secureUrl,           // secureUrl non-null only for PUBLIC
        String originalFilename, String contentType, long byteSize,
        Integer width, Integer height, String checksumSha256,
        long uploadedBy, MediaOwnerRef owner, String status,
        java.time.Instant createdAt) {}

public record SignedUrl(String url, java.time.Instant expiresAt) {}
public record ProxyStream(java.io.InputStream body, String contentType, long contentLength,
                          String downloadFilename) {}
public record MediaOwnerRef(MediaOwnerKind kind, long id) {}

public enum MediaAccessClass { PUBLIC, PROTECTED, SENSITIVE }
public enum MediaOwnerKind { UNIVERSITY, SCHOLARSHIP, PROGRAM, APPLICANT, APPLICATION, DOCUMENT, USER }
public enum MediaCategory {
    UNIVERSITY_LOGO, UNIVERSITY_BANNER, UNIVERSITY_GALLERY,
    SCHOLARSHIP_HERO, SCHOLARSHIP_COVER, PROGRAM_IMAGE,
    APPLICANT_PHOTO,
    APPLICANT_DOCUMENT, APPLICATION_DOCUMENT, ADMISSION_DOCUMENT, JW202, OTHER_ATTACHMENT
}
```

**`nadoumi-modules/nadoumi-media`** (new; depends on `nadoumi-common` +
`com.cloudinary:cloudinary-http5` + mybatis + web + validation):

- `CloudinaryMediaStorage implements MediaStorageService` — the only production impl.
- `MediaAsset` entity + `MediaAssetMapper` (+ XML) — owns `nad_media_asset`.
- `MediaAccessLogMapper` — append-only writes to `nad_media_access_log`.
- `MediaValidation` — the boundary validator (§I.5).
- `MediaController` — `GET /api/media/{assetId}` for PUBLIC assets (redirect to
  `secure_url`; mostly a convenience/indirection point). PROTECTED / SENSITIVE
  delivery endpoints live with the owning module (catalog, applicant, document),
  which do the domain authorization then call `signedUrl` / `openStream`.
- `MediaReconciliationJob` (Quartz) — sweeps `status='DELETED'` rows older than N
  days, issues the Cloudinary `destroy`, then hard-deletes the row. Also flags
  `nad_media_asset` rows whose owner no longer exists.
- `MediaCategoryPolicy` — the static category → {default access class, allowed MIME
  set, max bytes, `resource_type`, Cloudinary folder} table (§I.5, §I.6).

`ruoyi-admin` adds `nadoumi-media` as a dependency so the bean is on the runtime
classpath. Catalog / applicant / (later) document modules depend only on
`nadoumi-common`'s interface and receive `MediaStorageService` by injection.

### Cross-module rule

`nadoumi-university` / `-scholarship` / `-applicant` **never** query
`nad_media_asset` directly. They store a `*_media_id` `bigint` and call
`mediaStorageService.find(id)` / `.publicUrl(id)`. `nad_media_asset` FK ownership
stays inside `nadoumi-media`.

## I.3 `nad_media_asset` (migration V26)

| Column | Type | Notes |
| --- | --- | --- |
| `id` | `bigint` PK auto | |
| `provider` | `varchar(24)` NN default `'CLOUDINARY'` | future-proofing only |
| `access_class` | `varchar(16)` NN | `PUBLIC` \| `PROTECTED` \| `SENSITIVE` |
| `category` | `varchar(40)` NN | `MediaCategory` |
| `resource_type` | `varchar(12)` NN | Cloudinary `image` \| `raw` \| `video` |
| `delivery_type` | `varchar(16)` NN | `upload` (PUBLIC) \| `authenticated` (PROTECTED/SENSITIVE) |
| `public_id` | `varchar(255)` NN | Cloudinary `public_id` (includes folder) |
| `asset_id` | `varchar(64)` NULL | Cloudinary `asset_id` |
| `cloud_version` | `bigint` NULL | Cloudinary `version` |
| `secure_url` | `varchar(1024)` NULL | **populated only when `access_class='PUBLIC'`** |
| `folder` | `varchar(255)` NULL | |
| `original_filename` | `varchar(255)` NN | sanitised |
| `content_type` | `varchar(128)` NN | sniffed value, not the client's claim |
| `byte_size` | `bigint` NN | |
| `width` | `int` NULL | images |
| `height` | `int` NULL | images |
| `checksum_sha256` | `char(64)` NULL | set for documents; optional for images |
| `uploaded_by` | `bigint` NN | `sys_user.user_id` |
| `owner_kind` | `varchar(16)` NN | `MediaOwnerKind` |
| `owner_id` | `bigint` NN | |
| `status` | `varchar(16)` NN default `'ACTIVE'` | `ACTIVE` \| `SUPERSEDED` \| `DELETED` |
| `superseded_by` | `bigint` NULL | FK → `nad_media_asset.id` (on `replace`) |
| `create_by` / `create_time` / `update_by` / `update_time` | RuoYi `BaseEntity` | |
| `deleted_at` | `datetime` NULL | |
| `deleted_by` | `bigint` NULL | |

Constraints / indexes:
- `uk_media_provider_public_id (provider, public_id)`
- `idx_media_owner (owner_kind, owner_id)`
- `idx_media_category (category)`
- `idx_media_status (status)`
- FK `superseded_by → nad_media_asset(id)` `ON DELETE SET NULL`
- No FK from `nad_media_asset` to owner tables (polymorphic owner); referential
  integrity of `*_media_id` FKs is enforced from the owning table side.

## I.4 `nad_media_access_log` (migration V26)

Append-only. One row per PROTECTED/SENSITIVE access attempt (issue signed URL, or
proxy stream, or metadata read of a protected asset).

| Column | Type | Notes |
| --- | --- | --- |
| `id` | `bigint` PK auto | |
| `media_asset_id` | `bigint` NN | FK → `nad_media_asset(id)` `ON DELETE RESTRICT` |
| `document_id` | `bigint` NULL | set once Step 7 exists |
| `application_id` | `bigint` NULL | context, when the access is application-scoped |
| `actor_user_id` | `bigint` NN | authenticated principal |
| `actor_applicant_id` | `bigint` NULL | which applicant identity the actor used |
| `access_kind` | `varchar(20)` NN | `SIGNED_URL_ISSUED` \| `STREAM_PROXY` \| `METADATA` |
| `result` | `varchar(8)` NN | `GRANTED` \| `DENIED` |
| `deny_reason` | `varchar(120)` NULL | e.g. `NO_APPLICANT_GRANT`, `ROLE_MISSING`, `APP_NOT_VISIBLE` |
| `ttl_seconds` | `int` NULL | for `SIGNED_URL_ISSUED` |
| `ip` | `varchar(45)` NULL | |
| `user_agent` | `varchar(255)` NULL | truncated |
| `created_at` | `datetime` NN | |

Indexes: `idx_mal_asset (media_asset_id, created_at)`,
`idx_mal_app (application_id, created_at)`,
`idx_mal_actor (actor_user_id, created_at)`.

Never mutated. Retained per the platform data-retention policy (DEPLOYMENT.md).

## I.5 Upload — server-side proxied (DM6)

**Endpoint shape** (each owning module exposes its own, all identical in mechanics):

```
POST /api/staff/universities/{id}/logo         multipart: file           → {mediaId, url}
POST /api/staff/scholarships/{id}/hero          multipart: file          → {mediaId, url}
POST /api/student/applicants/{id}/photo         multipart: file          → {mediaId}      (PROTECTED — no url)
POST /api/staff/applications/{id}/documents ... (Step 7)
```

**Boundary pipeline (before any Cloudinary call)** — implemented once in
`MediaValidation`, invoked by every upload controller:

1. **Auth** — the controller's `@PreAuthorize` / capability check for that owner
   (e.g. `nad:university:edit`; `EDIT_PROFILE` for an applicant photo).
2. **Size** — reject `> MediaCategoryPolicy.maxBytes(category)` before buffering the
   whole payload (streamed count; hard cap also set on `spring.servlet.multipart`).
3. **Filename** — strip path separators, control chars, leading dots; collapse to
   `[A-Za-z0-9._-]`, max 100 chars; empty → generated `upload-<uuid>`.
4. **Declared type allow-list** — `Content-Type` of the part must be in
   `policy.allowedMime(category)`.
5. **Magic-byte sniff** — Apache Tika `Detector` (or a compact signature table) on
   the first 8 KiB. The sniffed type must be in the allow-list **and** consistent
   with the declared type (image↔image, pdf↔pdf). Mismatch → `422`.
6. **Hard denylist** regardless of category: `text/html`, `image/svg+xml`,
   `application/x-msdownload`, archives, anything executable.
7. **Checksum** — SHA-256 computed while streaming for `*_DOCUMENT` / `JW202` /
   `ADMISSION_DOCUMENT` categories (used for dedupe + tamper evidence). Optional for
   images.
8. `MediaStorageService.put(cmd)` → Cloudinary upload with the resolved
   `delivery_type`, `resource_type`, folder → persist `nad_media_asset` → return.

**Per-category policy** (`MediaCategoryPolicy`, also mirrored in SECURITY.md):

| Category | Default access class | Allowed MIME | Max size | `resource_type` | Folder |
| --- | --- | --- | --- | --- | --- |
| `UNIVERSITY_LOGO` | PUBLIC | jpeg, png, webp | 4 MB | image | `nadoumi/<env>/university/logo` |
| `UNIVERSITY_BANNER` | PUBLIC | jpeg, png, webp | 8 MB | image | `…/university/banner` |
| `UNIVERSITY_GALLERY` | PUBLIC | jpeg, png, webp | 8 MB | image | `…/university/gallery` |
| `SCHOLARSHIP_HERO` | PUBLIC | jpeg, png, webp | 8 MB | image | `…/scholarship/hero` |
| `SCHOLARSHIP_COVER` | PUBLIC | jpeg, png, webp | 6 MB | image | `…/scholarship/cover` |
| `PROGRAM_IMAGE` | PUBLIC | jpeg, png, webp | 6 MB | image | `…/program` |
| `APPLICANT_PHOTO` | PROTECTED | jpeg, png, webp | 5 MB | image | `…/applicant/photo` |
| `APPLICANT_DOCUMENT` | PROTECTED | pdf, jpeg, png | 20 MB | raw / image | `…/applicant/doc` |
| `APPLICATION_DOCUMENT` | PROTECTED | pdf, jpeg, png | 20 MB | raw / image | `…/application/doc` |
| `ADMISSION_DOCUMENT` | PROTECTED | pdf, jpeg, png | 20 MB | raw / image | `…/application/admission` |
| `JW202` | **SENSITIVE** | pdf, jpeg, png | 20 MB | raw / image | `…/application/jw202` |
| `OTHER_ATTACHMENT` | PROTECTED | pdf, jpeg, png | 20 MB | raw / image | `…/application/other` |

Document **doc-types** `PASSPORT`, `VISA`, `FINANCIAL_PROOF`, `TRANSCRIPT`,
`POLICE_CLEARANCE` (Step 7) override their asset's access class to **SENSITIVE**
even though the category default is PROTECTED. The override lives in the Step 7
doc-type dictionary; `nad_media_asset.access_class` stores the resolved value.

`<env>` ∈ `dev` \| `staging` \| `prod`, from config, so environments never collide
in one Cloudinary cloud.

## I.6 Delivery (DM5)

### PUBLIC
- Cloudinary `type: upload`. `secure_url` is stored on `nad_media_asset` and returned
  in public/staff DTOs (`logoUrl`, `coverUrl`, `gallery[].url`, …).
- Optional Nadoumi indirection: `GET /api/media/{id}` → `302` to `secure_url`
  (lets us swap providers or add signing later without changing stored links). The
  catalog DTOs will carry the **direct `secure_url`** for SEO/SSR performance;
  `/api/media/{id}` exists as the stable alternative.
- `nadoumi-web` renders these directly; the interim Nitro `/media` proxy
  (`server/routes/media/[...path].ts`) and `mediaUrl()` helper are **removed** for
  catalog images (they now come back as absolute `https://res.cloudinary.com/...`).

### PROTECTED
- Cloudinary `type: authenticated`. **No URL is ever stored or put in a list DTO.**
- Delivery endpoint (owned by the domain module), e.g.
  `GET /api/student/applicants/{id}/photo` /
  `GET /api/staff/applications/{appId}/documents/{docId}/content` (Step 7):
  1. domain authorization (§I.7);
  2. `nad_media_access_log` row (`SIGNED_URL_ISSUED`, `GRANTED`/`DENIED`);
  3. on grant: `mediaStorageService.signedUrl(assetId, TTL)` where
     `TTL = 180s` (config `nadoumi.media.signed-url-ttl-seconds`, 60–600);
  4. `302` redirect to the signed URL, **or** `200 {url, expiresAt}` when the
     request carries `Accept: application/json` / `?json=1` (SPA use).
- The signed URL is single-purpose (one `public_id`, one resource type), unguessable,
  and expires. It is *not* embeddable in a cacheable page.

### SENSITIVE
- Cloudinary `type: authenticated` (or `private` for `raw`). **The client never
  receives a Cloudinary URL, signed or otherwise.**
- Delivery endpoint streams:
  1. domain authorization (§I.7);
  2. `nad_media_access_log` row (`STREAM_PROXY`, `GRANTED`/`DENIED`);
  3. `mediaStorageService.openStream(assetId)` → Nadoumi fetches the object from
     Cloudinary server-side (short-lived internal signed URL, never surfaced) and
     pipes it to the response with
     `Content-Type`, `Content-Length`,
     `Content-Disposition: attachment; filename="<original>"`,
     `Cache-Control: no-store`, `X-Content-Type-Options: nosniff`.
  4. Stream is copied with a bounded buffer; the app-server does not buffer the
     whole file in memory.
- Bandwidth cost is accepted for this class (passport, visa, financial, transcript,
  JW202).

### Access-flow (all protected/sensitive)

```
Student / Staff request
        │
        ▼
Nadoumi authN (JWT / session)            ── principal, roles, applicant grants
        │
        ▼
Domain authorization (§I.7)              ── ownership · on-behalf grant · staff role · app visibility
        │  deny ─────────────► 403  + nad_media_access_log(result=DENIED, deny_reason)
        │ grant
        ▼
nad_media_access_log(result=GRANTED)
        │
        ├─ PROTECTED  → MediaStorageService.signedUrl(ttl)  → 302 / {url,expiresAt}
        └─ SENSITIVE  → MediaStorageService.openStream()    → Nadoumi pipes bytes
                                                               (no URL to client)
```

## I.7 Document / media access authorization

Enforced **server-side, on every request** (DM7). No reliance on URL possession,
frontend guards, or "was granted before".

### Student / on-behalf-of actor

Given principal `U` acting as applicant identity `A` (own profile, or an
applicant `A` for which `U` holds a live `nad_user_applicant_access` grant):

| Action | Allowed when |
| --- | --- |
| View an application's document list + statuses (`REQUIRED` / `UPLOADED` / `IN_REVIEW` / `VERIFIED` / `REJECTED`) | the application's `applicant_id = A` **and** `U` holds `VIEW_APPLICATION` for `A` |
| Preview / download a document on that application | as above **and** the asset's owner resolves to `A` (own applicant docs) or the doc is attached to that application; **and** `U` holds `VIEW_DOCUMENT` for `A` |
| See a `REJECTED` document's `rejection_reason` (visibility `SHARED`) | same as download |
| Upload / replace a document (Step 7) | `U` holds `UPLOAD_DOCUMENT` for `A` and the application is in a stage that accepts uploads |
| Access a document of a **different** applicant | **never** — `403`, logged `DENIED / NO_APPLICANT_GRANT` |

An application is visible to the student side **after submit** and while in any
non-`DRAFT` stage (and in `DRAFT` to the creator). Internal notes, decision
rationales and staff-only events are **never** in the student timeline.

### Staff actor

| Action | Allowed when |
| --- | --- |
| View documents on an application | `nad:application:view` **and** the application is in the caller's scope — `ops_manager` / `nadoumi_super_admin`: all; `case_officer` / `document_reviewer`: `assignee_user_id = self` **or** the application is in their department queue (`PERMISSION_CATALOGUE.md §4`) |
| Preview / download (PROTECTED signed URL or SENSITIVE proxy) | as above; every access logged |
| Verify / reject / request replacement (Step 7) | `nad:document:verify` (or role equivalent) **and** application in scope |
| View verification history + metadata | `nad:application:view` + in scope |
| Export | `nad:application:export` (sensitive perm) |

### Revocation is immediate

- On each request the delivery endpoint re-resolves grants + roles from source
  (`NadoumiAccessService`, `sys_user_role`), never from a cached decision.
- A signed URL already handed out has at most `TTL` seconds of residual validity
  (≤ 600 s, config). For SENSITIVE media there is **no** residual window — every
  byte goes through a fresh authorization.
- Deactivating a `nad_user_applicant_access` grant, changing a role, or archiving an
  applicant takes effect on the next call.

## I.8 Versioning, replacement, deletion

- **Replace** (`MediaStorageService.replace`) uploads a new Cloudinary object,
  inserts a new `nad_media_asset` row, sets the old row `status='SUPERSEDED'`,
  `superseded_by=<new id>`. The owning entity's `*_media_id` is repointed by the
  domain service in the same transaction. Old assets are retained (audit) and cleaned
  by the reconciliation job only if policy says so (documents: **never** auto-purge;
  catalog images: purge SUPERSEDED after 30 days).
- **Soft delete** (`softDelete`) sets `status='DELETED'`, `deleted_at/by`. The
  domain `*_media_id` is nulled by the caller. `MediaReconciliationJob` issues the
  Cloudinary `destroy` after the grace period, then hard-deletes the row (unless a
  `nad_media_access_log` / `nad_document_version` still references it — then the row
  is kept, only the Cloudinary object is destroyed and `status='PURGED'`).
- Document versions (Step 7) are **immutable**: a "new version" is a new
  `nad_document_version` + new asset; assets behind a version are never replaced
  in place.

## I.9 Catalog image retrofit (in Part I / P1)

**Migrations**

- **V27 `nad_catalog_media_fk.sql`**
  - `nad_university`: add `logo_media_id bigint null`, `banner_media_id bigint null`
    (FK → `nad_media_asset(id)` `ON DELETE SET NULL`). Keep `logo_image_url` /
    `cover_image_url` columns **for one release** as read fallback; mark deprecated.
  - `nad_university_gallery`: add `media_id bigint null` (FK, `ON DELETE SET NULL`);
    keep `image_url` as deprecated fallback.
  - `nad_scholarship`: add `hero_media_id bigint null`, `cover_media_id bigint null`
    (FK). Keep `hero_image_url` / `cover_image_url` as deprecated fallback.
  - `nad_program`: add `image_media_id bigint null` (FK) — new capability
    (programmes had no image field before).
  - **Recreate `v_scholarship_student`** to expose `hero_media_id` / `cover_media_id`
    (the student view resolves to `secure_url` in the service layer, still no
    university/partnership leakage — these are PUBLIC assets).
- **V28 `nad_applicant_photo.sql`** — `nad_applicant`: add `photo_media_id bigint
  null` (FK → `nad_media_asset(id)` `ON DELETE SET NULL`).
- No dev backfill (dev DB has no production uploads; any interim `/profile` files
  are abandoned). A one-off ops script can re-upload if needed — not a migration.

**Backend**

- `nadoumi-university` / `-scholarship` / `-program` / `-applicant`:
  - new upload endpoints (§I.5) returning `{mediaId, url?}`;
  - responses carry `logoUrl` / `coverUrl` / `heroUrl` / `gallery[].url` resolved via
    `mediaStorageService.publicUrl(mediaId)`, falling back to the deprecated
    `*_image_url` string when `*_media_id` is null;
  - `UniversityRequest` / `ScholarshipRequest` etc. accept `logoMediaId` etc.
    (a `bigint`) instead of a URL string; the `*_image_url` request fields are
    dropped.

**Frontend — `nadoumi-admin`**

- `src/components/ui/ImageUpload.vue` posts `multipart/form-data` to the module's
  new upload endpoint (e.g. `/staff/universities/{id}/logo`) with the auth header,
  and emits the returned `mediaId`; preview uses the returned `url`.
- `src/utils/asset.ts` (`assetUrl`) is reduced to a passthrough for absolute URLs
  (Cloudinary returns absolute `https://`), then deleted once no caller needs it.

**Frontend — `nadoumi-web`**

- `app/utils/media.ts` (`mediaUrl`) and `server/routes/media/[...path].ts` proxy are
  **removed** — catalog images are absolute Cloudinary URLs. `@nuxt/image` points at
  Cloudinary's domain (or uses the `cloudinary` provider) for responsive variants.
- `UniversityCard` / `ScholarshipCard` / `ProgramCard` / detail pages consume the
  absolute `url` fields directly.

## I.10 Configuration & secrets

| Env var | Purpose |
| --- | --- |
| `CLOUDINARY_URL` | `cloudinary://<key>:<secret>@<cloud>` — the only credential. **Required at startup**; the app fails fast (`MediaStorageService` bean init) if absent or malformed. |
| `NADOUMI_MEDIA_ENV` | `dev` \| `staging` \| `prod` — folder prefix, so one cloud can host all envs without collision. Default `dev`. |
| `NADOUMI_MEDIA_SIGNED_URL_TTL_SECONDS` | default `180`, clamped `[60, 600]`. |
| `NADOUMI_MEDIA_MAX_UPLOAD_MB` | hard ceiling across all categories, default `20`; also sets `spring.servlet.multipart.max-file-size`. |

- Secrets never logged. `CloudinaryMediaStorage` logs `public_id` + `bytes` + owner,
  never the URL for PROTECTED/SENSITIVE, never the API secret.
- No `application.yml` default for `CLOUDINARY_URL` — must be supplied per
  environment (local `.env`, CI secret, prod secret manager).
- **No `local` / filesystem `MediaStorageService` bean exists** (DM3). Tests use
  `FakeMediaStorage` (in-memory `Map<Long, byte[]>` + deterministic fake
  `public_id` / signed URL), wired via a test `@TestConfiguration`.

## I.11 Doc updates (Part I)

| Doc | Change |
| --- | --- |
| `ARCHITECTURE.md` | D5 row: mark **superseded by DM1** (Cloudinary). Add `nadoumi-media` to the module map + the "foundational modules" note (DM8). Add the access-flow diagram (§I.6). |
| `DEPLOYMENT.md` | Replace the "Object storage (S3-compatible) / MinIO / AWS S3" baseline with Cloudinary. `NAD_STORAGE_*` → `CLOUDINARY_URL` + `NADOUMI_MEDIA_*`. Note no local-FS production path. Data-retention line for `nad_media_access_log`. Remove the `local` impl from the storage SPI description. |
| `DATABASE_DESIGN.md` | New tables `nad_media_asset`, `nad_media_access_log`. `*_media_id` columns on `nad_university` / `_gallery` / `nad_scholarship` / `nad_program` / `nad_applicant`. Note the deprecated `*_image_url` fallback columns and their removal release. Update V21 row to "superseded by V27 media FKs". Migration ledger V26–V28. |
| `DOCUMENT_MANAGEMENT.md` | §1 EXISTING: note the interim `/profile` approach is removed. §3: `nad_document_version.storage_key` → `media_asset_id` FK to `nad_media_asset`; drop `storage_key`. Add the access-class model, the three delivery modes, the student/staff access matrix (§I.7), signed-URL TTL, SENSITIVE proxy list, `nad_media_access_log`, upload validation + limits table, replacement/versioning rules. Cloudinary named as the provider behind `MediaStorageService`. |
| `API_DESIGN.md` | New: `POST …/logo|hero|cover|photo|documents` upload contracts; `GET …/photo`, `GET …/documents/{id}/content` delivery contracts (302-signed vs proxy); `GET /api/media/{id}`. Error codes: `413` too large, `415`/`422` type, `403` + logged. |
| `SECURITY.md` | New "File & document storage" section: authorization-before-access rule, no permanent public URLs for private docs, signed-URL TTL, SENSITIVE proxy, every access logged, immediate revocation, MIME allow-list + magic-byte sniff + size caps table, filename sanitisation, `svg`/`html`/executable denylist, `CLOUDINARY_URL` handling, checksum. Cross-reference the scholarship-confidentiality rule (PUBLIC catalog images are fine; nothing else is). |

---

# PART II — Application Engine & Workflow (Step 6)

Unchanged from the design conversation except migration numbers (shifted after
V26–V28) and the document/payment guard substitution (DA3).

## II.1 Module & boundaries

`nadoumi-modules/nadoumi-application`. Depends on `nadoumi-identity` (access grants,
`NadoumiAccessService`), and calls `ApplicantService` / `ProgramService` /
`ScholarshipService` **through their interfaces** to validate
`program_id` / `scholarship_id` / `intake_id` at draft creation and to assemble the
submit-time snapshot. No cross-module mapper or entity access (the pattern
`nadoumi-program` already uses for `UniversityService`). DB FKs
`nad_application → nad_applicant / nad_program / nad_scholarship` are `ON DELETE
RESTRICT`.

Controllers stay thin (validate + DTO map + delegate). All engine logic in
`WorkflowService`; persistence in mappers. Dedicated request/response records per
audience (public / student / staff); no entity is serialised.

## II.2 Migrations

| Migration | Content |
| --- | --- |
| **V29 `nad_wf_definition.sql`** | `nad_wf_definition` (`id, code, name, version, status`), `nad_wf_stage` (`id, definition_id, code, name, order_no, stage_type, status_label, sla_hours?`), `nad_wf_transition` (`id, definition_id, code, from_stage_id?, to_stage_id, guard_json?, auto`), `nad_wf_stage_task_template` (`id, stage_id, title, role_required?, mandatory, blocks_exit, order_no`). Columns exactly per `APPLICATION_WORKFLOW.md §3.1`. |
| **V30 `nad_application.sql`** | `nad_application` (`id, applicant_id, application_type, program_id, scholarship_id?, intake_id?, workflow_instance_id?, current_stage_id?, current_status, assignee_user_id?, submitted_at?, version int NN default 0`, audit), `nad_wf_instance` (`id, definition_id, definition_version, application_id UNIQUE, current_stage_id?, status, started_at, closed_at?`), `nad_application_stage_history` (append-only: `from_stage_id?, to_stage_id, transition_code, changed_by, changed_at, reason?`), `nad_application_event` (append-only timeline: `event_type, actor_user_id?, at, detail_json?`), `nad_application_decision` (append-only: `decision_type, outcome, rationale, drives_transition_code?, decided_by, decided_at`), `nad_application_task` (single table, D12: `wf_stage_task_template_id?, title, role_required?, mandatory, blocks_exit, status, assignee_user_id?, due_at?, skip_reason?`, audit), `nad_application_snapshot` (`application_id, kind, payload_json LONGTEXT, created_at`). |
| **V31 `nad_application_menu_seed.sql`** | `Applications` C-menu + `nad:application:{view,list,create,edit,assign,claim,transition,withdraw,decide,note:view,note:internal:view,note:add,submission:record,export}` F-menus (from `PERMISSION_CATALOGUE.md §3`), granted to `ops_manager` / `case_officer` / `document_reviewer` / `nadoumi_super_admin` per §4. |
| **V32 `nad_wf_program_scholarship_v1_seed.sql`** | `PROGRAM_WITH_SCHOLARSHIP_V1` — the 13 stages / 15 transitions / 19 task templates from `WORKFLOW_PROGRAM_WITH_SCHOLARSHIP.md §3–§5`, with DA3 guard substitution: transition `package_ready` guard becomes `{"all":["ALL_MANDATORY_TASKS_DONE","DECISION_RECORDED:FEE_SETTLED:CONFIRMED"]}`; `docs_complete` becomes `{"all":["DECISION_RECORDED:DOCUMENTS_COMPLETE:CONFIRMED","ALL_MANDATORY_TASKS_DONE"]}`; `submit` drops `ALL_REQUIRED_DOCUMENTS_ATTACHED` (documents are collected in a later staff stage in this build), keeping `FIELD_SET(program_id) ∧ FIELD_SET(scholarship_id) ∧ FIELD_SET(intake_id)`. `status='ACTIVE'` after the validity checks pass. |
| **V33 `nad_wf_program_only_v1_seed.sql`** | `PROGRAM_ONLY_V1` — stages `DRAFT, SUBMITTED, ELIGIBILITY_REVIEW, DOCUMENT_COLLECTION, PACKAGE_PREPARATION, SUBMITTED_TO_UNIVERSITY, UNIVERSITY_DECISION, OFFER_RESPONSE, PRE_DEPARTURE, ENROLLED, UNSUCCESSFUL, WITHDRAWN`. Drops `SCHOLARSHIP_DECISION` and all `sch_*` transitions; `uni_offer` goes `UNIVERSITY_DECISION → OFFER_RESPONSE`. Scholarship-specific task templates and document requirements removed. `scholarship_id` must be `NULL` for this type (a CHECK / service guard). |

`nad_program.workflow_definition_code` `varchar(64) null` and
`nad_scholarship.workflow_definition_code` `varchar(64) null` are **added in V32**
(they do not exist yet). They route an application to a specific definition; `NULL`
→ the default definition for `application_type` (`PROGRAM_WITH_SCHOLARSHIP` →
`PROGRAM_WITH_SCHOLARSHIP_V1`, `PROGRAM_ONLY` → `PROGRAM_ONLY_V1`). The routing
resolves the **`ACTIVE`** version at `startDraft` time.

**FlywayMigrationsIT** counts move from 22 fresh / 21 baselined to **30 / 29**
(V26–V33); `AbstractNadIntegrationTest.baseSetup()` clears the new tables in FK order
(`nad_application_*` → `nad_wf_instance` → `nad_application` → `nad_wf_*` →
`nad_media_access_log` → `nad_media_asset`, then the existing order).

## II.3 `WorkflowService`

```java
Application startDraft(long applicantId, ApplicationType type,
                      long programId, Long scholarshipId, Long intakeId, Actor actor);

TransitionResult execute(long applicationId, String transitionCode,
                         Actor actor, String reason, int expectedVersion);

Decision recordDecision(long applicationId, String decisionType, String outcome,
                        String rationale, Actor actor);

void completeTask(long applicationId, long taskId, Actor actor);
void skipTask(long applicationId, long taskId, String reason, Actor actor);
void assign(long applicationId, long assigneeUserId, Actor actor);
void claim(long applicationId, Actor actor);

ValidationReport activate(long definitionId);   // §II.4 checks
```

- **`startDraft`** — resolve the `ACTIVE` definition (routing above); create
  `nad_application` (`current_status='DRAFT'`, `version=0`) + `nad_wf_instance`
  pinned to `definition.version`; position at the `START` stage; materialise
  START-stage task templates; `event: APPLICATION_CREATED`.
- **`execute`** — one DB transaction, optimistic-locked on `nad_application.version`
  (`UPDATE … SET version = version + 1 WHERE id = ? AND version = ?`; 0 rows → `409`):
  1. transition exists `from` the instance's `current_stage_id`;
  2. actor gate — staff: `nad:application:transition` + the transition's
     `role_required`; student: the named `ApplicantCapability` for that applicant;
  3. every current-stage task with `blocks_exit=1` is `DONE`/`SKIPPED` (always);
  4. `guard_json` predicates all true (§II.4);
  then: append `nad_application_stage_history`; set `current_stage_id` +
  `current_status` (from `to` stage's `status_label` — **engine is the only writer of
  `current_status`**); set `nad_wf_instance.current_stage_id`; close the instance if
  `to` stage is `TERMINAL`; materialise `to` stage task templates into
  `nad_application_task`; append `nad_application_event`; publish a Spring
  `ApplicationTransitioned` event (async notification fan-out is stubbed to a
  logging listener until Step 5).
  Auto-transitions (`auto=1`, guard already true, no blocking tasks) are chained;
  each writes its own history row.
- **Guard evaluator** — fixed dispatch, no expression language:
  - `ALL_MANDATORY_TASKS_DONE`, `TASKS_BLOCKING_EXIT_DONE` — over
    `nad_application_task` on the current stage.
  - `DECISION_RECORDED(type[,outcome∈{…}])` — over `nad_application_decision`.
  - `FIELD_SET(name)` — a whitelisted `nad_application` column is non-null
    (whitelist: `program_id, scholarship_id, intake_id, submitted_at`).
  - `ALL_REQUIRED_DOCUMENTS_ATTACHED`, `ALL_REQUIRED_DOCUMENTS_VERIFIED` →
    `DocumentGuardProvider` (no bean in this build).
  - `PAYMENT_SETTLED(kind)` → `PaymentGuardProvider` (no bean in this build).
  - Guard string grammar: `{"all":["PRED", "PRED:ARG", "PRED:ARG:OUTCOME", …]}` —
    AND only; OR is two transitions.

## II.4 Definition-validity checks (`activate`)

Per `APPLICATION_WORKFLOW.md §3.5`, plus DA3:

1. exactly one `START` stage;
2. ≥ 1 `TERMINAL` stage;
3. every non-terminal stage has ≥ 1 outgoing transition;
4. no unreachable stage (BFS from `START`);
5. every `DECISION` stage has ≥ 2 outgoing transitions distinguished by
   `DECISION_RECORDED(...)` guards;
6. **every predicate referenced by any `guard_json` has a registered provider** —
   `ALL_REQUIRED_DOCUMENTS_*` / `PAYMENT_SETTLED` with no bean → `activate` fails
   with `UNBACKED_GUARD_PREDICATE`. This is why the seed uses the
   `DECISION_RECORDED` substitution now.

`activate` flips `DRAFT → ACTIVE` and demotes any prior `ACTIVE` version of the same
`code` to `RETIRED`. Running instances keep their pinned `definition_version`.

## II.5 Snapshot (DA4)

On `submit`, in the transition transaction:

- `nad_application_snapshot(kind='PROFILE')` — JSON of the applicant aggregate via
  `ApplicantService`: identity + education + test scores + contacts, plus the chosen
  `program` / `scholarship` / `intake` display facts (name, university, degree,
  intake term). No PII redaction (internal record).
- `nad_application_snapshot(kind='REQUIREMENTS')` — the resolved document-requirement
  list for this application's programme + scholarship + workflow (from
  `WORKFLOW_PROGRAM_WITH_SCHOLARSHIP.md §6`), captured as the checklist definition
  that applied at submit time. (No `nad_document` rows yet — Step 7 fills the
  checklist against this snapshot.)

Snapshots are never updated or deleted. Later profile edits do not touch them.

## II.6 Staff API — `/api/staff/applications`

| Method / path | Perm (+ transition role gate) |
| --- | --- |
| `GET /api/staff/applications` (filter: type, status, stage, assignee, q; paged) | `nad:application:list` (+ scope filter for `case_officer` / `document_reviewer`) |
| `GET /api/staff/applications/{id}` (application + instance + current tasks + history + events + decisions + snapshot refs) | `nad:application:view` (+ scope) |
| `POST /api/staff/applications` (staff-initiated draft) | `nad:application:create` |
| `PUT /api/staff/applications/{id}` (opportunity / intake while `DRAFT`) | `nad:application:edit` |
| `POST /api/staff/applications/{id}/transitions/{code}` `{reason?, version}` | `nad:application:transition` + transition `role_required` |
| `POST /api/staff/applications/{id}/decisions` `{decisionType, outcome, rationale}` | `nad:application:decide` |
| `POST /api/staff/applications/{id}/tasks/{taskId}/complete` / `/skip {reason}` | `nad:application:transition` (task ownership or role) |
| `POST /api/staff/applications/{id}/assign` `{assigneeUserId}` / `/claim` | `nad:application:assign` / `:claim` |
| `GET /api/staff/workflow-definitions` / `/{code}` (read-only viewer) | `nad:application:view` |

All responses are dedicated records. `current_status` is read-only everywhere except
the engine.

## II.7 Public / student API

| Method / path | Auth |
| --- | --- |
| `POST /api/public/applications` `{applicantId, applicationType, programId, scholarshipId?, intakeId}` | authenticated; caller holds `CREATE_APPLICATION` for `applicantId` (own or grant) |
| `GET /api/student/applications` (the caller's applications across all applicants they can see) | `VIEW_APPLICATION` per applicant |
| `GET /api/student/applications/{id}` (stage, status, safe timeline) | `VIEW_APPLICATION` for that applicant |
| `PUT /api/student/applications/{id}` (opportunity / intake while `DRAFT`) | `CREATE_APPLICATION` |
| `POST /api/student/applications/{id}/submit` | `SUBMIT_APPLICATION`; runs `submit` transition + writes snapshots |
| `POST /api/student/applications/{id}/transitions/{accept_offer\|decline_offer\|withdraw}` `{reason?}` | the definition's student-actor capability; `withdraw` requires a reason |

The student timeline exposes only safe events + public-safe status labels; never
internal notes, decision rationales, assignee identities, or SLA data.

## II.8 Web — Apply Now + portal (`nadoumi-web`)

- **Entry points:** "Apply Now" CTA on `scholarships/[slug]` and `programs/[slug]`.
  - From a scholarship → the flow requires choosing a programme at that scholarship's
    university + an intake (`PROGRAM_WITH_SCHOLARSHIP`).
  - From a programme → optionally "add a scholarship" (→ `PROGRAM_WITH_SCHOLARSHIP`)
    or continue without one (→ `PROGRAM_ONLY`); choose an intake.
  - Unauthenticated → `/login?next=/apply?...`; after login, if the user holds grants
    for other applicants, an applicant switcher (the dashboard shell already has one).
- **Flow** `/apply?program=…&scholarship=…&intake=…` — steps: (1) confirm applicant +
  opportunity; (2) review prefilled profile (read-only summary pulled live, with
  "edit profile" deep-links into the dashboard); (3) confirm + submit → confirmation
  screen with the application reference and a link to the portal.
- **Portal** `/dashboard/applications` — list (status chip, opportunity, next action)
  and `/dashboard/applications/[id]` — stage timeline. Reuses the existing dashboard
  shell. i18n keys added to all four locales (`en`, `fr`, `ar`, `zh`) with exact
  parity (enforced by `i18n-keys.test.ts`).

## II.9 Admin — workbench (`nadoumi-admin`)

`/applications` — `DataTable` list (filters: type, status, stage, assignee;
`storage-key="applications"`), row → detail:

- stage timeline (`nad_application_stage_history` + `nad_application_event`);
- tasks panel — complete / skip (with reason) via the shared `Drawer`;
- decisions panel — record a decision (drives `DECISION`-stage fan-out);
- transition buttons that **show why a transition is blocked** (failing guard name /
  open blocking task);
- assign / claim;
- a read-only workflow-definition viewer (stages, transitions, guards).

Built from the shared `DataTable` / `Drawer` / `DescriptionList` / `StatusBadge` /
`FormSection` primitives. New `nav.ts` entry `{ path: '/applications', status:
'implemented' }`. i18n `en.ts` + `zh.ts`.

## II.10 Authorization model (II)

- **`User ≠ Applicant`.** Every applicant-scoped call checks
  `NadoumiAccessService` for the required `ApplicantCapability` — `CREATE_APPLICATION`
  to start/edit a draft, `SUBMIT_APPLICATION` to submit, `VIEW_APPLICATION` to read.
  Staff never hold these (they use `nad:application:*` perms + scope).
- **Application ownership** — a student actor may only see/act on applications whose
  `applicant_id` is one they hold a grant for. Cross-applicant → `403`.
- **Staff transitions** — `nad:application:transition` **and** the transition's
  `role_required`; a non-staff caller attempting a staff transition → `403`, not
  `500`.
- **Optimistic lock** — concurrent conflicting transition → `409`.
- **History integrity** — `*_stage_history` / `*_event` / `*_decision` are
  append-only; no update/delete paths exist in the mappers.

## II.11 Tests (per `testing.md` + `APPLICATION_WORKFLOW.md §6`)

Backend (JUnit 5 + Testcontainers):

- `WorkflowDefinitionValidationTest` — the six §II.4 checks, including
  "predicate with no provider → `activate` rejects".
- `WorkflowGuardTest` — `docs_complete` blocked without the `DOCUMENTS_COMPLETE`
  decision; `package_ready` blocked without `FEE_SETTLED`; `eligible` blocked without
  a `NADOUMI_INTERNAL` decision; wrong role → `403`; optimistic-lock conflict → `409`.
- `WorkflowDecisionFanoutTest` — `uni_reject` vs `uni_offer` chosen by recorded
  outcome; same for scholarship.
- `WorkflowWithdrawTest` — `withdraw` from every non-terminal stage lands in
  `WITHDRAWN`, writes exactly one history row, closes the instance.
- `WorkflowVersionPinningTest` — activating `…_V2` leaves a running `…_V1` instance's
  stages untouched.
- `ApplicationSnapshotTest` — `submit` writes `PROFILE` + `REQUIREMENTS`; a later
  profile edit does not alter the snapshot.
- `ApplicationAuthorizationTest` — a user without a grant cannot start / submit /
  read another applicant's application; on-behalf-of with `SUBMIT_APPLICATION`
  succeeds; staff scope filter (`case_officer` sees only assigned/in-queue).
- `ProgramOnlyWorkflowTest` — `PROGRAM_ONLY` routes to `PROGRAM_ONLY_V1`, has no
  `SCHOLARSHIP_DECISION` stage, rejects a non-null `scholarship_id`.
- `HistoryImmutabilityTest` — no mapper statement updates or deletes a
  `*_history` / `*_event` / `*_decision` row.

Media (Part I):

- `MediaValidationTest` — oversize → `413`; disallowed MIME → `415`; declared/sniffed
  mismatch → `422`; `svg` / `html` / executable rejected; filename sanitisation.
- `MediaAccessAuthorizationTest` — PUBLIC asset served to anonymous; PROTECTED photo:
  owner gets a signed URL, a different applicant gets `403` + a `DENIED` log row;
  SENSITIVE category is never returned as a URL (proxy only); a revoked grant loses
  access on the next call.
- `MediaAccessLogTest` — every PROTECTED/SENSITIVE access writes exactly one
  `nad_media_access_log` row with the right `result` / `deny_reason`.
- `CatalogImageRetrofitTest` — university logo upload → `nad_media_asset` PUBLIC row
  + `logo_media_id` set + response `logoUrl` is the Cloudinary `secure_url`;
  `v_scholarship_student` still leaks no confidential field.
- `FakeMediaStorage` is the test double; no test hits Cloudinary or a filesystem.

Frontend:

- `nadoumi-web` Vitest — Apply Now flow (steps, prefilled profile, submit calls the
  right endpoint), portal list/detail render without leaking staff fields, locale
  parity.
- `nadoumi-admin` Vitest — workbench list, transition-blocked reason display, task
  complete/skip drawer, definition viewer.

## II.12 Doc updates (Part II)

`APPLICATION_WORKFLOW.md` (EXISTING → what's built; DA3 substitution noted),
`WORKFLOW_PROGRAM_WITH_SCHOLARSHIP.md` (seed applied; guard substitution; add
`PROGRAM_ONLY_V1` sibling section), `DATABASE_DESIGN.md` (V29–V33 tables + ledger),
`API_DESIGN.md` (staff / student / public application surfaces),
`PLATFORM_ARCHITECTURE.md §8` (Step 6 rows → progressive ✅),
`FRONTEND_ARCHITECTURE.md` + `ADMIN_ARCHITECTURE.md` (Apply Now, portal, workbench),
`PERMISSION_CATALOGUE.md` (confirm granted `nad:application:*` set),
`DOMAIN_MODEL.md` (Application aggregate → EXISTING).

---

## 3. Implementation phasing

Each phase is a separate implementation plan (`writing-plans`) and a separate PR
(or small PR series). A phase ends green: `mvn -o -pl ruoyi-admin verify` +
`nadoumi-admin` lint/build/test + `nadoumi-web` typecheck/test, with the phase's docs
updated in the same PR.

| Phase | Deliverable | Migrations |
| --- | --- | --- |
| **P1 — Media layer + catalog retrofit** | `nadoumi-common` SPI; `nadoumi-media` module + `CloudinaryMediaStorage`; `nad_media_asset` + `nad_media_access_log`; `MediaValidation` + `MediaCategoryPolicy`; upload + delivery endpoints for university / scholarship / program images (PUBLIC) and applicant photo (PROTECTED, signed URL); admin `ImageUpload` + web rendering switched to Cloudinary; the six doc updates in §I.11; `FakeMediaStorage`; full media test set. | V26, V27, V28 |
| **P2 — Application engine (backend)** | `nadoumi-application` module; `nad_wf_*` + `nad_application*` tables; `WorkflowService` (transition, guards, auto-chain, optimistic lock, `activate` checks); provider interfaces `DocumentGuardProvider` / `PaymentGuardProvider` (unimplemented); seed `PROGRAM_WITH_SCHOLARSHIP_V1` + `PROGRAM_ONLY_V1`; staff API; menu + perms seed; full engine test set. No UI. | V29, V30, V31, V32, V33 |
| **P3 — Public Apply Now + student portal** | Public/student API (`startDraft`, draft edit, `submit` + snapshot, student transitions, list/detail); `nadoumi-web` Apply Now flow + `/dashboard/applications`; i18n ×4; web tests. | — |
| **P4 — Admin workbench** | `nadoumi-admin` `/applications` list + detail (timeline, tasks, decisions, guarded transitions, assign/claim, definition viewer); nav + i18n; admin tests. | — |

Later (not this spec): Step 5 notification fan-out replaces the logging listener;
Step 7 `nadoumi-document` implements `DocumentGuardProvider` + the applicant document
upload UI against §I.7 and ships `PROGRAM_WITH_SCHOLARSHIP_V2` with the real document
guards; Step 9 `nadoumi-payment` implements `PaymentGuardProvider`.

## 4. Migration numbering

Applied so far: V1–V11, V14, V16–V25. Retired gaps: V12, V13, V15.
This spec adds **V26–V33**. `FlywayMigrationsIT`: **30 fresh / 29 baselined** after
P2. Each migration is append-only and immutable once merged; a fix is a new `V*`.
DDL and seed are separate migrations (V29 vs V32/V33; V26 vs V27 retrofit).

## 5. Risks & mitigations

| Risk | Mitigation |
| --- | --- |
| Cloudinary outage → uploads + protected downloads fail | PUBLIC catalog images keep serving from Cloudinary's CDN (read path resilient). Upload failures return `503` with a retryable error; no partial `nad_media_asset` row (persist only after a confirmed Cloudinary response). Reconciliation job repairs orphans. |
| Signed URL leaked within its TTL | TTL ≤ 600 s (default 180 s); URL is single-asset; SENSITIVE media never gets a URL at all; every issue is logged with actor + IP. |
| App-server bandwidth for SENSITIVE proxy | Bounded to the SENSITIVE doc-type list (passport/visa/financial/transcript/JW202); streamed with a bounded buffer; monitored. Revisit signed-direct if volume demands. |
| Large `payload_json` snapshots | `LONGTEXT`; snapshot is written once, read rarely; not indexed. |
| `activate` rejects the seeded definition because a guard is unbacked | Intentional (DA3) — the seed uses `DECISION_RECORDED` substitutions; `ALL_REQUIRED_DOCUMENTS_*` / `PAYMENT_SETTLED` appear in no seeded `guard_json` until Step 7/9. |
| `v_scholarship_student` recreated again (V27) | Same disciplined pattern as V21–V25; a `CatalogImageRetrofitTest` re-asserts no confidential column is exposed. |
| Two image mechanisms during the deprecation window | `*_image_url` columns kept read-only as fallback for exactly one release; removal migration tracked. |

## 6. Open items for reviewer

1. **`nadoumi-media` module name** — `nadoumi-media` vs folding the impl into a
   future `nadoumi-document`. Recommendation: `nadoumi-media` (public catalog images
   must not depend on a document module). Confirm.
2. **`/api/media/{id}` indirection for PUBLIC images** — ship it as the canonical
   link (swap-friendly) but have DTOs carry the direct `secure_url` for SSR/SEO?
   Recommendation: yes to both. Confirm.
3. **Deprecated `*_image_url` columns** — drop in the very next migration after P1
   ships, or keep for a full release cycle? Recommendation: one release.
4. **`PROGRAM_ONLY_V1` intake requirement** — is `intake_id` mandatory for
   programme-only applications (recommended: yes, same as combined) or optional?
5. **Applicant photo access class** — PROTECTED (signed URL) as specified, or
   SENSITIVE (proxy only)? Recommendation: PROTECTED (it's a headshot, not a
   document).

---

*End of specification. On approval: `writing-plans` for P1 first.*
