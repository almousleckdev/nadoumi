# Nadoumi — Applicant Onboarding

> Read `docs/PLATFORM_ARCHITECTURE.md` first — the approved enterprise-platform direction this doc rolls up to (2026-09-02).

Date: 2026-09-03 (Revision 3)
Depends on: Document slice + object storage (`docs/DOCUMENT_MANAGEMENT.md`),
University/Program/Scholarship catalog (`docs/DOMAIN_MODEL.md` §2.1).
Related: `docs/superpowers/specs/2026-09-02-nadoumi-web-public-site-design.md`
(§16 / Revision 3 D-R3-3), `docs/DOMAIN_MODEL.md` §4, `docs/DATABASE_DESIGN.md` §5.2.

## Status legend

| Tag | Meaning |
| --- | --- |
| **EXISTING** | Built and wired to a real backend endpoint today. |
| **PLANNED** | Designed here; frontend may render it disabled/read-only with a "coming soon — not saved yet" note. Never faked as persisted. |
| **REQUIRES BACKEND** | Needs a schema + endpoint that does not exist yet (this doc specifies it). |

**Per-step backend gap** (Contacts endpoint to wire, Interests / Location / Photo
/ Passport / onboarding-complete flag to build): see
`ARCHITECTURE_GAP_ANALYSIS.md` §7 — kept in sync with this doc as each step lands.

**Revision 3 build (`/dashboard/onboarding`):** a 7-step wizard shell —
**Personal · Identity · Education · Interests · Location · Contact · Review** — with
a persistent progress indicator. Steps backed by EXISTING endpoints save for real;
PLANNED steps are visible but explicitly not saved. The `ProfilePhotoUploadCard` /
`PassportUploadCard` / `DocumentPreview` / `ImageCropper` components are built
**client-only** (crop/zoom/rotate/preview/validate); their upload action is disabled
(REQUIRES BACKEND — Document slice). No OCR / face-match / passport-authenticity
claims.

---

## 1. Goals & non-goals

**Goals**

- A real, guided education-profile onboarding after first login — not a flat form.
- Collect exactly what an international application needs: identity, passport,
  education history, study interests, current location, languages, tests, work
  history, references.
- Every field explicitly classified **Required / Recommended / Optional**.
- Save-and-resume; a visible progress indicator; edit any section later.
- Clean, centered, responsive, accessible; the same design system as the rest of
  `nadoumi-web` (Tailwind + Nadoumi tokens + shared `ui/` components).

**Non-goals / not claimed**

- **No OCR, no passport authenticity verification, no face match, no liveness.**
  Uploads are stored and shown; a human reviews them later (Document slice).
- No third-party identity provider.
- No payment collection in onboarding.

---

## 2. UX flow (Revision 3 — 7 steps)

After registration the student lands on `/dashboard/onboarding`. A persistent
progress indicator shows the seven steps and % complete; content is centered,
generously spaced, responsive, with reduced-motion-aware step transitions and
loading/error/success states. Every field carries a **Required / Recommended /
Optional** badge.

| # | Step | Contents | Persistence |
| --- | --- | --- | --- |
| 1 | **Personal** | First name, last name (from registration; editable) | **EXISTING** — applicant `nickName` / given+family name |
| 2 | **Identity** | Given name, family name, DOB, nationality, passport number, phone | **EXISTING** — `PUT /api/student/applicants/{id}` (current `/dashboard/profile` fields). *Gender, country of residence, passport expiry, preferred comms language* → **PLANNED / REQUIRES BACKEND** (§5); shown disabled with the coming-soon note. |
| 3 | **Education** | Highest level, institution, field, GPA + scale, start/end dates (repeatable) | **EXISTING** — `/api/student/applicants/{id}/education`. *Other qualifications* → PLANNED. |
| 4 | **Interests** | Desired degree, fields, majors, preferred countries/universities, scholarship interest, preferred intake | **PLANNED — REQUIRES BACKEND** (§5 `nad_applicant_interest*`). Rendered read-only + coming-soon. |
| 5 | **Location** | *"Are you currently in China?"* Yes/No branch — Chinese city/address/WeChat/school/visa **or** country/city/WhatsApp/preferred contact; phone | **PLANNED — REQUIRES BACKEND** (§5 `nad_applicant_residence`). |
| 6 | **Contact** | Guardian / emergency contact (relation, name, email, phone) | **EXISTING** — `/api/student/applicants/{id}/contacts`. |
| 7 | **Review** | Summary of everything captured; **Finish** marks onboarding complete (client-side flag) → `/dashboard` | **EXISTING** (client-side completion flag; a server `onboarded_at` is PLANNED, §5). |

