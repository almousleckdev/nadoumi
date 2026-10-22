# Nadoumi — Student Application Process

Status: **PROPOSED** (development spec, nothing in §5–§16 is built yet) · written 2026-10-22 · revised with the product owner's decisions (§16)

Legend used throughout, as required by `CLAUDE.md` §5:

| Label | Meaning |
| --- | --- |
| **EXISTING** | verified in the repository today |
| **PROPOSED** | the design this document recommends; not implemented |
| **DECISION REQUIRED** | needs a product owner answer before the affected slice starts |

> Companion documents: `APPLICATION_WORKFLOW.md` (workflow engine), `WORKFLOW_PROGRAM_WITH_SCHOLARSHIP.md`
> (stages), `DOCUMENT_MANAGEMENT.md` (documents, media access classes), `APPLICANT_ONBOARDING.md`
> (profile capture), `FRONTEND_ARCHITECTURE.md` and `CLAUDE.md` §24 (design rules).
> This document is the single source for the **student-facing application journey**: from pressing
> **Apply** to the **Nadoumi Application Form PDF** reaching the Nadoumi administrators, and the **payment
> receipt** reaching the student.

---

> **Update 2026-10-22: the application fee is IMPLEMENTED (supersedes the payment parts of §7, §9 and the AWAITING_PAYMENT model below).**
>
> * **Domain correction.** Programs belong to universities and carry no scholarship. Scholarships are standalone
>   and are not linked to a university. Every mention below of a "programme with scholarship" is superseded: an
>   application is either a program application or a scholarship application. Decision D19 is withdrawn. **`SCHOLARSHIP_ONLY` is
>   now implemented (V114, V115):** `program_id` is nullable; `ApplicationShape` states which columns each type must
>   and must not carry (a scholarship-only application has a scholarship and no programme or intake); it starts on the
>   new `SCHOLARSHIP_ONLY_V1` workflow (submit guard `FIELD_SET:scholarship_id`; stages Submitted, Eligibility review,
>   Document collection, Package preparation, Submitted to scholarship body, Scholarship decision, Award response,
>   Awarded). Like the other definitions it is seeded DRAFT and an administrator must activate it.
> * **Model.** A draft is always saved, paid or not. Submit is refused with HTTP 402 until the fee is `CONFIRMED`
>   (checked in `StudentApplicationService.submit` through the `PaymentGuardProvider` SPI, for every draft).
>   **Pay** opens a session (`PENDING`, 15 minutes, `expires_at`): the QR is shown with a server-computed countdown.
>   The student pays, then reports it (transaction number + screenshot) inside the window
>   (`PROOF_SUBMITTED`). Staff with `nad:payment:verify` confirm or reject (reason 3 to 300 chars). Statuses:
>   `PENDING, PROOF_SUBMITTED, CONFIRMED, REJECTED, EXPIRED, CANCELLED`. A window that closes is enforced by a
>   conditional update (`expires_at > now`), lazily on read, and by the `applicationPaymentExpiryJob` sweep (5 min).
> * **QR decision.** `paymentqr.png` is a static merchant QR (no amount, no order). It is **not stored in MySQL**.
>   It ships as a classpath resource of `nadoumi-payment` and is served only by
>   `GET /api/student/applications/{id}/payment/qr` while the caller owns the application and a session is open
>   (`Cache-Control: no-store`). It is never a public file. An admin-managed override through the media module is
>   possible later (separate permission, audited); it is not built.
> * **Integrity.** Unique `reference` (`NAD-XXXXXXXX`); one open payment per application (generated `open_flag`);
>   one live payment per transaction number (generated `txn_key`); atomic `where status=? and version=?` decisions
>   (second confirm is 409); a staff member cannot verify a payment opened by their own account (403); revenue is
>   booked once (`RevenueService.recordApplicationFee`, idempotent). Proof screenshots are `PAYMENT_PROOF` media
>   (SENSITIVE, streamed by the backend, never a storage URL). Every transition writes `nad_application_payment_event`.
> * **Config.** `sys_config` `nadoumi.application.fee` is seeded at **0, which disables the payment step**; set the
>   real amount in System > Configuration before launch. `nadoumi.payment.payee` is the payee label.
> * **Shipped as** migrations V110 (tables) and V111 (config, job, menu, templates); module `nadoumi-payment`;
>   admin Payments queue (`/payments`); student `PaymentPanel` on the application page.
>
> **Update 2026-10-22 (later): the receipt and the Application Form PDF are IMPLEMENTED (§10 is superseded where it differs).**
>
> * **Renderer decision (supersedes §10.4): OpenPDF, built programmatically, not OpenHTMLtoPDF.** Student text is
>   drawn as plain strings and never parsed as HTML or CSS, so the renderer has no resource-loading surface (no
>   SSRF, no file read, no XXE) and needs no deny-all resolver. Fonts (Noto Sans, OFL) and the logo are classpath
>   resources. Glyphs the font lacks become `?`; control characters are dropped. Passport names are Latin by
>   definition; CJK labels are not printed yet.
> * **Look.** A4, logo and title masthead, numbered orange section badges, solid-white table rows, the logo as a
>   6 % watermark under all content on every page, the company footer (address, emails, phones, website from
>   `nadoumi.brand.*`) and `Page n / N` on every page. Prices are red. Times are shown in UTC+8.
> * **Confidentiality, in one place.** `ApplicationOpportunityService.describe` returns the university only for a
>   plain programme application; any application that involves a scholarship omits it. Both PDFs, the emails and
>   the notifications are built from that description, so a template cannot print what the model does not carry.
> * **Flow.** `applicationPaymentDocumentsJob` (every minute, V113): (1) a confirmed payment gets its receipt and
>   form v1 rendered and stored as `PAYMENT_DOCUMENT` media (SENSITIVE, backend-streamed) with a row in
>   `nad_payment_document` (number, version, SHA-256); (2) the receipt is emailed to the student (applicant's
>   verified email, else the paying account); (3) once the application is submitted, form v2 is rendered from the
>   current profile; (4) v2 is emailed to the **administrators only**: active users holding a role in
>   `nadoumi.application.notify-roles` (default `nadoumi_super_admin`), each as a separate message. Staff roles
>   receive nothing. Every send takes a unique claim first (`nad_payment_document_delivery`), so a retry or a second
>   worker never sends twice; a failed send releases its claim and is retried.
> * **Email.** `EmailMessage` gained attachments (10 MB cap, never serialised, names only in the dev outbox).
>   Subjects are stripped of CR/LF and control characters and length-capped. The old plain "payment confirmed"
>   email was removed (V113) because the receipt email replaces it; the in-app notice stays.
> * **API.** `GET /api/student/applications/{id}/payment/documents` (list) and `.../documents/{RECEIPT|APPLICATION_FORM}`
>   (PDF, `no-store`, access-logged), both under `VIEW_APPLICATION`.
> * **Form content = what the portal collects.** Applying for, personal and passport, marital status, religion, place of
>   birth, official language and permanent address, test scores, education, work experience, **family members, financial
>   supporter and emergency contact in China**, study plan, the affirmation and a signature line. A section a student has
>   not filled is printed as "Not provided". The applicant's profile photo is printed in the masthead (re-encoded to a bounded JPEG; a format the JDK cannot decode, such as AVIF or WebP, simply leaves it out). **Not built:** Chinese labels, the verification QR.
> * **Erasure.** `PaymentErasure` also deletes the generated documents and soft-deletes their PDFs.
>
> **Update 2026-10-22 (conditional requirements): requirement conditions are IMPLEMENTED (V119).** A required document (programme or scholarship) can carry an
> `appliesWhen` condition: `MINOR` (under 18), `IN_CHINA`, `STUDIED_IN_CHINA`, `MASTER_OR_DOCTORAL`, `DOCTORAL`. Admins pick it in the programme and scholarship
> drawers; the checklist leaves a requirement off when it does not apply to that student (`RequirementApplicability`, evaluated from the profile: date of birth,
> location answers, study level from the study interests else the programme). **A fact that is not known yet counts as "applies"**, so a document is asked for
> rather than skipped. The public scholarship page shows the condition under the document.
>
> **Update 2026-10-22 (signature): the handwritten signature is IMPLEMENTED.** A student draws it on a pad or uploads a picture, once
> (`POST /api/student/applicants/{id}/signature`). The image is decoded and redrawn as a bounded PNG on white (an empty pad or a
> non-image is refused, 400), stored as a SENSITIVE `APPLICANT_SIGNATURE` media asset on `nad_applicant_personal` (V118), and shown only to the
> owner through the backend (`GET .../signature/image`). It is printed above the signature line of the Application Form with the day it was given.
> **Submit is refused (409) until the applicant has signed** (the form is not valid without it), as part of the engine's `SubmitGate`. The readiness
> hint lists `SIGNATURE`. Replacing it soft-deletes the old image; erasing the applicant removes it.
>
> **Update 2026-10-22 (documents checklist): Step 3 is IMPLEMENTED in its first form.** The documents an application needs come from the
> programme (new **Required documents** section in the admin programme drawer, stored as `nad_document_requirement` scope PROGRAM)
> and from the scholarship's own list, merged by document type (mandatory if either says so). `GET /api/student/applications/{id}/documents/checklist`
> returns one row per type with the student's status (`MISSING`, `SUBMITTED`, `IN_REVIEW`, `VERIFIED`, `REJECTED`, `EXPIRED`) and a rejection
> reason; each row is matched to the student's best document of that type, either uploaded for this application or kept on their profile
> (a passport), never a document of another application. The application page shows it with an upload per row (a rejected document is
> replaced by a new version), and **submit is refused (409) until every mandatory document is uploaded and not rejected**, after the payment
> check. An application whose programme or scholarship lists nothing is not held back. **One vocabulary:** scholarship requirement codes were
> renamed to the `nad_document_type` dictionary codes (V117: DEGREE to DEGREE_CERTIFICATE, LANGUAGE_CERT to LANGUAGE_TEST_REPORT,
> BANK_STATEMENT to FINANCIAL_PROOF; STUDY_PLAN, PHYSICAL_EXAM, PHOTO and CV added to the dictionary). **Not built:** per-requirement
> conditions (minor, in China, degree level), formats and word counts, templates to download, waiving a requirement, the signed application
> form as a required upload.
>
> **Update 2026-10-22 (family data): the missing onboarding data is IMPLEMENTED (§6.3).** `/dashboard/family` holds four
> sections, each saved once on the applicant and reused for every application: **personal details** (marital status, religion,
> place of birth, official language, permanent address and post code), **family members** (father, mother, spouse, siblings,
> other relatives; a parent or relative who has died is kept with a note), the **financial supporter** and the **emergency
> contact in China** (one of each, the same fields). Rules (`FamilyRules`): a father, a mother and a spouse are recorded at most
> once; a death needs a note; the section is complete when both parents are recorded and, if one has died, at least two siblings
> or other relatives are. `GET /api/student/applicants/{id}/form-readiness` lists what the form still lacks; the application
> page shows it as guidance with a link (it does not block submitting). The data is in the printed form and erased with the
> applicant. It is deliberately **not** part of the onboarding gate, so signing up stays short.
>
> **Update 2026-10-22 (apply screen): the first slice of the apply journey is IMPLEMENTED.** `/dashboard/apply?programme=<slug>`
> or `?scholarship=<slug>` (the Apply buttons on the programme and scholarship pages link here; a guest signs in first and
> is returned to it). The student picks which student the application is for (every applicant the account may act for),
> a programme also asks for an intake, and **Start application** creates the draft (`POST /api/student/applications`, grant
> checked server-side) and opens `/dashboard/applications/[id]`, where the payment card and Submit already live. An existing
> unsubmitted draft of the same student for the same opportunity is offered as **Continue** instead of a duplicate.
> A programme page starts `PROGRAM_ONLY`; a scholarship page starts `SCHOLARSHIP_ONLY` and shows no university.
> `StudentApplicationResponse` gained `applicantId`. **Not built yet from §5:** template downloads, eligibility hints, the
> missing-information panel, application details (Step 2), the documents checklist (Step 3), a guided review step, and an
> inline "add a new student" form.

