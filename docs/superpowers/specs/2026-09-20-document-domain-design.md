# Document domain — implementation-readiness addendum

Status: **ADDENDUM** to an already-approved baseline. This is not a from-scratch
design — `docs/DOCUMENT_MANAGEMENT.md` (reconciled with Rev 3 + P1) already
contains the model, storage decision, access-control matrix, upload hardening
pipeline, versioning/retention rules and a testing outline. This doc verifies
that baseline is still sound, closes the one real gap found (a stale DDL
column), assigns concrete migration numbers, and specifies the one thing that
doesn't exist anywhere yet: the module structure and exact API surface.

Read first: `docs/DOCUMENT_MANAGEMENT.md` (the model), `docs/ddl/nad_core.draft.sql`
§7 (draft DDL, STUB), `docs/DATABASE_DESIGN.md` §5.6/§5.6a (reconciled column
list), `docs/PLATFORM_ARCHITECTURE.md` §8 Step 7 row.

---

## 1. What already exists (EXISTING — verified this session)

- **Storage layer is fully built and tested (P1, `V26`–`V29`).** `nadoumi-media`
  module: `MediaStorageService` SPI, `CloudinaryMediaStorage` +
  `LocalFilesystemMediaStorage` impls, `MediaService` implementing the
  `MediaGateway` façade (`nadoumi-common`), `nad_media_asset` +
  `nad_media_access_log` tables, `MediaValidation` (size/filename/magic-byte
  sniff/denylist), `MediaCategoryPolicy`, `MediaReconciliationJob`. 15 test
  classes exist (`MediaServiceTest`, `MediaStorageContractTest`,
  `CloudinaryMediaStorageTest`, etc.) — this is not paper design, it runs.
- **All the `MediaCategory` values Document needs already exist and are
  policy-configured**, reserved and unused until now: `APPLICANT_DOCUMENT`,
  `APPLICATION_DOCUMENT`, `ADMISSION_DOCUMENT`, `JW202`, `OTHER_ATTACHMENT`
  (`MediaCategoryPolicy.java:81-89`) — access class, MIME allow-list, max size
  are already decided (`DOCUMENT_MANAGEMENT.md` §3.4 table). Nothing to design
  here, just consume.
- **The Document data model is already drafted**, `nad_core.draft.sql:456-527`:
  `nad_document`, `nad_document_version`, `nad_document_requirement`,
  `nad_document_event`, plus `nad_application_document` (M:N, §8.9,
  `nad_core.draft.sql:734-748`). `DOMAIN_MODEL.md` §Document (G-D1..G-D4) has
  already resolved the three design questions that usually bite here: no
  `documentIds[]` array on Application; `current_version_id` is the
  authoritative version and `status` is derived from it; `rejection_reason` is
  `SHARED`-visible while review notes stay internal.
- **The reconciliation between the draft DDL and the storage decision is
  already written down** — `DATABASE_DESIGN.md:183` already shows
  `nad_document_version.media_asset_id → nad_media_asset` superseding the
  originally-drafted `storage_key`. The raw `.sql` file just hasn't been
  updated to match yet (see §2).

## 2. Gap found: the draft DDL is stale, one concrete fix

`nad_core.draft.sql:474-489` (`nad_document_version`) still has the **pre-P1**
shape:

```sql
storage_key          varchar(255) NOT NULL COMMENT 'object-storage key; bytes NOT in MySQL (D5)',
content_type          varchar(120) NOT NULL,
size_bytes            bigint       NOT NULL,
checksum_sha256       char(64)         NULL,
```

`DATABASE_DESIGN.md:183` (written after P1 landed) has already resolved this:
`storage_key` is replaced by `media_asset_id bigint NOT NULL REFERENCES
nad_media_asset(id)`; `content_type`/`size_bytes`/`checksum_sha256` are **kept**
as a denormalized, immutable-at-upload copy (avoids a join for the common
"list my documents" read; `nad_media_asset` remains authoritative for the
actual bytes/access-control path). **PROPOSED**: when Step 7 migrations are
written, use the `DATABASE_DESIGN.md:183` shape, not the raw draft file's. No
other table in the draft needs a structural change — `nad_document`,
`nad_document_requirement`, `nad_document_event`, `nad_application_document`
are all internally consistent with the reconciled docs.