**Profile photo & Passport** (their own sub-step under Identity, or a step 2b):
`ProfilePhotoUploadCard` / `PassportUploadCard` — **PLANNED — REQUIRES BACKEND**
(Document slice + object storage). Built client-only per §3.2/§3.3; the upload
button is disabled with "Available once document storage is enabled".

**Persistence rule (hard):** a PLANNED field is never sent to the server and never
shows a fake success. It is visibly marked *not saved yet*.

### 2.0 Historical 11-step design (superseded by the 7-step flow above)

The rest of this document keeps the fuller field catalogue, domain model, schema
and API design. The 7 Revision-3 steps group those fields; nothing below is
removed, only re-sequenced.

| # | Step | Route | Gate to advance |
| --- | --- | --- | --- |
| 1 | **Identity** | `/identity` | all Required identity fields |
| 2 | **Profile photo** | `/photo` | Recommended (skippable with a confirm) |
| 3 | **Passport** | `/passport` | Recommended (skippable with a confirm) |
| 4 | **Education & history** | `/education` | ≥1 education entry + highest level |
| 5 | **Education interests** | `/interests` | desired degree + ≥1 field + ≥1 country |
| 6 | **Current location** | `/location` | the *"Are you currently in China?"* branch answered + that branch's Required fields |
| 7 | **Languages & proficiency** | `/languages` | English proficiency selected |
| 8 | **Tests** | `/tests` | Optional (reuses the existing test-scores model) |
| 9 | **Work experience** | `/work` | Optional |
| 10 | **Certifications** | `/certifications` | Optional |
| 11 | **Guardian / emergency contact** | `/contact` | ≥1 contact (reuses `nad_applicant_contact`) |
| — | **Review & finish** | `/review` | user confirms; profile marked onboarded |

Rules:

- A step with only Optional/Recommended content shows **Skip for now**; skipping
  records the skip so the review screen can nudge.
- The wizard writes through the same section endpoints used by the standalone
  `/dashboard/*` editors — onboarding is a guided path over the same data, not a
  parallel store.
- Completion sets `nad_applicant_profile.onboarded_at`; the dashboard then shows the
  standard section editors and an "Onboarding complete" state.
- All copy from i18n keys (`onboarding.*`); RTL-correct.

### 2.1 Current-location branch (step 6)

Question: **"Are you currently in China?"** (Yes / No).

**If Yes** — fields: Chinese city (Required); current address (Required); phone
(Required); WeChat ID (Recommended); current school / university (Recommended);
current student status (Recommended: enrolled / graduated / gap / language-study /
other); residence/visa info — permit type + expiry (Recommended).

**If No** — fields: country (Required); city (Required); phone (Required); WhatsApp
(Recommended); preferred contact method (Required: email / WhatsApp / phone).

Switching the answer keeps previously entered values for the other branch (soft
retain) but only the active branch's fields validate.

---

## 3. Field catalogue

Legend — **R** Required · **Rec** Recommended · **O** Optional · **PII** personal
data (`docs/SECURITY.md` §6 masking + audit rules apply).

### 3.1 Identity (step 1) — extends `nad_applicant`

| Field | Class | Type | Validation | PII | Notes |
| --- | --- | --- | --- | --- | --- |
| First name | R | string(100) | non-blank | PII | "as on passport" helper |
| Last name | R | string(100) | non-blank | PII | "as on passport" helper |
| Date of birth | R | date | past; age ≥ 15 warn | PII | |
| Gender | R | enum | `MALE\|FEMALE\|UNSPECIFIED` | PII | **new column** |
| Nationality | R | ISO-3166-1 alpha-2 | valid code | PII | |
| Country of residence | R | ISO-3166-1 alpha-2 | valid code | PII | **new column**; pre-fills step 6 |
| Passport number | Rec | string(64) | — | PII | soft-dupe warn (existing rule) |
| Passport expiry | Rec | date | future warn | PII | **new column** |
| Phone | R | string(32) | E.164-ish | PII | |
| Preferred communication language | Rec | enum | `en\|zh\|fr\|ar` | | **new column** |

### 3.2 Profile photo (step 2) — Document slice

| Field | Class | Notes |
| --- | --- | --- |
| Photo | Rec | JPEG/PNG/WebP, ≤ 5 MB, min 400×400. Client: crop (1:1), zoom, rotate, preview, replace, remove. Stored as a `nad_document` of type `PROFILE_PHOTO`. Quality guidance shown (face centered, plain background, good light) — **guidance only, not enforced by CV**. |

### 3.3 Passport (step 3) — Document slice, reusable `PassportUploadCard`