## 1. Goal and scope

A student (or someone applying on a student's behalf) applies for a **programme** or a **scholarship** on
Nadoumi. The journey is:

```
Apply  →  1 Student & template  →  2 Application details  →  3 Documents  →  4 Payment (QR)  →  5 Submit
                                                                                      │
     Nadoumi application fee confirmed ──► Application Form (PDF) rendered + Receipt (PDF) rendered
                                                 │                                   └──► emailed to the student
                                                 ▼
                          Submit ──► Application Form PDF emailed to the Nadoumi ADMINS (admins only)
```

Rules given by the product owner (all carried into this design):

1. **Apply** opens an apply screen. Step 1 selects the student and offers **Download Application Form
   Template**. The template is uploaded by an admin when creating the programme or the scholarship.
2. When an existing student is selected, every known fact is **auto-completed**. When the student does not
   exist yet, the applicant fills in all of it during the application.
3. Facts the Nadoumi form needs but onboarding does not collect today (marital status, religion, permanent
   address, …) are **added to registration/onboarding**.
4. Step 2 of the user's description is **document upload**. Each programme and scholarship has its own
   **required documents, chosen by the admin** at creation time, plus the signed application form template.
5. The fee is the **Nadoumi application fee**. Until it is paid, the student **cannot submit** and **cannot
   download** the Nadoumi Application Form. Payment is by **QR code** for now.
6. **When the payment is confirmed the system renders** the Nadoumi Application Form (PDF) **and a branded
   payment receipt**. The receipt is **emailed to the student automatically**. When the application is submitted,
   the PDF goes to the Nadoumi **administrators only** (not to staff) with the submit notification.
7. Everything in `applicationdocument/NADOUMI APPLICATION FORM.doc` is part of that form.

Out of scope here: the post-submission workflow stages themselves (already built, §4.2), card/online payment
(§10.7 leaves the seam), university-side submission mechanics.

---

## 2. Research

### 2.1 Method and limits

Research was restricted to the two sources named in the request, plus the Nadoumi form:

* `https://www.scholarshipchina.com` and its programme pages and news site;
* `https://www.eduprchina.com` and its news site;
* `applicationdocument/NADOUMI APPLICATION FORM.doc` (converted to text and read in full).

Both home pages are script-rendered and expose navigation only ("How to Apply", "Apply", "Log in",
"Sign up", "Wallet", "Orders", …), and many deep programme URLs return 404 to an automated fetch, so the
evidence below comes from the programme/news pages that **could** be read and from search snippets of those
same sites. It is marketing content, some of it dated (2024–2026 intakes). Figures are examples of how the
market presents requirements, **not** rules Nadoumi must copy; every number Nadoumi uses must be entered by an
admin per programme (§8) and confirmed with the university. Nothing below was taken from other sites.

### 2.2 How ScholarshipChina handles an application

| Topic | Observed |
| --- | --- |
| Entry | Register an account, pick a programme, submit the application with documents; or contact a consultant by QR code, email or WhatsApp. |
| Programme facts shown before applying | Scholarship/programme ID, city, degree, **duration**, **intake** (e.g. 2026 Autumn), **deadline**, teaching language, **age range** (e.g. 18–25), whether **minors** and **students already in China** are accepted, **applicant location**, academic requirement (e.g. score over 60 %, no failed subjects), original tuition and dorm (single/double/quad), **scholarship tiers** (e.g. year-1 free tuition, later years by performance; "Belt and Road" vs Type A). |
| Documents (bachelor) | passport-sized photo; passport ID page; academic transcripts (colour scan); highest diploma (colour scan); foreigner physical examination form; non-criminal record (colour scan); English proficiency certificate; **bank statement above 30,000 RMB**; **application form**; guarantee letter **if under 18**; residence permit/valid visa **if in China**; study certificate and transcript **if previously studied in China**; personal statement in English; optional CSCA transcript; "other". |
| Documents (Chinese language, 1 year) | the same core set, plus: passport valid **at least one year beyond enrolment**; translations **notarised with official seal**; physical examination and non-criminal record **valid within 6 months**; bank statement equal to **one year's tuition**; scholarship application form; **self-introduction video, max 1 minute, MP4, under 40 MB**; guarantee letter for minors requiring a guardian **and** an entrusted guardian who holds Chinese nationality and has a job. |
| Fees | **Application fee ≈ 350–500 RMB, non-refundable, paid right after submitting — processing does not start until it is paid.** A separate **project/service fee** (≈ 2,980–5,400 RMB) is charged at a later milestone (pre-admission or JW202, depending on the listing). A **school deposit/registration fee** is paid to the university when the digital admission arrives, to get the JW202 processed. |
| Timeline | After university assessment a digital admission notice follows in about **5–10 working days**; then JW202. |

### 2.3 How EDUPRCHINA handles an application

| Topic | Observed |
| --- | --- |
| Entry | Three steps on every programme page: **1)** sign up on the site, **2)** enter personal information and upload basic documents to get the account **approved**, **3)** start student applications online. Account area: Orders, Wallet, Favorites, Messages, Vouchers. |
| Programme facts | degree, teaching language, city, intake, **deadline** (a date), **age 18–28**, minors not accepted, prior China study accepted, applicant location unrestricted, score over 60 %, tuition per year (e.g. 16,000 or 30,000 RMB), **single room 6,000 / double 3,000 RMB per year**, "No CSCA required". |
| Documents | the same core set; many programmes add an **application form**, **study plan (usually ≥ 1,000 words; a fast-track Chinese programme ≥ 400)**, **two recommendation letters**, language certificate (master: **TOEFL iBT 65+ / IELTS 5.5+**; Chinese-taught: **HSK 4+**), **bank statement (10,000 RMB or 3,000 USD, by programme)**, CV, residence permit, self-introduction video. Where IELTS/TOEFL is missing: a **3–5 minute English video or an interview**. |
| Upload limits stated on forms | 5 MB for transcripts/diplomas; 10 MB and up to 10 files for other documents; up to 50 MB for the passport page. |
| Speed claims | "Pre-admission in 3 working days", "FAST admission". |

### 2.4 The Nadoumi form (`NADOUMI APPLICATION FORM.doc`)

Bilingual (English + Chinese) labels; `*` marks required fields. Sections, in order:

1. **Personal info** — surname, given name (as in passport), date of birth, place of birth, nationality,
   present country/city, passport no., passport expiry date, sex, religion, native language, official
   language, consulate name and email for the visa application, marital status (single/married/other),
   highest education level (high school/bachelor/master/doctoral), **current contact** (address, tel, email,
   post code), **permanent contact** (address, tel, email, post code), Chinese proficiency (excellent/good/
   poor/none), HSK level (none, 1–6), English proficiency, TOEFL score, IELTS score.
2. **Education background** (primary school until now): stage, start–end (YYMM), institution, country,
   major, GPA — rows for primary, middle, high school, bachelor, master, doctor, non-degree/training.
3. **Work experience** (required if graduated more than 3 months ago; gaps must be filled to today):
   period, company, position, company address.
4. **Family members** (parents required; if deceased add a note and at least two siblings/relatives):
   father, mother, spouse, other — full name, age, date of birth, nationality, company, position, company
   address, phone, email.
5. **Financial supporter** (must match the bank statement or guarantee letter): name, relationship, sex,
   email, tel, position, company name and address, residential address. Plus **emergency contact in China**
   (same fields, occupation and institution).
6. **Study plan in China**: degree (non-degree/bachelor/master/doctoral), majors (multiple), teaching
   language (English/Chinese), study period (from–to), preferred universities (none/list), preferred city or
   province (none/list), **student category** (self-support, or scholarship: full/partial/other), and for
   scholarships a ~50-word **reason**.