## 3. Storage approach — PROPOSED (reaffirms the existing decision)

**Recommendation: consume `MediaGateway` exactly as every other module does.
Do not build a parallel storage path for documents.** This was actually
already decided (`DATABASE_DESIGN.md:183`, `PLATFORM_ARCHITECTURE.md` Step 7
row: *"consumes nadoumi-media"*) — restating it here only because the parent
brainstorming session considered it an open question. Two alternatives were
implicitly available and are both wrong for this codebase:

| Approach | Why not |
| --- | --- |
| New document-specific storage abstraction | Duplicates `MediaStorageService`/`MediaGateway`, which already do exactly this (validate → store → asset row → access log) and are tested. Pure YAGNI violation. |
| Store `content_type`/`size_bytes` only on `nad_media_asset`, nothing on `nad_document_version` | Forces a join for every document list read; `nad_media_asset` is polymorphic (`owner_kind`/`owner_id`) and not indexed for "all versions of this document" queries the way `nad_document_version.document_id` is. |
| **Denormalized copy on `nad_document_version`, `media_asset_id` as the authority (recommended)** | One read for the common case, `nad_media_asset` still the single source of truth for bytes/access/audit. This is what `DATABASE_DESIGN.md` already specifies. |

`nad_document_version.uploaded_by`/`verified_by` are `sys_user.user_id`
references, same as `nad_media_asset.uploaded_by` — no duplication of identity,
just of the three scalar fields above.

## 4. Finalized migration plan — PROPOSED (V71–V78 assigned this session)

(Application-domain design, run in parallel this session, uses V63–V68 — no
overlap. `PLATFORM_ARCHITECTURE.md` Step 7 row says "next free (≥ V30)"; V71 is
next-free as of this session, V63–V70 reserved for Application.)

| # | File | Content |
| --- | --- | --- |
| V71 | `V71__nad_document.sql` | `nad_document` table, per §1/§2 above |
| V72 | `V72__nad_document_version.sql` | `nad_document_version` with `media_asset_id` FK (§2 shape), `fk_document_current_version` added after |
| V73 | `V73__nad_document_requirement.sql` | `nad_document_requirement` |
| V74 | `V74__nad_document_event.sql` | `nad_document_event` |
| V75 | `V75__nad_application_document.sql` | `nad_application_document` — **conditional**: only if the Application-domain migrations (V63–V68) have landed first, since it FKs `nad_application`. If Document ships before Application, split this into its own later migration instead of blocking Document on Application's timeline (see DECISION REQUIRED #1). |
| V76 | `V76__nad_document_type_dict_seed.sql` | Seed `sys_dict_type`/`sys_dict_data` for `nad_document_type` — **not seeded anywhere today**, confirmed by grep. Minimum v1 set: `PASSPORT`, `VISA`, `FINANCIAL_PROOF`, `TRANSCRIPT`, `POLICE_CLEARANCE`, `DEGREE_CERTIFICATE`, `RECOMMENDATION_LETTER`, `PERSONAL_STATEMENT`, `LANGUAGE_TEST_REPORT`, `OTHER` — the five SENSITIVE-override types already named in `DOCUMENT_MANAGEMENT.md` §3.2a plus the obvious application-document set. Seed data, separate from DDL per `.claude/rules/database.md`. |
| V77 | `V77__nad_media_access_log_document_fk.sql` | `nad_media_access_log.document_id` is listed as a column in `DATABASE_DESIGN.md:199` ("set once Step 7 exists") but the `V26` migration that created the table couldn't reference `nad_document` (didn't exist yet) — this migration adds the nullable FK column now that it can. |
| V78 | *(reserved, unused — buffer)* | — |

## 5. Module structure — PROPOSED

`nadoumi-modules/nadoumi-document/`, mirroring `nadoumi-applicant`'s layout
exactly (`domain/`, `mapper/`, `service/`, `web/`):

- **Entities**: `Document`, `DocumentVersion`, `DocumentRequirement`,
  `DocumentEvent`, `ApplicationDocument`.
- **Mappers**: one per entity, MyBatis XML alongside, parameterized queries
  only (`.claude/rules/database.md`).