| Field | Class | Notes |
| --- | --- | --- |
| Passport image | Rec | JPEG/PNG/PDF, ≤ 10 MB. Client: large readable preview, zoom, rotate, crop-to-page, replace, remove, upload-progress, MIME/size validation. The user must confirm "the details are readable" before continuing. Stored as `nad_document` type `PASSPORT`. **No OCR / authenticity check** — copy must not imply one. |

`PassportUploadCard` is a shared component (also used later by staff review and by
the application document checklist).

### 3.3b Reusable document/image components (`app/components/onboarding/`)

Built in Revision 3 as **client-only** (no upload endpoint yet — REQUIRES BACKEND,
Document slice). Presentational + local image manipulation only.

| Component | Responsibility |
| --- | --- |
| `ImageCropper` | `<canvas>`-based crop with zoom + rotate; emits a cropped `Blob`/`dataURL`. Aspect-ratio prop (`1` for photo, free for passport). Keyboard-nudgeable handles; `prefers-reduced-motion`. |
| `DocumentPreview` | Large read-optimised viewer: pan, zoom (buttons + wheel/pinch), rotate 90°, fit/fill. Used to inspect a passport before continuing. |
| `ProfilePhotoUploadCard` | Drop/select → validate (JPEG/PNG/WebP ≤ 5 MB, min 400×400) → `ImageCropper` (1:1) → preview; replace / remove; quality guidance (face centred, plain background, even light — guidance only). **Upload button disabled** with "Available once document storage is enabled". |
| `PassportUploadCard` | Drop/select → validate (JPEG/PNG/PDF ≤ 10 MB) → `DocumentPreview` (large, zoom, rotate) + optional crop-to-page; "confirm the details are readable" checkbox; replace / remove; accepted-format/size guidance; upload-progress affordance (inert until backend). **Upload disabled**, same note. |

All four are reused by staff document review and the application checklist later.
None claims OCR, face match, or passport authenticity.

### 3.4 Education & history (step 4)

| Field | Class | Type | Notes |
| --- | --- | --- | --- |
| Highest education level | R | dict `nad_degree_level` | **new column on `nad_applicant_profile`** |
| Latest degree / diploma title | Rec | string(200) | |
| Education entries (repeatable) | R (≥1) | — | reuses `nad_applicant_education` (institution R, level, field, GPA, GPA scale, start, end, graduation year) |
| Other qualifications | O | free text / repeatable | short list of extra credentials |

### 3.5 Education interests (step 5) — new `nad_applicant_interest` (single row) + link tables

| Field | Class | Type |
| --- | --- | --- |
| Desired degree level | R | dict `nad_degree_level` |
| Fields of study | R (≥1) | multi-select (dict) |
| Preferred majors | Rec | free-text tags |
| Preferred countries | R (≥1) | ISO-3166 multi-select |
| Preferred universities | O | free-text / catalog refs when catalog exists |
| Scholarship interest | Rec | enum `REQUIRED\|PREFERRED\|NOT_NEEDED` |
| Preferred intake | Rec | `YYYY` + term (`SPRING\|FALL\|SUMMER\|WINTER`) |

### 3.6 Current location (step 6) — new `nad_applicant_residence`

See §2.1 for the branch. Columns cover both branches + `in_china boolean`.

### 3.7 Languages & proficiency (step 7) — new `nad_applicant_language`

| Field | Class | Type |
| --- | --- | --- |
| English proficiency | R | enum `NONE\|BASIC\|INTERMEDIATE\|ADVANCED\|NATIVE` |
| Chinese proficiency | Rec | same enum |
| Other languages (repeatable) | O | `{ language, proficiency }` |

Standardized tests (IELTS/TOEFL/HSK/…) are **step 8**, using the existing
`nad_applicant_test_score` model unchanged.

### 3.8 Work experience (step 9) — new `nad_applicant_work`

`{ employer, title, start_date, end_date?, is_current, description? }` — repeatable,
all Optional.

### 3.9 Certifications (step 10) — new `nad_applicant_certification`

`{ name, issuer?, issued_on?, expires_on?, credential_id? }` — repeatable, Optional.

### 3.10 Guardian / emergency contact (step 11)

Reuses `nad_applicant_contact` (`relation`, `name`, `email?`, `phone?`). ≥1 Required
for onboarding completion. A *login* guardian is still a separate `sys_user` + grant
(`docs/DOMAIN_MODEL.md` §4) — unchanged.

---

## 4. Domain model (PROPOSED)