7. **Handwritten signature picture** — "the application is invalid without the signature".
8. **Documents to attach** (by programme): passport photo, passport ID page, highest diploma, transcripts,
   physical examination form, non-criminal record, English proficiency certificate, Chinese proficiency
   certificate, **study plan (≥ 800 words, master/doctor)**, **two recommendation letters (master/doctor, with
   professor's signature and contact)**, **CV (master/doctor)**, **thesis (PhD)**, **school transfer
   certificate** and **study certificate in China** (only if already studying in China). Certificates in a third
   language must be notarised into Chinese or English.
9. **Applicant's six-point affirmation** (information is true; agency may receive admission documents
   including JW202; obey Chinese law; obey the host university's arrangements and rules; accept sanctions),
   signature and date.
10. **Footer**: Nadoumi Education Consulting, 802 Building 6, JiuZhou BeiJun, FuXi Road No 2, Fucheng
    District, Mianyang, Sichuan, China · team@nadoumiconsulting.com · Tel/WhatsApp/WeChat +86 15908237607.

### 2.5 What the research implies for Nadoumi

1. The document list is **per programme/scholarship and conditional** (minor, in China, master/PhD, studied in
   China). A flat list is wrong; requirements need **conditions**, **validity windows**, **formats/sizes**,
   **minimum word counts** and **counts** (two letters).
2. The **application form is itself a required document**: a template the student downloads, completes/signs
   and uploads back. It is not the same thing as Nadoumi's own form.
3. The **application fee gates processing**. Both sites treat payment as the moment the case becomes real.
4. Programme pages publish **eligibility facts** (age, minors, applicant location, prior China study, score).
   Nadoumi already stores most of these on scholarships (§4.1); the apply screen should show them as hints.
5. Most of the Nadoumi form is **about the person**, not the application. It belongs on the profile and is
   asked once (`CLAUDE.md` §24, "ask for a fact once").

---

## 3. Principles

1. **Ask once.** Anything about the person lives on the applicant profile (single source of truth). The
   application stores only what is specific to *this* application.
2. **The server is the record.** Progress, payment and unlocks are server state; a student who closes the
   browser resumes where the server says they stopped (same rule as onboarding).
3. **Authorization on the server.** Every endpoint checks the caller's `nad_user_applicant_access` grant; a
   student can apply only for applicants they may act for. Payment status and the PDF unlock are decided
   server-side, never by the UI.
4. **History is preserved.** Submitting freezes snapshots; payments and generated forms are append-only.
5. **Shared components.** The wizard reuses `SidebarFrame`/stepper patterns, `FilePicker`, `DetailCard`,
   `PriceRows`, and the admin reuses one document-requirements editor for programmes and scholarships.
   No second copy of anything (`CLAUDE.md` §24).

---

## 4. What exists today

### 4.1 EXISTING — verified in the repository

| Area | State |
| --- | --- |
| **Application aggregate** | `nad_application` with `applicant_id`, `application_type` (`PROGRAM_WITH_SCHOLARSHIP`, `PROGRAM_ONLY`), `program_id`, `scholarship_id`, `intake_id`, `current_stage_id`/`current_status` (engine-written), `assignee_user_id`, `submitted_at`, optimistic `version`. |
| **Workflow engine** | Versioned definitions (`nad_wf_definition/stage/transition/stage_task_template`), per-application instance, append-only `stage_history`, `event`, `decision`, `task`. Seeded `PROGRAM_WITH_SCHOLARSHIP_V1` and `PROGRAM_ONLY_V1` (V66/V67): DRAFT → SUBMITTED → ELIGIBILITY_REVIEW → DOCUMENT_COLLECTION → PACKAGE_PREPARATION → SUBMITTED_TO_UNIVERSITY → UNIVERSITY_DECISION → (SCHOLARSHIP_DECISION) → OFFER_RESPONSE → PRE_DEPARTURE → ENROLLED, with UNSUCCESSFUL/WITHDRAWN terminals. `submit` is guarded by `FIELD_SET` on `program_id` and `intake_id`. |
| **Student API** | `POST /api/public/applications` (authenticated despite the prefix; body `applicantId`, `applicationType`, `programId`, `scholarshipId`, `intakeId`), `GET /api/student/applications[/{id}]`, `PUT /{id}` (draft opportunity fields), `POST /{id}/submit`, `POST /{id}/transitions/{accept_offer\|decline_offer\|withdraw}`. |
| **Snapshots on submit** | `nad_application_snapshot` kinds `PROFILE` and `REQUIREMENTS` (immutable, written once). |
| **Domain event** | `ApplicationSubmitted` exists in `OutboxEventTypes`. |
| **Guard seams, unbacked** | `DocumentGuardProvider` (`ALL_REQUIRED_DOCUMENTS_ATTACHED/VERIFIED`) and `PaymentGuardProvider` (`PAYMENT_SETTLED(kind)`) are interfaces with **no bean**; `GuardEvaluator#activate` rejects any definition using them (fail-closed). The seeded definitions gate on recorded `DOCUMENTS_COMPLETE` and `FEE_SETTLED` **decisions** instead. |
| **Documents** | `nad_document` (`applicant_id`, `application_id`, `doc_type`, status `DRAFT/SUBMITTED/IN_REVIEW/VERIFIED/REJECTED/EXPIRED`, reviewer, rejection reason, expiry) with versions, events, student upload/download endpoints, media access classes `PROTECTED`/`SENSITIVE`. Ten `doc_type` values in dictionary `nad_document_type`: PASSPORT, VISA, FINANCIAL_PROOF, TRANSCRIPT, POLICE_CLEARANCE, DEGREE_CERTIFICATE, RECOMMENDATION_LETTER, PERSONAL_STATEMENT, LANGUAGE_TEST_REPORT, OTHER. |
| **Required documents** | Two parallel mechanisms: `nad_scholarship_document_requirement` (doc type, mandatory, note — edited in the **scholarship** admin form) and the generic `nad_document_requirement` (scope `PROGRAM`/`SCHOLARSHIP`/`WF_STAGE`, mandatory, waivable, notes) read through `DocumentRequirementService.checklistFor`. **Programmes have no admin editor for required documents.** |
| **Applicant profile** | `nad_applicant`: names, DOB, nationality, passport number, **passport issue/expiry**, gender, country of origin/residence, native language, email (verified), phone, WeChat, WhatsApp. Children: **residence** (in China, country, city, address, China education level, China school, **visa type, visa expiry**), **contacts** (GUARDIAN/EMERGENCY/OTHER: name, email, phone), **education**, **work**, **test scores** (type, score, sub-scores, dates), **interests** (level, scholarship interest, intake, teaching language, fields, cities). |
| **Opportunity data** | Scholarship: eligibility (age, in-China rule, GPA, IELTS, TOEFL, HSK, nationality), fees (application fee, service fee), intakes, document requirements. Programme (V108): offerings (level × duration × tuition × accommodation), admission requirements, documents text, min HSK/IELTS/TOEFL, **application fee**. |
| **Student UI** | `/dashboard/applications` (list) and `/dashboard/applications/[id]` (detail, timeline, withdraw). "Apply now" on programme/scholarship pages currently only routes to `/register` or `/dashboard`. **There is no apply screen.** |
| **Finance** | `nad-finance` is a **staff-entered ledger** (revenue/expense with a receipt writer). There is no student-facing payment, no QR, no payment status on an application. |
| **Mail** | `EmailMessage(to, subject, body, htmlBody)` — **no attachment support**. |
| **PDF** | **No PDF library** is on any module classpath. |

### 4.2 Gaps this document closes

| # | Gap |
| --- | --- |
| G1 | No apply screen, no student selection, no template download. |
| G2 | Profile lacks marital status, religion, official language, place of birth, permanent address and post code, family members, financial supporter, study plan, signature, "ever been to China". |
| G3 | Programmes cannot declare required documents; scholarship and generic requirement tables overlap; requirements have no conditions, formats, validity, word counts, counts or template. |
| G4 | No application-form template storage or admin upload. |
| G5 | No payment model, no QR settings, no proof upload/verification, `PAYMENT_SETTLED` unbacked. |
| G6 | No Nadoumi Application Form PDF, no PDF engine, no email attachment, no team notification carrying the form. |
| G7 | Submit does not require completed details, documents or payment. |

---

## 5. The journey (PROPOSED)

Entry point: **Apply** on a programme or scholarship page → `/dashboard/apply?programme=<slug>` or
`?scholarship=<slug>` (a guest is sent through login/registration first and returned here; a student who has
not finished onboarding is sent to onboarding first).

The wizard is a fixed shell like onboarding (stepper in a sidebar on desktop, segmented progress on phones,
content scrolls, action bar fixed). Every step autosaves; **Save and continue** is the explicit commit. A draft
application row is created on entry, so a returning student resumes through **My applications → Continue**.

### Step 1 — Student and template

* **Opportunity header**: programme/scholarship, university (staff-safe name only for scholarships — the
  student-safe DTO rule of `CLAUDE.md` §8 still applies), level and duration offering, **intake** (required
  because `submit` is guarded by `intake_id`), and — for a programme — the scholarship choice when
  `PROGRAM_WITH_SCHOLARSHIP`.
* **Select student**: every applicant the caller may act for (own profile and on-behalf grants).
  * Existing student → profile loads; **all known facts auto-complete** (§6.2).
  * **Add a new student** → inline form that creates an applicant with the same forms as onboarding. This is
    where "if the student does not exist the applicant fills in all of it" is honoured.
* **Download Application Form Template**: one button per template attached to the opportunity (§8.3). Shows
  version and file name. If a programme and its scholarship both have templates, both are listed; each is also
  a required upload in Step 3.
* **Eligibility hints** (non-blocking, from the opportunity's published facts): age range, minors, applicant
  location, prior China study, minimum HSK/IELTS/TOEFL. A mismatch shows a neutral notice and, for minors, adds
  the guarantee-letter requirement; staff still decide eligibility.
* **Missing-information panel**: the fields the Nadoumi form requires that the selected student does not have
  yet (§6.3), editable inline and saved **to the profile**, so the student is never asked twice.

Gate to continue: student selected, intake chosen, no required profile field missing.

### Step 2 — Application details

Sections follow the Nadoumi form (§2.4). Profile-derived blocks are pre-filled and editable (edits write back
to the profile); application-specific blocks are saved with the application.

| Block | Source | Notes |
| --- | --- | --- |
| Personal info | profile | includes marital status, religion, place of birth, official language, languages/HSK/TOEFL/IELTS |
| Current and permanent contact | profile | address, tel, email, post code (two blocks) |
| China status | profile | ever been to China, currently in China, visa type, visa expiry |
| Education background | profile | primary → doctor and non-degree rows, GPA, dates |
| Work experience | profile | required when the highest education ended more than 3 months ago; gaps flagged up to today |
| Family members | profile | parents required; deceased parent needs a note plus two siblings/relatives |
| Financial supporter | profile | must be consistent with the bank statement/guarantee letter |
| Emergency contact in China | profile | extends the existing `EMERGENCY` contact |
| **Study plan** | **application** | degree, majors (multi), teaching language, from–to, preferred universities, preferred city/province, student category (self-support / full / partial / other), reason (~50 words) for scholarships |
| **Signature** | **application** | handwritten signature image (draw on screen or upload); required |
| **Affirmation** | **application** | six statements accepted with a checkbox, timestamp and IP kept |

### Step 3 — Documents

* Checklist generated from the opportunity's **required documents** (§8), evaluated for this student's
  conditions (minor, in China, studied in China, degree level). Each row shows: name, mandatory/optional,
  why it applies, accepted formats and size, validity window, minimum words/count, a hint, status, and a
  **template** download when one is attached.
* The **signed application form template** is one of the rows (doc type `APPLICATION_FORM_SIGNED`).
* Upload uses `FilePicker`-style compact controls; files are validated by type and size client- and
  server-side; PDFs and images only except the video row. Replacing a file creates a new version.
* Gate to continue: every mandatory row has a file (staff verify later). Conditional rows that do not apply are
  hidden, not greyed.

### Step 4 — Review and pay

* Read-only review of Steps 1–3 with an **Edit** link per block.
* Fee summary (`PriceRows`, prices red): the **Nadoumi application fee** of this programme or scholarship (§8.1),
  in RMB with the USD equivalent.
* **Pay by QR**: the Nadoumi QR image, payee label and instructions (admin-managed, §7.6); the amount; a unique
  **payment reference** to put in the transfer note. The student pays in their banking/payment app, then enters
  the transaction number, uploads a screenshot and presses **I have paid**.
* Status chip: *Awaiting payment → Verification in progress → Confirmed* (or *Rejected, with reason*).
* Until **Confirmed**: **Submit is disabled** and **Download Nadoumi Application Form is disabled**, each with the
  reason shown. Both are also refused by the server.
* **On confirmation** (system actions, §10.4): the details are **frozen into a `FORM` snapshot**, the Application
  Form PDF is rendered, the payment **receipt** is rendered, and the receipt is **emailed to the student**. Steps
  1–2 become read-only (the PDF must keep matching what was paid for); uploading or replacing documents stays
  open until submit. Profile edits that change snapshotted fields are **blocked** for an application whose payment is `CONFIRMED` and that is not yet submitted, so the frozen form cannot diverge from the profile. A change to a frozen detail needs an admin to *reopen details* (admin-only, reason required, audited), which supersedes the PDF and
  re-renders it on the next confirmation-equivalent action (§9.3).

### Step 5 — Download, receipt and submit

* The student can **download the Nadoumi Application Form (PDF)** and the **receipt (PDF)** from the Payment card.
* **Submit application** runs the guarded `submit` transition (§9). On success:
  1. immutable snapshots (`PROFILE`, `REQUIREMENTS`; `FORM` was written at confirmation) are completed;
  2. an `ApplicationSubmitted` event fans out: the **Nadoumi administrators** receive an **email with the
     Application Form PDF attached** (staff do **not** receive this email; staff keep the in-app queue
     notification they already get), and the student receives the existing submission confirmation;
  3. the workflow continues from SUBMITTED as today.

### After submit

Unchanged from the existing workflow (§4.1). The student sees the safe timeline, documents the Nadoumi team asks for
(DOCUMENT_COLLECTION), and offer responses. The fee row stays visible in the application detail.

---

## 6. Profile data: what we have, what we add

### 6.1 Rule

Person facts are captured **in onboarding** and stored on the applicant. The application never keeps its own
copy except the immutable submit snapshot.

### 6.2 Field mapping — Nadoumi form → source

The twenty fields the request lists first, then the rest of the form.

| Form field | Source today | Status |
| --- | --- | --- |
| Surname, Given name | `nad_applicant.family_name`, `given_name` | EXISTING |
| Nationality | `nationality` | EXISTING |
| Gender / Sex | `gender` | EXISTING |
| Date of birth | `dob` | EXISTING |
| Passport no. | `passport_no` | EXISTING |
| Passport expiry date | `passport_expiry_date` | EXISTING |
| Email, Phone, WeChat, WhatsApp | `email`, `phone`, `wechat_id`, `whatsapp` | EXISTING |
| Highest education | derived from `nad_applicant_education.level` (max) | EXISTING (derived) |
| Has the student been in China now? | `nad_applicant_residence.in_china` | EXISTING |
| Visa type, Date of visa expiry | `residence.visa_type`, `visa_expiry_date` | EXISTING |
| **Marital status** | — | **PROPOSED** `marital_status` |
| **Religion** | — | **PROPOSED** `religion` (optional) |
| **Official language** | — | **PROPOSED** `official_language` |
| Native language | `native_language` | EXISTING |
| **Place of birth** | — | **PROPOSED** `place_of_birth` |
| **Permanent address** (+ post code, tel, email) | — | **PROPOSED** block on the residence record |
| Current address (+ post code) | `residence.address`, `country`, `city` | EXISTING, **post code PROPOSED** |
| **Has your student ever been to China?** | — | **PROPOSED** `residence.ever_in_china` |
| Chinese / English proficiency (self-rating) | — | **PROPOSED** two enums on the profile |
| HSK level, TOEFL, IELTS | `nad_applicant_test_score` | EXISTING |
| Education background rows | `nad_applicant_education` | EXISTING (primary/middle school rows **PROPOSED**: new `level` values) |
| Work experience | `nad_applicant_work` | EXISTING |
| **Family members** | — | **PROPOSED** `nad_applicant_family_member` |
| **Financial supporter** | — | **PROPOSED** `nad_applicant_financial_supporter` |
| Emergency contact in China | `nad_applicant_contact` (`EMERGENCY`) | EXISTING, **relationship, sex, occupation, institution, address PROPOSED** |
| Consulate name/email for the visa | — | **PROPOSED**, **application-level** (differs per destination) |
| Study plan, preferred universities/cities, student category, reason | `nad_applicant_interest` partly (level, fields, cities, scholarship interest) | **PROPOSED** application-level copy that defaults from interests |
| Handwritten signature | — | **PROPOSED**, application-level, protected media |

### 6.3 Onboarding additions (PROPOSED)

Registered students already finished onboarding and have `onboarded_at` set, so two things are needed:

1. **New students**: the onboarding wizard (see `APPLICANT_ONBOARDING.md`) gains these fields in the existing
   steps — no new step:
   * *Personal*: marital status, place of birth, official language, religion (optional).
   * *Location*: permanent address block (address, city, country, post code), "have you ever been to China?"
     (shown with the existing in-China question), current post code.
   * *Language/skills* (inside the existing *Education* or *Interests* step): self-rated Chinese and English
     proficiency.
   * Family members and the financial supporter are **not** forced into onboarding (they are long and only
     matter when applying). They are collected in the apply wizard (Step 2) and saved to the profile.
   Required for onboarding completion: marital status, permanent address. Optional: religion, place of birth.
   **DECISION REQUIRED**: confirm that place of birth is optional at onboarding but required by the Nadoumi form
   (the apply wizard then asks for it).
2. **Existing students and on-behalf applicants**: a *profile completeness for applying* check (a pure function
   shared by the server and the UI, like `OnboardingRules`) lists the missing fields in Step 1. They are filled
   inline and saved to the profile. The same check is what makes "student does not exist → applicant fills
   everything" work.

New database columns/tables are in §7.

---

## 7. Data model (PROPOSED)

Next free migration number is **V110** (latest is V109). One migration per logical change, append-only and
immutable once merged (`database.md`). **Every migration carries a manual-rollback header comment**, DDL and data
seeds are separate files, and every new table follows the checklist: surrogate `bigint` key, explicit `ON DELETE`,
unique business keys, indexes on every foreign key, audit columns (`create_by`, `create_time`, `update_by`,
`update_time`).

| Migration | Content |
| --- | --- |
| V110 | applicant application profile (§7.1) |
| V111 | requirement and template columns, Nadoumi application fee columns (§7.2) |
| V112 | data copy: scholarship requirements and the `NADOUMI_APPLICATION` fee line (§7.2) |
| V113 | application form (§7.3) |
| V114 | payments and payment events (§7.4) |
| V115 | generated documents and number sequence (§7.5) |
| V116 | workflow definition v2 seed (§9.2) |

### 7.1 `V110__nad_applicant_application_profile`

```
nad_applicant
  + marital_status       varchar(12)  null   SINGLE | MARRIED | OTHER
  + religion             varchar(60)  null
  + official_language    varchar(40)  null
  + place_of_birth       varchar(120) null
  + chinese_proficiency  varchar(12)  null   EXCELLENT | GOOD | POOR | NONE
  + english_proficiency  varchar(12)  null   EXCELLENT | GOOD | POOR | NONE

nad_applicant_residence
  + ever_in_china        tinyint(1)   null
  + post_code            varchar(20)  null
  + permanent_country    varchar(2)   null
  + permanent_city       varchar(120) null
  + permanent_address    varchar(300) null
  + permanent_post_code  varchar(20)  null
  + permanent_phone      varchar(32)  null
  + permanent_email      varchar(120) null

nad_applicant_contact          (existing EMERGENCY rows gain detail)
  + relationship  varchar(60), sex varchar(8), occupation varchar(120),
    institution varchar(200), institution_address varchar(300), residential_address varchar(300)

nad_applicant_family_member
  id, applicant_id→nad_applicant (CASCADE), relation(FATHER|MOTHER|SPOUSE|SIBLING|OTHER),
  full_name, age?, dob?, nationality?, company?, position?, company_address?, phone?, email?,
  deceased tinyint, note?, sort_order

nad_applicant_financial_supporter      (one per applicant)
  applicant_id (unique), full_name, relationship, sex, email, tel, position,
  company_name, company_address, residential_address

nad_applicant_education  (level enum gains PRIMARY, MIDDLE_SCHOOL, NON_DEGREE; unchanged columns)
```

Every FK `ON DELETE CASCADE` (true ownership), indexes on `applicant_id`, a unique key on the supporter's
`applicant_id` and audit columns on the new tables. Family-member and supporter data and the signature are
personal data: the signature is `SENSITIVE` media, the family/supporter tables are excluded from broad staff lists
(§12.1 S13) and are covered by erasure (§12.1 S14). Erasure paths (`ApplicantErasureService` and the per-module erasure participants)
are extended in the same change.

### 7.2 `V111__nad_opportunity_requirements_and_templates` and `V112__nad_opportunity_requirements_seed`

Converge the two requirement tables on **`nad_document_requirement`** (**DECISION REQUIRED**, recommended) and
add what the research shows is needed:

```
nad_document_requirement
  + label           varchar(120) null     overrides the dictionary label for this opportunity
  + description     varchar(500) null     student-facing instructions (e.g. "valid within 6 months")
  + applies_when    varchar(24)  not null default 'ALWAYS'
        ALWAYS | APPLICANT_UNDER_18 | APPLICANT_IN_CHINA | STUDIED_IN_CHINA | LEVEL_IN
  + applies_levels  varchar(60)  null     "MASTER,PHD" when applies_when = LEVEL_IN
  + max_count       tinyint      not null default 1      (two recommendation letters = 2)
  + accepted_formats varchar(60) null     "pdf,jpg,png" | "mp4"
  + max_size_mb     smallint     null
  + validity_months smallint     null     physical exam / non-criminal record = 6
  + min_words       smallint     null     study plan = 800 / 1000
  + template_media_id bigint     null→nad_media_asset (SET NULL)   the downloadable template
  + sort_order      smallint     not null default 0
```

Three points the convergence must state (architecture review):

* **Uniqueness**: V73 has `unique(scope, ref_id, doc_type)`, which blocks two rows of the same type with different
  conditions (for example a residence permit for applicants in China and a visa for others). V111 replaces it with
  `unique(scope, ref_id, doc_type, applies_when, applies_levels)`.
* **Integrity**: `ref_id` is polymorphic and has no foreign key; the **owning module's service** (programme or
  scholarship) validates the reference on every write and deletes the rows when the opportunity is deleted, exactly as
  `nad_media_asset.owner_id` is handled today.
* **Ownership of writes**: the scholarship and programme services write requirements **through the document
  module's `DocumentRequirementService` interface**; `ScholarshipChildrenWriter` stops writing its own table and the
  old mapper goes read-only. The old table is dropped in a later migration with its own rollback header.

`applies_when` is a **fixed vocabulary evaluated by a pure function** (same discipline as workflow guards, D4);
there is no expression language. `V112` (data only, per `database.md`) copies `nad_scholarship_document_requirement` rows into the
generic table with `scope='SCHOLARSHIP'` and the existing scholarship `NADOUMI_APPLICATION` fee line into
`nadoumi_application_fee_amount`; the old table stays for one release, read-only, then is dropped by its own migration.

New dictionary values for `nad_document_type` (labels in `sys_dict_data`, editable by admin):

`PASSPORT_PHOTO`, `PASSPORT_ID_PAGE`, `HIGHEST_DIPLOMA`, `TRANSCRIPT` (exists), `PHYSICAL_EXAM_FORM`,
`NON_CRIMINAL_RECORD` (maps to the existing `POLICE_CLEARANCE`; keep one), `ENGLISH_PROFICIENCY`,
`CHINESE_PROFICIENCY`, `STUDY_PLAN`, `RECOMMENDATION_LETTER` (exists), `CV`, `THESIS`, `BANK_STATEMENT`
(maps to `FINANCIAL_PROOF`), `GUARANTEE_LETTER`, `RESIDENCE_PERMIT_OR_VISA` (maps to `VISA`),
`STUDY_CERTIFICATE_CHINA`, `SCHOOL_TRANSFER_CERTIFICATE`, `SELF_INTRO_VIDEO`, `CSCA_TRANSCRIPT`,
`APPLICATION_FORM_SIGNED`, `SIGNATURE_IMAGE`, `PAYMENT_PROOF`, `OTHER`. The SENSITIVE override list
(`DOCUMENT_MANAGEMENT.md` §3.2a) is extended to the identity/financial/criminal types.

Application-form **templates** are not a separate table: a template is the `template_media_id` of the
`APPLICATION_FORM_SIGNED` requirement row of a programme or scholarship (versioning = a new media asset; the
old one stays for applications already started). A university-level or global default template resolves when
the programme has none (**DECISION REQUIRED**: resolution order scholarship → programme → university → global).

### 7.3 `V113__nad_application_form`

```
nad_application_form             1:1 with nad_application
  application_id (unique→nad_application, CASCADE)
  study_plan_json   json   degree, majors[], teaching_language, from, to,
                           preferred_universities, preferred_city, student_category,
                           scholarship_kinds[], scholarship_reason
  consulate_name, consulate_email
  signature_media_id→nad_media_asset
  affirmed_at, affirmed_ip
  step_status_json  json   {"student":"DONE","details":"DONE","documents":"OPEN","payment":"OPEN"}
  version  int             optimistic lock

nad_application_snapshot         kind gains 'FORM'
```

### 7.4 `V114__nad_application_payment`

```
nad_application_payment
  id, application_id→nad_application (RESTRICT)
  kind            varchar(24)   APPLICATION_FEE   (SERVICE_FEE reserved)
  amount          decimal(12,2) currency char(3)           ← snapshot of the fee at creation
  method          varchar(12)   QR
  reference       varchar(32)   unique, generated, shown to the student ("NAD-PAY-2026-000123")
  status          varchar(20)   AWAITING_PAYMENT | PROOF_SUBMITTED | CONFIRMED | REJECTED | CANCELLED
  student_txn_no  varchar(80)   null   what the student typed
  proof_media_id  bigint        null→nad_media_asset (PROTECTED)
  submitted_at    datetime null
  verified_by     bigint null, verified_at datetime null, rejection_reason varchar(300) null
  create_by/time, update_by/time, version
  key (application_id, kind, status)

nad_application_payment_event    append-only history (from_status, to_status, actor, at, note)
```

Money is `decimal`, timestamps UTC (`database.md`). A new attempt after a rejection adds a **new row**; history
is never overwritten.

Integrity rules (security review):

* **Amount snapshot**: `amount`/`currency` are frozen when the row is created in `AWAITING_PAYMENT`, from the
  opportunity's Nadoumi application fee (or the global default). The client never sends an amount. If the student
  changes the opportunity, intake or applicant (`PUT /applicant`), the open payment is **cancelled and a new one
  created** with the then-current fee. Staff confirmation screens always show the snapshot amount.
* **One open payment per (application, kind)**: unique index on `(application_id, kind, open_flag)`, where
  `open_flag` is a generated column that is 1 for `AWAITING_PAYMENT`/`PROOF_SUBMITTED`, null otherwise.
* **One transfer, one application**: unique `(student_txn_no)` among non-rejected payments, so a single bank
  transfer cannot back two applications.
* **Separation of duties**: `verified_by` must differ from the creating user and must not be a user linked to the
  applicant (a staff member cannot confirm their own or their relative's payment).
* **Atomic confirm**: `UPDATE nad_application_payment SET status='CONFIRMED', verified_by=?, verified_at=?, version=version+1
  WHERE id=? AND status='PROOF_SUBMITTED' AND version=?`; **0 rows → HTTP 409**, no side effects. The ledger row,
  the document numbers and the outbox event are written in **that same transaction**.

### 7.5 `V115__nad_generated_document`

One table for both generated PDFs (application form and receipt) so the storage, hashing and delivery code exists
once:

```
nad_generated_document
  id
  kind          varchar(20)   APPLICATION_FORM | PAYMENT_RECEIPT
  application_id →nad_application (RESTRICT)
  payment_id     →nad_application_payment (RESTRICT, null for APPLICATION_FORM)
  doc_number    varchar(32)   unique   "NAC-APP-2026-000123" | "NAD-RCT-2026-000123"
  version       int           1..      (a re-render after "reopen details" adds a version; old ones stay)
  media_id      →nad_media_asset (SENSITIVE)   categories APPLICATION_FORM_PDF / PAYMENT_RECEIPT_PDF
  sha256        char(64)
  generated_at  datetime, generated_by bigint (null = system)
  emailed_at    datetime null, emailed_to varchar(255) null
  unique (application_id, kind, version)
  unique (payment_id, kind)            -- a receipt exists once per payment
```

`nad_revenue` additionally gets a unique key on `(related_type, related_id, reference)` so a repeated confirmation
cannot book the fee twice. `media_id` is **never returned in any DTO** and these assets are unreachable through
the generic `/api/media` route: the media guard for `APPLICATION_FORM_PDF`, `PAYMENT_RECEIPT_PDF`, `SIGNATURE` and
`PAYMENT_PROOF` calls the same rules as §12.1 (grant, payment `CONFIRMED` where applicable) and refuses otherwise.

Numbers come from a gap-tolerant counter per kind and year, never from `max()+1`:

```
nad_number_sequence               created by V115 with nad_generated_document
  kind        varchar(20)  APPLICATION_FORM | PAYMENT_RECEIPT
  year        smallint
  next_value  bigint       not null default 1
  primary key (kind, year)
```

The service does `SELECT … FOR UPDATE` on the row, uses `next_value`, increments it and inserts the document **in the
same transaction**; a rolled-back transaction leaves a gap, which is acceptable. Receipts are immutable once issued; a reprint is the same number with a
"Duplicate copy" mark, not a new number.

### 7.6 Settings (no new table)

System configuration keys (existing `sys_config`, editable in the admin System screens, one dedicated
**Payment settings** screen over them):

* payment QR media id, payee label, payment instructions (EN/ZH);
* `nadoumi.application.fee` — the **global default Nadoumi application fee** (RMB);
* `nadoumi.application.notify-roles` — role keys whose active members receive the submit email
  (default `nadoumi_super_admin`; **admins only**, never staff roles);
* the **company block** used on both PDFs: legal name, full address, email, phone numbers, website. Seed values
  are the ones the platform already uses: *Nadoumi Education Consulting*, *802, Building 6, JiuZhou BeiJun, FuXi
  Road No 2, Fucheng District, Mianyang, Sichuan, China*, `team@nadoumiconsulting.com`,
  `+86 159 0823 7607 / +86 155 2057 6024`, `https://nadoumi.com`;
* receipt/number prefixes.

### 7.7 New media categories

`MediaCategory` additions with the policy table in `MediaCategoryPolicy` (MIME, size, access class):

| Category | Access | Allowed | Max |
| --- | --- | --- | --- |
| `APPLICATION_FORM_TEMPLATE` | PROTECTED, served only by the grant-checked `/templates/{requirementId}` route, access-logged | **pdf, docx** (legacy `.doc` is not accepted: its type cannot be verified from content) | 10 MB |
| `PAYMENT_QR` | PUBLIC (read-only to students) | jpeg, png, webp | 2 MB |
| `PAYMENT_PROOF` | PROTECTED | jpeg, png, pdf | 5 MB |
| `SIGNATURE` | SENSITIVE | png, jpeg | 1 MB |
| `APPLICATION_FORM_PDF` | SENSITIVE | pdf | 10 MB |
| `PAYMENT_RECEIPT_PDF` | SENSITIVE | pdf | 5 MB |
| `APPLICATION_VIDEO` | PROTECTED | mp4 | 40 MB |

`MediaValidation` today understands images, PDF and Office files only. Two changes ship with these categories: an **MP4 sniff** (an `ftyp` brand check; anything else is refused; videos are never served inline) and a **streamed size check** that does not trust the declared length. The 8 KB sniff window is accepted for images/PDF but videos must also pass the brand check. All downloads carry `X-Content-Type-Options: nosniff`, `Content-Disposition: attachment` and `Cache-Control: no-store`.

**Changing the payment QR is a fraud vector** (a swapped QR redirects every student's money): it needs its own permission `nad:payment:settings`, writes an audit record with the old and new asset ids, and notifies the administrators.

Existing `APPLICATION_DOCUMENT` (pdf, jpeg, png, 20 MB) keeps serving the checklist uploads. Limits per row
come from the requirement (`max_size_mb`, `accepted_formats`) and may only **tighten** the category policy.

---

## 8. Admin: configuring an opportunity (PROPOSED)

Applies to **both** the Programme drawer and the Scholarship drawer, through one shared section component so
the two never diverge.

### 8.1 "Application" section

1. **Required documents** — an editor listing rows from the document-type dictionary. Per row: type, label
   override, mandatory/optional, **applies when** (always / under 18 / in China / studied in China / levels),
   max count, accepted formats, max size, validity months, minimum words, instructions, sort order,
   and (for `APPLICATION_FORM_SIGNED`) the **template upload**.
2. **Application Form Template** — upload (pdf/doc/docx) with a visible current file name, version and
   *replace* action. Replacing keeps the previous asset for applications already in progress.
3. **Nadoumi application fee** — amount and currency per programme/scholarship (RMB entry with the live USD hint),
   stored as `nadoumi_application_fee_amount/currency` on `nad_program` and `nad_scholarship` (added in V111). An
   empty value falls back to the global default (§7.6), so admins only enter exceptions. This is the amount charged
   at Step 4. The existing university-side *application fee* and the *Nadoumi service fee* on scholarships stay
   informational (shown as "upfront fees"); the service fee remains a later milestone (§10.7). The existing
   scholarship fee line of kind `NADOUMI_APPLICATION` is migrated into the new column so nothing is entered twice.
4. **Presets** — "Apply defaults for Bachelor / Master / PhD / Chinese language" to pre-fill the editor from the
   research baseline (§2); admins can edit freely. Baseline presets: *Bachelor* — photo, passport page,
   diploma, transcripts, physical exam (6 months), non-criminal record (6 months), English certificate,
   bank statement, signed application form, guarantee letter (under 18), residence permit (in China),
   study certificate (studied in China); *Master/PhD* add study plan (≥ 800 words), two recommendation letters,
   CV, and (PhD) thesis/research proposal; *Chinese language* adds the self-introduction video.

### 8.2 Payment settings screen

QR image upload (preview), payee label, instructions, the global Nadoumi application fee, the admin roles that
receive the submit email, and the company block used on both PDFs.

### 8.3 Staff workbench additions

* **Payments** queue (finance/ops): proof image, amount, reference, student transaction number, application
  link; actions **Confirm** and **Reject (reason required)**. Permissions `nad:payment:list/verify`.
* Application detail gains a **Payment** card and **Nadoumi Application Form** (download latest).
* The existing DOCUMENT_COLLECTION stage's document verification keeps working on the same `nad_document` rows.

---

## 9. Backend design (PROPOSED)

Module placement follows the modular monolith rules (`architecture.md`): cross-module calls go through service
interfaces, never mappers.

| Concern | Module | Notes |
| --- | --- | --- |
| Apply orchestration (draft, step state, form) | `nadoumi-application` | new `ApplicationFormService`; resolves the checklist through the SPI below |
| Profile completeness for applying | `nadoumi-applicant` | pure `ApplicationProfileRules` (mirrors `OnboardingRules`), new family/supporter services |
| Requirement CRUD and condition evaluation | `nadoumi-document` | `DocumentRequirementService` gains write side; pure `RequirementConditions` |
| Payments | new `nadoumi-payment` (Payment is a listed domain, `CLAUDE.md` §7; the platform plan's Step 9) | owns `nad_application_payment*`; implements `PaymentGuardProvider` |
| PDF generation | `nadoumi-application` (document-rendering sub-package) | interface `ApplicationFormRenderer` |
| Mail with attachment | `nadoumi-identity` mail package | extend `EmailMessage`/sender |
| Notifications | `nadoumi-notification` | new event handlers |

### 9.0 Dependency direction (no cycles)

`nadoumi-application` owns the **provider SPI** (it already owns `DocumentGuardProvider` and
`PaymentGuardProvider`). Other modules **implement** it; the application module **never** calls them directly:

```
nadoumi-document  ──implements──►  nadoumi-application  (DocumentGuardProvider, RequirementProvider)
nadoumi-payment   ──implements──►  nadoumi-application  (PaymentGuardProvider)
nadoumi-payment   ──depends on──►  nadoumi-finance      (RevenueService, system-actor method)
nadoumi-finance, nadoumi-application  never depend back on nadoumi-payment
nadoumi-notification  depends on none of them: consumers read the outbox payload only
```

* A new SPI `RequirementProvider` (in `nadoumi-application`) returns the resolved checklist for an opportunity;
  `nadoumi-document` implements it from `DocumentRequirementService`. This avoids the cycle that a direct
  application → document call would create (the document module depends only on identity today).
* `nadoumi-finance` gains a **system-actor, idempotent** method (`RevenueService.recordSystemRevenue(...)`, unique on
  `(related_type, related_id, reference)`) because the existing `RevenueService.create` takes the staff DTO.
* Each outbox consumer names its owning module: rendering and the two emails run in `nadoumi-application`
  (document-rendering package); payment events are produced by `nadoumi-payment`; the notification module only
  maps events to channels.
* Cross-module calls go through service interfaces, never mappers or entities (`architecture.md`).

### 9.1 Endpoints

Student (`/api/student/...`, all authorization-checked against applicant grants):

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/applications/apply-context?programmeId=&scholarshipId=` | opportunity summary, intakes, templates, eligibility hints |
| POST | `/applications` | start a draft (exists as `POST /api/public/applications`; reused) |
| GET/PUT | `/applications/{id}/form` | study plan, consulate, affirmation; returns `stepStatus` |
| PUT | `/applications/{id}/applicant` | change the selected student (draft only) |
| GET | `/applications/{id}/missing-profile` | fields still missing for the form |
| GET | `/applications/{id}/requirements` | resolved checklist for this student with per-row status |
| POST | `/applications/{id}/signature` | signature image upload |
| GET | `/applications/{id}/templates/{requirementId}` | template download (authenticated) |
| GET | `/applications/{id}/payment` | current payment + QR + instructions + reference |
| POST | `/applications/{id}/payment/proof` | multipart: transaction number + screenshot → `PROOF_SUBMITTED` |
| GET | `/applications/{id}/form.pdf` | **403 unless payment CONFIRMED**; streams the stored Application Form PDF (404 while it is still being prepared), audited |
| GET | `/applications/{id}/payment/receipt.pdf` | **403 unless payment CONFIRMED**; streams the receipt PDF, audited |
| POST | `/applications/{id}/payment/receipt/resend` | re-emails the receipt to the student (rate-limited) |
| POST | `/applications/{id}/submit` | exists; gains the new guards |
| GET/PUT/DELETE | `/applicants/{id}/family-members…`, `/financial-supporter` | profile children |

Staff (`/api/staff/...`): payments list/detail/confirm/reject; requirement CRUD under programmes and
scholarships; template upload; payment settings; application PDF download.

Every new endpoint carries an explicit `@PreAuthorize` and a controller/authorization test (`security.md`).
Student DTOs never include staff-only data (payment verifier identity, rejection internals beyond the
student-safe reason).

### 9.2 Guards and workflow

`submit` (DRAFT → SUBMITTED) is guarded today by `FIELD_SET:program_id` and `FIELD_SET:intake_id`, plus
`FIELD_SET:scholarship_id` for `PROGRAM_WITH_SCHOLARSHIP`. The new guard is written as a **new definition version**
(`V116__nad_wf_program_only_v2_seed` and the `PROGRAM_WITH_SCHOLARSHIP` sibling), which copies the stages,
transitions and task templates and keeps all existing `FIELD_SET` predicates:

```
submit guard (v2):  all existing FIELD_SET predicates of v1,
                    FORM_COMPLETE,
                    ALL_REQUIRED_DOCUMENTS_ATTACHED,
                    PAYMENT_SETTLED:APPLICATION_FEE
```

What each predicate needs in code (architecture review):

* **`FORM_COMPLETE` is not free.** `GuardEvaluator.evaluateOne` has no such case, so an unknown predicate would hit the
  `default` branch and throw at the first submit, and `unbackedPredicates` ignores unknown names, so `activate` would
  not catch it. The change ships: a `FormGuardProvider` SPI in `nadoumi-application`, the new `evaluateOne` case,
  the predicate added to the unbacked check, **and unknown predicate names rejected at activation** (fail-closed, DA3).
* `ALL_REQUIRED_DOCUMENTS_ATTACHED` and `PAYMENT_SETTLED` become available when `nadoumi-document` and
  `nadoumi-payment` provide their beans (§9.0). `ALL_REQUIRED_DOCUMENTS_VERIFIED` stays unused at submit.
* The v2 definition is **activated in the same release** as the provider beans (`WorkflowDefinitionService.activate`
  refuses otherwise, correctly). Activation retires v1 for new applications.
* **In-flight applications keep their pinned v1 definition and therefore bypass the new fee/form/document gate.**
  That is intended (they were created under the old rules); staff handle them with the existing decisions.
* The guard JSON stays AND-only; OR is modelled as two transitions.
* The later `package_ready` guard keeps `FEE_SETTLED` as a recorded decision until the payment module records it
  (**D13**, recommended: confirming the fee writes `FEE_SETTLED` as a system decision with the payment id as
  evidence).

### 9.3 Unlock rules (server-side, tested)

| Action | Requires |
| --- | --- |
| Download template | authenticated, grant on the applicant |
| Upload a document | draft or DOCUMENT_COLLECTION stage |
| Submit payment proof | draft; amount snapshot exists |
| **Download Nadoumi Application Form / receipt** | payment `CONFIRMED` |
| Edit details (Steps 1–2) | draft **and** payment not yet `CONFIRMED`; afterwards only an admin *reopen details* |
| **Submit** | payment `CONFIRMED`, form complete, mandatory documents attached |
| Re-render the Application Form | only after an admin *reopen details*; adds a version, never overwrites; locked after submit |

### 9.4 Payment state machine

```
AWAITING_PAYMENT ──proof──► PROOF_SUBMITTED ──confirm──► CONFIRMED ─(terminal for the fee)
        │                          │ └──reject(reason)──► REJECTED ──new attempt──► AWAITING_PAYMENT (new row)
        └──────────cancel──────────┘
```

Confirm/Reject only by staff with `nad:payment:verify`; each transition writes a payment event and an outbox
event (`ApplicationPaymentProofSubmitted`, `ApplicationPaymentConfirmed`, `ApplicationPaymentRejected`).
Optimistic locking prevents a double confirm. Refunds are out of scope (non-refundable by policy, §2.2).

**Unlock rule — DECIDED: staff confirmation (Option A).** The Application Form and the receipt unlock only after
a staff member with `nad:payment:verify` confirms the payment. Faster alternatives (unlock on proof, or submit
during `PROOF_SUBMITTED`) were rejected because they weaken "no payment, no submit". Mitigation: a finance-queue
notification and a working-hours SLA shown to the student.

---

## 10. Generated documents: Application Form and Receipt (PROPOSED)

Two branded PDFs share one renderer, one stylesheet, one font set and one storage table (§7.5), so they look like
one family and exist in code once.

### 10.1 Application Form — content

Every item of the `.doc` (§2.4), in the same order, with bilingual labels (English + Chinese), plus a header so the
administrators can review it without opening the portal.

1. **Header band** (page 1): Nadoumi logo, title *Application Form 申请表*, **application reference**
   (`NAC-APP-YYYY-NNNNNN`), generated date/time (UTC+8), the **opportunity**, intake, applicant photo, and a
   **PAID** mark with the payment reference.
   * **Programme application → the university name is shown** (decided).
   * **Scholarship application → the university name is not shown**; only the scholarship title and reference
     appear (decided; keeps the partner relationship confidential per `CLAUDE.md` §8).
   * **Programme *with* scholarship** (`PROGRAM_WITH_SCHOLARSHIP`): the application names a scholarship, so the
     **stricter rule applies — the university is not shown** (the partner link could otherwise be inferred).
     *(Design assumption by the reviewers' recommendation; confirm in §16.2, D19.)*
   There is one PDF for both audiences (the student downloads it and the admins receive it), so this rule applies to
   the single file. The rule is enforced in one place: a dedicated `ApplicationFormModel` / `ReceiptModel` that
   **has no university field** for scholarship types, so a template cannot print what the model does not carry. For scholarship types the model is built from the
**student-safe scholarship DTO** produced by the scholarship service; it never touches the staff/internal DTO. The
   same model supplies the **file name, PDF metadata (title/subject), the emailed subject and the receipt "For"
   line**; the preferred-universities free text is the student's own words and is printed as typed.
2. **Sections 1–6** exactly as the Word form: personal info; education background table; work experience table (gap
   notice if the period does not reach today); family members table; financial supporter; emergency contact in China;
   study plan (degree, majors, teaching language, period, preferred universities/city, student category,
   scholarship reason).
3. **Documents checklist**: each required row with *Provided / Not provided / Not applicable* and the file name,
   mirroring the Word form's "attach the application docs" list; the third-language notarisation note is printed.
4. **Applicant affirmation**: the six numbered statements in English and Chinese, the signature image, the date,
   and "no signature, no valid application".
5. **Verification block** (optional, slice A6): short SHA-256 code and a QR to a staff-only verification page.

### 10.2 Visual design (both PDFs)

| Element | Specification |
| --- | --- |
| Page | A4 portrait, 16 mm side margins, 12-column grid; Noto Sans (Latin) + **Noto Sans SC** (CJK), embedded subsets, OFL licences shipped with the fonts. |
| **Logo** | Header: the Nadoumi logo (the repository asset is a 4500 × 4500 transparent PNG, orange `#F29200` and black) at about 22 mm beside the document title. |
| **Background watermark** | The same logo, centred, about 110 mm wide, **opacity 5–6 %** and slightly desaturated, drawn as the lowest layer on **every page** (a fixed, page-level background). Text, tables and photos always sit above it. Legibility is a hard requirement: body text keeps at least WCAG AA contrast (≥ 4.5:1) against the watermarked background, tables keep a solid white row fill so the mark never shows through data, and the watermark never exceeds the opacity cap. The cap is covered by an automated test (§14). |
| Palette | White page, slate text and neutral hairlines (the site rule, `CLAUDE.md` §24). The Nadoumi orange appears only on the logo, the section number badges and one thin header rule. Amounts and fees are red. |
| Typography | Title 20 pt semibold; section headers 12 pt with an orange number badge; labels 8.5 pt slate-500 small caps; values 10 pt slate-900; tables 9 pt with zebra rows and **repeated header rows** across page breaks; no orphaned headings. |
| Components | Label/value pairs in a two-column grid; the signature in a ruled box at fixed height; photo 35 × 45 mm with a hairline frame; status stamp (PAID) in outline style. |
| **Footer on every page** | A hairline rule, then the full company block, centred in two lines: **Nadoumi Education Consulting · 802, Building 6, JiuZhou BeiJun, FuXi Road No 2, Fucheng District, Mianyang, Sichuan, China** / **team@nadoumiconsulting.com · +86 159 0823 7607 · +86 155 2057 6024 · nadoumi.com**; at the right, `Page n / N` and the document number. All values come from the settings in §7.6, not from code. |
| PDF properties | Title, author *Nadoumi Education Consulting*, subject = document number, tagged PDF, selectable text, embedded fonts, PDF/A-friendly. |

### 10.3 Receipt — content and design

Issued automatically when a payment becomes `CONFIRMED`; one page A4 (or A5 landscape variant, **DECISION
REQUIRED**: A4 recommended), same header, watermark and footer as the form.

| Block | Content |
| --- | --- |
| Title | *Payment Receipt 付款收据* with the receipt number `NAD-RCT-YYYY-NNNNNN` and issue date/time (UTC+8) |
| Paid by | student full name, applicant public id, email; when someone paid on the student's behalf, the account holder's name is added |
| For | *Nadoumi application fee* for the application reference, with the programme or scholarship title (university name follows the same rule as §10.1) and intake |
| Payment | method *QR payment*, payment reference, the student's transaction number, confirmed date, confirmed-by label (a role, not a personal email) |
| Amount | a clear amount panel: **¥ amount** in red with the USD equivalent and the rate date; status **PAID** |
| Notes | the non-refundable notice, and the statement that the receipt is system-generated and valid without a signature |
| Authenticity | receipt number plus a short SHA-256 code; optional verification QR (slice A6) |
| Footer | the same full company block as §10.2 |

The receipt is the student's proof of payment; it also writes the **finance ledger entry**: confirming a payment
creates a `nad_revenue` row (source *Application fee*, `related_type = APPLICATION`, `related_id`, reference =
payment reference) through `RevenueService`, so bookkeeping needs no second entry. The existing `ReceiptWriter`
(an HTML print page for internal expenses) is **not** reused or extended; it keeps serving expenses only.

### 10.4 Generation approach and timing

**Renderer — DECISION REQUIRED (recommended): OpenHTMLtoPDF (+ PDFBox)** over HTML/CSS templates.

| Option | Pros | Cons |
| --- | --- | --- |
| **OpenHTMLtoPDF** — *recommended* | HTML/CSS design iteration, embedded fonts, page-break CSS, table header repetition, fixed page background (the watermark) and running footers, pure Java, no external process, Apache-2.0 | CJK fonts must be embedded; CSS paged-media subset only |
| Programmatic PDFBox / OpenPDF | full control, tiny dependency | laborious layout, hard to keep beautiful |
| Headless Chrome (Playwright) | best CSS fidelity | heavy extra runtime and process in production |
| JasperReports | report designer | heavier tooling for two documents |

**Renderer hardening (mandatory, security review).** Student text (names, study plan, supporter, affirmation) is
untrusted input to an HTML renderer.

* The DOM is built with **escaped values only**; no raw HTML or Markdown from a student ever reaches the template.
* The renderer is configured with a **deny-all resource resolver**: only the classpath logo, fonts and stylesheet
  load (`FSEntityResolver`/`UserAgentCallback` that refuse `http(s):`, `file:`, `ftp:`, `jar:` and relative paths
  outside the classpath). No `@import`, `<link>`, `url()` or external fonts. XML external entities and DTDs are
  disabled in the parser.
* The signature and the photo are **re-encoded from bytes** and embedded as `data:` URIs.
* A **render timeout** and a **page cap** apply; failures log a code, never the template or student data.
* Fonts, logo and stylesheet are pinned by hash and verified at startup.
* Test: a payload containing `<img src="http://127.0.0.1:…">` and `@import url(...)` produces **no outbound call**
  and no file read.

Templates live in `nadoumi-application/src/main/resources/templates/pdf/` (`application-form.html`,
`receipt.html`, shared `base.css`, `fonts/`, `logo.png`). The Word file is a **content reference only**; the layout
is rebuilt, not converted. The renderer sits behind `PdfRenderer`, a narrow interface, so the engine can change.

**Timing — DECIDED: render when the payment is confirmed.**

| Moment | Action |
| --- | --- |
| Payment **CONFIRMED** (one transaction) | write the `FORM` snapshot, assign document numbers, enqueue `ApplicationPaymentConfirmed` |
| Outbox consumer (retryable; idempotent by `(application, kind, version)`; **claim-then-send** so a crash between SMTP and `emailed_at` cannot send twice) | render the Application Form PDF and the Receipt PDF, hash, store as SENSITIVE media, write `nad_generated_document` rows |
| Same consumer | **email the receipt to the student** (§10.5), update `emailed_at` |
| Student | downloads both from the Payment card |
| **Submit** | no re-render; the admins' email attaches the stored Application Form PDF (plus the existing student submission confirmation) |

The render always reads the frozen `FORM` snapshot **version**, never live profile data. A rendering or delivery failure never blocks or rolls back the confirmation: the outbox retries with back-off, a
terminal failure raises an in-app alert for admins, and the student sees "Preparing your documents…" until both
files exist. Because the form is frozen at confirmation (§5 Step 4), the PDF the student downloaded is exactly what
the administrators receive.

### 10.5 Email delivery

The mail sender gains attachments: `EmailMessage` becomes `(to, cc, subject, body, htmlBody, attachments)` with a
size cap. To avoid breaking every `new EmailMessage(to, subject, body, htmlBody)` call site, the 4-argument
constructor and the `text(...)` factory stay (delegating with empty `cc` and `attachments`); the affected call
sites are listed in the A5 slice. The sender refuses to send an attachment over the size cap. Delivery is two separate emails to two separate audiences.

**A. Receipt → the student (automatic, at confirmation)**

* To: the applicant's verified email; if absent or unverified, the paying account's email. When the application is
  made on someone's behalf, the account holder is added in cc (**DECISION REQUIRED**).
* Subject: `Your Nadoumi payment receipt NAD-RCT-… — application NAC-APP-…`.
* Body: HTML + text in the Nadoumi style (logo header, amount panel, next step: "complete your documents and
  submit"), link to the application; the receipt PDF attached.
* Idempotent: a retried job never sends twice (`emailed_at` guard); the student can re-send from the dashboard
  (rate-limited).

**B. Application Form → the Nadoumi administrators (at submit, admins only)**

* Recipients: **active users holding a role in `nadoumi.application.notify-roles`** (default
  `nadoumi_super_admin`). **Staff roles (case officers, ops, content, support, …) receive no email.** Recipients are
  resolved from roles at send time, never from user input or a fixed shared mailbox, and each admin gets their own
  message (no exposed recipient list).
* Subject: `[Nadoumi] New application NAC-APP-… — <student name> — <programme or scholarship>`.
* Body: summary table (student, nationality, level, opportunity, intake, payment reference, documents provided
  count) and a link to the application in the admin console; the stored Application Form PDF attached as
  `NADOUMI-APPLICATION-<ref>.pdf`.
* Implemented as a new notification type `APPLICATION_SUBMITTED_ADMIN` (email channel, targeted by role). It is
  separate from the existing `APPLICATION_SUBMITTED` (confirmation to the student's linked users) and from
  `APPLICATION_RECEIVED` (in-app, staff who work the queue), neither of which changes.

Mail hardening: **CR/LF and control characters are stripped** from the subject, display name and every header
field (the student name and programme title go into the subject); `From` and `Reply-To` are fixed by
configuration, never user-controlled; the sender **requires STARTTLS and fails closed**; a size cap applies; the
attachment is never logged and the PDF is never mentioned in the subject or preview. Only roles on a **server-side
allow-list** can be configured in `notify-roles`; each recipient must be active with a verified email **at send
time**; `emailed_to` is stored per recipient; the idempotency key is `(application, recipient)`. The receipt goes
to a **verified** email only. Where possible the email also carries an expiring link to the secure download.

Privacy note: the PDF contains personal data and travels by email by explicit product decision. Use TLS to the
SMTP relay, send only to resolved admin accounts, keep the secure admin download as the canonical copy.

### 10.6 Delivery checks

* Both PDFs open with selectable text, embedded fonts and correct CJK glyphs.
* The watermark never lowers text contrast below AA and never shows through a table row.
* Footer shows the full address, email and both phone numbers on every page.
* Document numbers are unique and immutable; a re-render adds a version.

### 10.7 Payment evolution seam

`PaymentService` depends on a `PaymentMethod` strategy (`QrManualPaymentMethod` now). A later gateway (card,
Alipay, WeChat Pay) adds a method that produces `CONFIRMED` through a verified webhook, with no change to guards,
unlock rules, rendering or receipts. The later **Nadoumi service fee** fits the same table with
`kind = SERVICE_FEE` and a workflow guard at the pre-admission/JW202 stage, and gets its own receipt.

---

## 11. Frontend design (PROPOSED)

Follows `CLAUDE.md` §24 — shared components, white canvas, brand colour only on buttons/icons, prices red.

### 11.1 Routes

| Route | Purpose |
| --- | --- |
| `/dashboard/apply` | entry; creates or resumes the draft, redirects to the wizard |
| `/dashboard/applications/[id]/apply` | the wizard (steps 1–5) |
| `/dashboard/applications/[id]` | existing detail; gains Payment and Form cards |

Apply buttons on programme/scholarship detail pages link to `/dashboard/apply?…` (replacing today's
register/dashboard link).

### 11.2 Components

Reuse: `SidebarFrame`/`SidebarItem` (wizard stepper), `OnboardingProgress` pattern (mobile segments), `FilePicker`,
`DetailCard`, `PriceRows`, `DetailFacts`, `NAlert`, `NButton`, `FormTextField` family, `AsyncState`.

New (each used by more than one place or one-per-step):

* `ApplyWizardShell`, `ApplyStepper` (generalises `OnboardingStepper`; **extract the shared step-list logic
  rather than copying it**),
* `StudentPicker` (select/add student), `TemplateDownloadList`,
* `ProfileGapForm` (the missing-fields panel; reuses the onboarding section components),
* `FamilyMembersEditor`, `FinancialSupporterForm`, `StudyPlanForm`, `SignaturePad` (draw or upload),
* `RequirementChecklist` (rows with `DocumentUploadRow`), `PaymentPanel` (QR, reference, proof upload, status),
* `ApplicationFormDownloadCard` (locked/unlocked states with reason text).

### 11.3 Behaviour

* Fixed shell, autosave per field group, **Save and continue** commits, server-derived resume (reuse
  `resumeStepIndex`'s approach with application step status).
* Locked actions show *why* ("Waiting for payment confirmation"), never just a disabled button.
* All strings in the five locales (`en` default, then `fr`, `zh`, `ar`, `es`); RTL-safe.
* Mobile: single column, bottom action bar, QR full width, signature pad touch-optimised.
* Accessibility: labelled controls, focus management between steps, `aria-live` for payment status changes.

### 11.4 Admin UI

Shared `OpportunityApplicationSection` inside `ProgramDrawer` and `ScholarshipDrawer`; **Payments** queue screen;
**Payment settings** screen; application detail cards. English and Chinese only (admin rule).

---

## 12. Security and privacy

* Authorization on every endpoint through `@na.canAccessApplication / canAccessApplicant`; on-behalf-of grants
  are honoured and recorded; staff-only transitions reject student callers.
* Documents, signature, payment proof, PDF: **PROTECTED/SENSITIVE**, streamed by the backend, never exposed as
  provider URLs; every read writes the media access log; `Cache-Control: no-store`.
* Upload validation: magic-byte sniffing, allow-lists per category, size caps, no HTML/SVG/archives
  (`MediaValidation`), video limited to MP4.
* Payment: server computes the amount from the opportunity at draft time and stores the snapshot; the client
  cannot send an amount. Proof screenshots are evidence, not trusted input. Reference collisions impossible
  (unique). Confirm/reject require the permission and are audited.
* Rate limits on proof submission and template downloads; idempotent submit (optimistic `version`).
* PII minimisation in notifications; the email attachment is the one deliberate exception (§10.5).
* Data protection: erasure paths extended to the new tables; retention for submitted forms is a business
  decision (**DECISION REQUIRED**).
* Scholarship confidentiality still holds: student-facing apply context never carries the partner-university
  relationship, and the PDF follows the decided rule — **programme applications show the university, scholarship
  applications do not** (§10.1). The staff console remains the place where staff see the full relationship.
* Receipt and form PDFs, signature, payment proof and uploaded documents are SENSITIVE: streamed by the backend,
  `no-store`, access-logged. The admin-only email is the one deliberate exception for the form (§10.5).
* Admin-only recipients are resolved server-side from role membership; changing `notify-roles` requires the
  system-configuration permission and is audited.

---

### 12.1 Required safeguards (from the security review, 2026-10-22)

| # | Safeguard | Where it is specified |
| --- | --- | --- |
| S1 | PDF rendering is injection- and SSRF-proof (escaped DOM, deny-all resolver, no XXE, data-URI images, timeout/page cap) | §10.4 |
| S2 | **Read matrix** per asset (below); media ids and provider URLs never appear in DTOs; generated/sensitive assets are unreachable through the generic media route | §7.5, below |
| S3 | The unlock rule (payment `CONFIRMED` + grant) is enforced **at the media guard**, not only at the endpoint | §7.5 |
| S4 | Confirm is a single atomic conditional update; unique keys on payment/receipt/ledger; side effects in the same transaction | §7.4, §7.5 |
| S5 | Amount snapshot at creation; cancel-and-recreate when opportunity/intake/applicant changes; one open payment per application; one transfer per application; separation of duties | §7.4 |
| S6 | Admin recipients: server-side role allow-list, active + verified at send time, per-recipient idempotency, header-injection stripping, no user-controlled From/Reply-To | §10.5 |
| S7 | Email carries PII by decision: TLS required (fail closed), size cap, no PII in subject/preview, link preferred, attachment not logged | §10.5 |
| S8 | New media categories validated for real: MP4 brand sniff, streamed size check, `nosniff`, `attachment`, `no-store`; templates limited to pdf/docx | §7.7 |
| S9 | Changing the payment QR needs its own permission, is audited (old/new) and notifies admins | §7.7 |
| S10 | Document numbers incremented in the same transaction as the insert, never `max()+1`, unique index, tested under concurrency | §7.5 |
| S11 | Outbox consumers: claim-then-send, idempotency key includes the version, dead-letter without PII, replay renders the snapshot version | §10.4 |
| S12 | Confidentiality of scholarship applications covers **every** channel: header, file name, metadata, email subject, receipt line, verification page; one model with no university field | §10.1 |
| S13 | Field-level access: religion, passport data, family and supporter data are excluded from broad staff lists; `affirmed_ip` has a retention and erasure rule | §7.1, §7.3 |
| S14 | Erasure: a per-module participant covers form, payments, generated documents, signature, proofs and uploads; financial rows are **anonymised, not deleted** (legal retention, D12); provider assets hard-deleted; outbox payloads and mail logs scrubbed. Without it the new `RESTRICT` foreign keys would make erasure fail and orphan media | §7, D12 |
| S15 | Rate limits also on `form.pdf` / `receipt.pdf` downloads and on payment-reference guessing | §9.1 |
| S16 | Errors: rendering failures show "Preparing your documents…" only; admin alerts carry no PII; every PDF response is explicitly `application/pdf` + `no-store` | §10.4 |

**Read matrix**

| Asset | Student who owns the application | On-behalf-of grant holder | Assigned staff | Finance (`nad:payment:verify`) | Administrators |
| --- | --- | --- | --- | --- | --- |
| Application Form PDF | yes, after confirmation | yes, after confirmation | yes | no | yes |
| Receipt PDF | yes, after confirmation | yes, after confirmation | no | yes | yes |
| Payment proof (bank screenshot) | uploader only | no | no | **yes** | yes |
| Signature image | owner | no | yes (case work) | no | yes |
| Uploaded documents | owner | grant holder | yes | no | yes |
| Template | any authenticated student with a grant | same | yes | no | yes |

Every read writes the media access log; none of these routes returns a media id or a provider URL.

---

## 13. Notifications

| Event | Recipient | Channel |
| --- | --- | --- |
| Payment proof submitted | finance/ops queue (staff with `nad:payment:verify`) | in-app |
| Payment confirmed | student | in-app |
| **Payment receipt** | **student (applicant email)** | **email with the receipt PDF, automatic** |
| Payment rejected (reason) | student | in-app + email |
| **Application submitted (new)** | **Nadoumi administrators only** (role-resolved) | **email with the Application Form PDF** |
| Application submitted (existing) | applicant's linked users | email confirmation, unchanged |
| Application received (existing) | staff working the queue | **in-app only**, unchanged |
| Rendering/delivery failed | administrators | in-app alert |

All through the outbox so they survive restarts (`DOMAIN_EVENTS.md` pattern).

---

## 14. Testing plan

Required by `testing.md`; real MySQL for non-trivial SQL.

* **Unit**: `ApplicationProfileRules`, `NadoumiApplicationFee` resolution (opportunity value → global default), `RequirementConditions` (under 18 / in China / studied in China /
  levels), payment state machine, fee snapshot, `FORM_COMPLETE`, `ApplicationFormModel` mapping, PDF model
  completeness (every field of §2.4 present).
* **Service**: gating matrix of §9.3, on-behalf-of access, new-student path, template resolution order,
  requirement migration copy.
* **Controller/API (MockMvc)**: 401/403 not 500; DTO shape (no staff fields); PDF 403 before confirmation;
  confirm/reject permissions; double-confirm race.
* **Authorization**: a user without a grant cannot read or mutate another applicant's application, payment,
  signature or PDF (`security.md` required tests).
* **Integration**: confirm payment end-to-end with Testcontainers MySQL → both PDFs stored → receipt mail to the student captured with
  attachment → submit → admin-only mail captured with the form attached; Flyway count test updated per migration.
* **Recipients**: the admin email goes only to active holders of the configured roles; a staff-role user, a disabled
  admin and a student never receive it; each recipient gets a separate message; the receipt email goes to the
  applicant and is sent once.
* **Receipt**: number uniqueness under concurrency, immutability, ledger entry created exactly once per confirmation,
  "Duplicate copy" reprint keeps the number.
* **University rule**: programme PDF contains the university name; scholarship PDF does not (asserted on extracted text).
* **PDF**: render with sample data; assert page count bounds, embedded fonts (incl. CJK glyph coverage), text
  extractable, metadata; golden-image check of page 1 at low resolution to catch layout regressions; **watermark legibility** (opacity cap,
  AA contrast of text over the watermark, solid table rows) and footer content on every page.
* **Frontend (Vitest)**: step gating, autosave/resume, locked states and reasons, checklist conditions,
  payment panel states, i18n key parity, shared-component usage, no brand-coloured text/chips, red prices.
* **E2E** (Playwright, one critical flow): apply → select student → fill → upload → pay (staff confirm stub) →
  download → submit.

---

## 15. Delivery plan

Incremental, each slice shippable and tested, in this order (each slice updates the docs in the same change):

| Slice | Content | Migration |
| --- | --- | --- |
| **A1** Profile fields | onboarding additions, `ApplicationProfileRules`, family members, financial supporter, emergency-contact detail, erasure | V110 |
| **A2** Requirements and templates | converge requirement tables, conditions, formats/validity, template upload, programme + scholarship admin section, new document types, media categories, `RequirementProvider` + `DocumentGuardProvider` beans | V111, V112 |
| **A3** Apply wizard (steps 1–3) | apply context, student picker + add student, template download, details form, signature, checklist upload, autosave/resume | V113 |
| **A4** Payment | payment module, QR settings, **Nadoumi application fee** setting and fields, proof upload, staff queue, system-actor ledger method, `PaymentGuardProvider`, **activation of definition v2 in the same release** (all three providers now exist) | V114, V116 |
| **A5** Documents + delivery | renderer, fonts, logo/watermark, Application Form template, **receipt template**, render-on-confirmation outbox consumer, mail attachments, **receipt email to the student**, **admin-only submit email**, student downloads; lists and updates every `EmailMessage` call site | V115 |
| **A6** Hardening | verification QR (optional), digest emails, metrics, accessibility pass, load check of PDF rendering | — |

Each slice ends with: build green, tests listed in §14 for that slice, docs updated, no new random colours or
duplicated components.

### Acceptance criteria (end state)

1. From a programme or scholarship page, **Apply** opens the wizard; the student picks (or adds) a student and
   downloads the admin-uploaded template.
2. An existing student's known facts are pre-filled; missing ones are requested once and saved to the profile.
3. The document checklist exactly matches what the admin configured, including conditional rows and the signed
   template.
4. Without a confirmed **Nadoumi application fee** payment the student can neither submit nor download the form —
   enforced by the server and explained in the UI.
5. On confirmation both PDFs are rendered; the Application Form contains every field of `NADOUMI APPLICATION
   FORM.doc`; both carry the logo, the legible background watermark and the full address, email and phone numbers
   in the footer of every page.
6. The student receives the receipt by email automatically; on submit only the Nadoumi administrators receive the
   email with the Application Form PDF; staff receive no email.
7. Closing the browser at any step and returning resumes at the right step.
8. All new endpoints have authorization tests; the Flyway test counts the new migrations.

---

## 16. Decisions

### 16.1 Decided by the product owner (2026-10-22)

| # | Decision |
| --- | --- |
| D1 | Unlock only after **staff confirm** the QR payment (§9.4). |
| D2 | The fee charged is the **Nadoumi application fee** (§8.1), not the university fee and not the service fee. |
| D6 | The PDFs are **rendered after the payment is confirmed**, not at submit (§10.4). |
| D6b | On submit the Application Form PDF is emailed to the **Nadoumi administrators only**, not to staff (§10.5). |
| D7 | Receipt generated automatically at confirmation, branded, **emailed to the student automatically** (§10.3, §10.5). |
| D9 | **Programme application → university name shown; scholarship application → not shown** (§10.1). |

### 16.2 Still required

| # | Decision | Recommendation |
| --- | --- | --- |
| D3 | Converge the two requirement tables (§7.2) | yes, onto `nad_document_requirement` |
| D4 | Template resolution order (§7.2) | scholarship → programme → university → global |
| D5 | PDF engine (§10.4); a new major dependency needs a recorded decision (`CLAUDE.md` §2) in `ARCHITECTURE.md` when approved | OpenHTMLtoPDF over HTML/CSS |
| D8 | Verification QR on both PDFs (§10.1, §10.3) | yes, staff-only verification page, slice A6 |
| D10 | Required-at-onboarding set (§6.3) | marital status + permanent address required; religion, place of birth optional |
| D11 | Dedicated payment-settings screen vs generic config (§7.6) | dedicated screen |
| D12 | Retention period for submitted forms, receipts and proofs (§12) | business/legal to specify |
| D13 | Does confirming the fee write the `FEE_SETTLED` decision (§9.2) | yes, as a system decision |
| D14 | Place of birth optional at onboarding, required by the form (§6.3) | confirm |
| D15 | Which roles count as "admin" for the submit email (§7.6) | `nadoumi_super_admin` only, configurable |
| D16 | Receipt cc to the paying account on behalf-of applications (§10.5) | yes |
| D17 | Receipt paper size (§10.3) | A4 |
| D18 | Admin *reopen details* after confirmation (§5 Step 4, §9.3) | admin-only action that supersedes the PDF, reason required |
| D19 | University name for a **programme with scholarship** application (§10.1) | hidden (the stricter rule); assumed in this document |

---

## 17. Inputs still needed from the product owner

* The **payment QR image** and payee details (to be placed under `applicationdocument/` and then uploaded through
  the payment-settings screen; the repository copy is only a seed).
* The **logo** is already in the repository (`nadoumi-web/app/assets/images/logo.jpg`, actually a transparent PNG,
  4500 × 4500). A vector (SVG) version would give the sharpest PDF header and watermark; the PNG is the fallback.
* Confirmation of the **company block** text (address, email, both phone numbers, website) and of which accounts
  are the **administrators** who receive the submit email.
* The **Nadoumi application fee** amounts (a global default and any per-opportunity exceptions).
* The per-programme/per-scholarship **document lists** (admins enter them through the new section; the §8.1 presets
  are a starting baseline only).

---

## 18. References

Sources actually read for §2: ScholarshipChina programme/news pages (a 2026 Autumn Mianyang bachelor listing and
a Hunan Chinese-language listing) and search results of `scholarshipchina.com`; EDUPRCHINA news/programme pages
(a Guangzhou bachelor listing and search results of `eduprchina.com`); and the Nadoumi form
`applicationdocument/NADOUMI APPLICATION FORM.doc`. Repository facts in §4.1 were verified against the modules
`nadoumi-application`, `nadoumi-applicant`, `nadoumi-document`, `nadoumi-finance`, `nadoumi-program`,
`nadoumi-scholarship`, `nadoumi-identity` and the migrations up to V109.