- **`DocumentService`** (implementation) exposing a **`DocumentGateway`**
  interface in `nadoumi-common` — the same cross-module pattern as
  `MediaGateway` — so the not-yet-built `nadoumi-application` module (and the
  existing `nadoumi-applicant` module, for the reconciliation in §7) can
  depend on `nadoumi-common` only, never reach into `nadoumi-document`'s
  mappers directly (`.claude/rules/architecture.md`).
- **`DocumentRequirementService`**: resolves the checklist for a given scope
  (`PROGRAM`/`SCHOLARSHIP`/`WF_STAGE`) — read-only, consumed by the workflow
  engine's `ALL_REQUIRED_DOCUMENTS_VERIFIED` guard (Application domain design,
  not built by this module).
- **DTOs, split by audience** (`.claude/rules/architecture.md` — never a
  persistence entity over the wire):
  - `StudentDocumentDto` (own documents: id, docType, status, expiresOn,
    currentVersion summary, rejectionReason if REJECTED) — no
    `reviewer_user_id`, no internal event detail.
  - `StaffDocumentDto` (adds reviewer, full version history, event log,
    requirement linkage) — gated by `nad:document:verify` scope.

## 6. API surface — PROPOSED

Follows the exact convention already used by `StudentApplicantController`
(`/api/student/applicants/{id}/photo` etc.) and the staff equivalent:

**Student** (`/api/student/documents`, ownership = own applicant via
`NadoumiAccessService` grant — never trust a path id alone):

| Method | Path | Notes |
| --- | --- | --- |
| GET | `/api/student/documents?applicantId=` | list own documents, optionally `?applicationId=` to scope to one application's checklist |
| POST | `/api/student/documents` | create + upload first version (`applicantId`, `docType`, multipart) |
| POST | `/api/student/documents/{id}/versions` | upload a new version (replace) — moves status back to `IN_REVIEW` |
| DELETE | `/api/student/documents/{id}` | only while `status='DRAFT'` — no version uploaded yet; a submitted document is never hard-deleted (`.claude/rules/database.md` deletion-behavior requirement) |
| GET | `/api/student/documents/{id}/versions/{v}/content` | signed URL (PROTECTED) or streamed proxy (SENSITIVE), per `DOCUMENT_MANAGEMENT.md` §3.2a — blocked unless `scan_status='CLEAN'` |
| GET | `/api/student/applications/{id}/document-checklist` | resolved requirement list + attachment status, for the My Applications UI (consumer, not owner, of this endpoint's data — actual route may live in `nadoumi-application` once built; documented here so the shape is agreed now) |

**Staff** (`/api/staff/documents`, scope per `PERMISSION_CATALOGUE.md` §4 —
`case_officer`/`document_reviewer`: assignee or department queue;
`ops_manager`/`nadoumi_super_admin`: all):

| Method | Path | Notes |
| --- | --- | --- |
| GET | `/api/staff/documents?applicationId=` or `?applicantId=` | `nad:application:view` + in scope |
| GET | `/api/staff/documents/{id}` | full `StaffDocumentDto` |
| GET | `/api/staff/documents/{id}/versions/{v}/content` | `nad:document:verify`? — actually just viewing requires `VIEW_APPLICATION`+`VIEW_DOCUMENT`-equivalent staff scope; every access logged (§3.3) |
| POST | `/api/staff/documents/{id}/verify` | `nad:document:verify` — sets `VERIFIED`, `verified_by`/`verified_at` |
| POST | `/api/staff/documents/{id}/reject` | `nad:document:verify`, body `{reason}` — 422 if reason blank (`DOCUMENT_MANAGEMENT.md` §4 testing note) |
| POST | `/api/staff/document-requirements/waive` | `nad:document:verify`, waives a `waivable` checklist item with a recorded reason |

## 7. Photo/Passport reconciliation — DECISION REQUIRED

**The gap the parent investigation flagged.** Today `nad_applicant` has
`photo_media_id`/`passport_media_id` — single slots, no version history, no
reviewer, no rejection reason (`ApplicantMediaKind.java`). This is a **simpler,
different mechanism** than `nad_document`: the existing passport flow verifies
by **structural/automated comparison** against typed profile fields
(`matchesProfile`, `mismatches` — see `PassportUploadCard.vue`), not human
staff review. These solve different problems and can legitimately stay
separate forever.

But `DOMAIN_MODEL.md` G-D1 already states the intended design: *"Reusable
applicant-level docs (passport) are attached to applications only via
`nad_application_document`"* — which requires a `nad_document` row to exist
for the passport. Three ways to get there:

1. **Big-bang migrate**: on Step 7 ship, backfill every `photo_media_id`/
   `passport_media_id` into a new `nad_document`+`nad_document_version` row,
   deprecate the slot columns. Highest risk (touches every existing
   applicant), highest consistency.
2. **Permanent coexistence**: slots stay for onboarding's automated identity
   check; a student re-uploads passport separately into the Document system
   when an application actually needs it. Zero migration risk, worse UX
   (upload twice), and violates G-D1's stated intent.
3. **Lazy backfill (recommended)**: slots stay exactly as they are for
   onboarding's automated check (unrelated concern, don't touch). The first
   time a passport is needed for `nad_application_document` (student clicks
   "attach passport" on an application, or the workflow engine's document
   checklist runs), `DocumentService` transparently creates a `nad_document`
   row from the existing `passport_media_id`'s `nad_media_asset` — reusing the
   asset, not re-uploading bytes — and attaches it. No student-visible
   migration step, no big-bang backfill job, and G-D1's model is satisfied the
   moment it's actually used.