```
Applicant (existing aggregate root — nad_applicant)
├── ApplicantProfile          (1:1)  identity extras + onboarding state
│     gender, countryOfResidence, passportExpiry, preferredCommsLang,
│     highestEducationLevel, latestDegreeTitle, onboardedAt?, skippedSteps[]
├── ApplicantResidence        (1:1)  in-China branch vs abroad branch (sealed shape)
├── ApplicantInterest         (1:1)  desiredDegreeLevel, scholarshipInterest, intakeYear, intakeTerm
│     ├── InterestField[]      (n)   dict field refs
│     ├── InterestCountry[]    (n)   ISO country refs
│     └── InterestUniversity[] (n)   free text or catalog ref
├── ApplicantLanguage[]       (n)   {language, proficiency}; english/chinese are well-known rows
├── ApplicantWorkExperience[] (n)
├── ApplicantCertification[]  (n)
├── ApplicantEducation[]      (n)   EXISTING
├── ApplicantTestScore[]      (n)   EXISTING
├── ApplicantContact[]        (n)   EXISTING
└── ApplicantDocument[]       (n)   PROFILE_PHOTO, PASSPORT — via the Document slice
```

- `ApplicantProfile.onboardedAt` is the single "is onboarding done" signal.
- `ApplicantResidence` is modeled as one table with an `in_china` discriminator and
  nullable columns per branch; the service validates the active branch only.
- Authorization is unchanged — every read/write goes through
  `@na.canAccessApplicant` + capability checks (`docs/DOMAIN_MODEL.md` §4.4).
- All PII columns follow `docs/SECURITY.md` §6 (masked projection for staff without
  `nad:applicant:pii:view`; unmasked read audited).

---

## 5. Schema (PROPOSED — migration number assigned when scheduled)

```sql
-- extends nad_applicant with 1:1 profile extras
ALTER TABLE nad_applicant
  ADD COLUMN gender                     varchar(16)  NULL,
  ADD COLUMN country_of_residence       char(2)      NULL,
  ADD COLUMN passport_expiry            date         NULL,
  ADD COLUMN preferred_comms_lang       varchar(8)   NULL;

CREATE TABLE nad_applicant_profile (
  applicant_id           bigint      NOT NULL PRIMARY KEY,
  highest_education_level varchar(32) NULL,          -- dict nad_degree_level
  latest_degree_title    varchar(200) NULL,
  onboarded_at           datetime    NULL,
  skipped_steps_json     varchar(500) NULL,
  create_time            datetime    NOT NULL,
  update_time            datetime    NULL,
  CONSTRAINT fk_ap_profile_applicant FOREIGN KEY (applicant_id) REFERENCES nad_applicant(id)
);

CREATE TABLE nad_applicant_residence (
  applicant_id       bigint      NOT NULL PRIMARY KEY,
  in_china           tinyint(1)  NOT NULL,
  -- in-China branch
  cn_city            varchar(120) NULL,
  cn_address         varchar(300) NULL,
  wechat_id          varchar(120) NULL,
  current_school     varchar(200) NULL,
  student_status     varchar(32)  NULL,
  visa_type          varchar(64)  NULL,
  visa_expiry        date         NULL,
  -- abroad branch
  country            char(2)      NULL,
  city               varchar(120) NULL,
  whatsapp           varchar(64)  NULL,
  preferred_contact  varchar(16)  NULL,
  -- shared
  phone              varchar(32)  NULL,
  update_time        datetime     NULL,
  CONSTRAINT fk_ap_res_applicant FOREIGN KEY (applicant_id) REFERENCES nad_applicant(id)
);

CREATE TABLE nad_applicant_interest (
  applicant_id         bigint      NOT NULL PRIMARY KEY,
  desired_degree_level varchar(32) NULL,
  scholarship_interest varchar(16) NULL,             -- REQUIRED|PREFERRED|NOT_NEEDED
  intake_year          smallint    NULL,
  intake_term          varchar(16) NULL,
  majors_json          varchar(500) NULL,
  CONSTRAINT fk_ap_int_applicant FOREIGN KEY (applicant_id) REFERENCES nad_applicant(id)
);
CREATE TABLE nad_applicant_interest_field   (applicant_id bigint NOT NULL, field_code varchar(64) NOT NULL, PRIMARY KEY (applicant_id, field_code));
CREATE TABLE nad_applicant_interest_country (applicant_id bigint NOT NULL, country char(2)       NOT NULL, PRIMARY KEY (applicant_id, country));
CREATE TABLE nad_applicant_interest_uni     (id bigint AUTO_INCREMENT PRIMARY KEY, applicant_id bigint NOT NULL, label varchar(200) NOT NULL, university_id bigint NULL);

CREATE TABLE nad_applicant_language (
  id           bigint AUTO_INCREMENT PRIMARY KEY,
  applicant_id bigint       NOT NULL,
  language     varchar(32)  NOT NULL,                -- 'en','zh', ISO code, or free
  proficiency  varchar(16)  NOT NULL,               -- NONE|BASIC|INTERMEDIATE|ADVANCED|NATIVE
  UNIQUE KEY uq_ap_lang (applicant_id, language),
  KEY idx_ap_lang_applicant (applicant_id)
);

CREATE TABLE nad_applicant_work (
  id           bigint AUTO_INCREMENT PRIMARY KEY,
  applicant_id bigint       NOT NULL,
  employer     varchar(200) NOT NULL,
  title        varchar(150) NULL,
  start_date   date         NULL,
  end_date     date         NULL,
  is_current   tinyint(1)   NOT NULL DEFAULT 0,
  description  varchar(1000) NULL,
  KEY idx_ap_work_applicant (applicant_id)
);

CREATE TABLE nad_applicant_certification (
  id            bigint AUTO_INCREMENT PRIMARY KEY,
  applicant_id  bigint       NOT NULL,
  name          varchar(200) NOT NULL,
  issuer        varchar(200) NULL,
  issued_on     date         NULL,
  expires_on    date         NULL,
  credential_id varchar(120) NULL,
  KEY idx_ap_cert_applicant (applicant_id)
);
```

Profile photo + passport are **`nad_document`** rows (Document slice), not columns
here — `{ id, applicant_id, doc_type (PROFILE_PHOTO|PASSPORT), storage_key,
mime_type, size_bytes, status, uploaded_by, uploaded_at, ... }` against object
storage. See `docs/DOCUMENT_MANAGEMENT.md`.

Dictionaries to add: `nad_gender`, `nad_language_proficiency`,
`nad_scholarship_interest`, `nad_student_status`, `nad_intake_term`,
`nad_comms_language`.

---

## 6. API (PROPOSED — all under `/api/student/applicants/{id}/…`, bearer + `@na.canAccessApplicant`)

| Method | Path | Body / notes |
| --- | --- | --- |
| GET / PUT | `/profile` | `ApplicantProfileRequest { gender, countryOfResidence, passportExpiry, preferredCommsLang, highestEducationLevel, latestDegreeTitle }` |
| GET / PUT | `/residence` | `ResidenceRequest` — `{ inChina: true, cnCity, cnAddress, phone, wechatId?, currentSchool?, studentStatus?, visaType?, visaExpiry? }` **or** `{ inChina: false, country, city, phone, whatsapp?, preferredContact }`; server validates the active branch |
| GET / PUT | `/interests` | `InterestRequest { desiredDegreeLevel, fields[], countries[], universities[], majors[], scholarshipInterest, intakeYear, intakeTerm }` |
| GET / POST / PUT / DELETE | `/languages` (+ `/{langId}`) | `LanguageRequest { language, proficiency }` |
| GET / POST / PUT / DELETE | `/work` (+ `/{workId}`) | `WorkRequest { employer, title?, startDate?, endDate?, isCurrent, description? }` |
| GET / POST / PUT / DELETE | `/certifications` (+ `/{certId}`) | `CertificationRequest { name, issuer?, issuedOn?, expiresOn?, credentialId? }` |
| POST / DELETE | `/photo` | multipart upload / remove → Document slice; returns `{ documentId, url }` |
| POST / DELETE | `/passport` | multipart upload / remove → Document slice |
| GET | `/onboarding` | `{ steps: [{ key, status: DONE\|SKIPPED\|TODO }], percent, onboardedAt? }` |
| POST | `/onboarding/complete` | marks `onboarded_at = now` if all Required steps are `DONE` |

- Education / test-scores / contacts keep their existing endpoints unchanged.
- All bodies are Java `record` DTOs; errors are `application/problem+json`.
- Field-level Required/Recommended/Optional is enforced server-side per step, not
  only in the wizard UI.

---

## 7. Sequencing (hard dependency order)

1. **Document slice + object storage** (`docs/DOCUMENT_MANAGEMENT.md`) — profile
   photo and passport upload have no home until this lands.
2. **Catalog** (University/Program/Scholarship) — needed for "preferred
   universities" as real refs; until then free-text.
3. **This onboarding phase** — the schema above, the section endpoints, the wizard.
4. Wires into the **Application** business case (`docs/APPLICATION_WORKFLOW.md`) —
   a completed profile is a precondition for creating an application.

Until then, `/dashboard/profile` stays at Revision 1 scope and this document is the
agreed target.