**Recommend option 3.** Needs explicit user sign-off before the Application
domain's document-checklist guard is implemented, since that guard is what
would trigger the lazy creation.

## 8. Testing plan — PROPOSED, per `.claude/rules/testing.md`

- **Service**: new version keeps prior versions immutable; `current_version_id`
  repoints on new version; status legal transitions only
  (`DRAFT→SUBMITTED→IN_REVIEW→VERIFIED|REJECTED|EXPIRED`); `REJECTED` without
  `rejection_reason` → 422; `waivable` skip records a reason.
- **Authorization (required)**: cross-applicant fetch → 403; student cannot
  read `StaffDocumentDto` fields (reviewer id, internal event detail) even via
  a 200 response shape check; anonymous content-URL request → 401; staff
  outside assignee/department scope → 403.
- **Upload**: delegates to `MediaValidation` — covered by existing
  `MediaValidationTest`; Document-specific test is just "wrong category
  rejected" / "PENDING scan blocks `.../content`".
- **Requirements**: checklist built from program + scholarship + stage
  requirement rows; `ALL_REQUIRED_DOCUMENTS_VERIFIED` guard reflects reality
  (integration test once the workflow engine exists — can stub the guard
  interface until then).
- **Integration**: `FlywayMigrationsIT` extended to cover V71–V77.

## 9. Migration risks / affected modules

- **Affected**: new `nadoumi-document` module; `nadoumi-common` (new
  `DocumentGateway` interface, mirrors `MediaGateway`); `nadoumi-applicant`
  (only if/when §7 option 3 is implemented — reads `passport_media_id` to
  seed a lazy `nad_document`, no schema change to `nad_applicant` itself);
  `nad_media_access_log` gets one nullable column (V77).
- **Risk**: none of V71–V77 touch an existing table's data, only add new
  tables + one nullable column — low risk, no backfill required at ship time
  under the recommended §7 option 3.
- **Sequencing risk**: V75 (`nad_application_document`) FKs `nad_application`,
  which doesn't exist until the Application-domain migrations land. If the two
  domains ship in different order, split V75 out and apply it whenever both
  tables exist — flagged as DECISION REQUIRED #1.

## 10. Open decisions for the user

1. **Ship order**: does Document (V71–V77) ship before, after, or interleaved
   with Application (V63–V68)? Determines whether V75 needs to be held back.
2. **§7 Photo/Passport reconciliation** — confirm option 3 (lazy backfill) as
   recommended, or pick 1/2.
3. **`nad_document_type` seed set (V76)** — confirm the v1 list in §4 is
   complete/correct before it's seeded (dictionary values are cheap to add
   later, but the five SENSITIVE-override types must match
   `DOCUMENT_MANAGEMENT.md` §3.2a exactly since access-class resolution keys
   off them).
