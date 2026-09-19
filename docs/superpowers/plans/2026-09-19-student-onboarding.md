# Student Onboarding v2: Plan

> Branch `feat/student-onboarding`. Supersedes the 7-step "Revision 3" flow in `docs/APPLICANT_ONBOARDING.md`
> (updated at the end of each slice). Decisions taken with the product owner on 2026-09-19:
> **passport check = in-browser MRZ read behind a swappable reader**, **delivery = four tested slices**.

## Findings (investigation, 2026-09-19)

| Area | Today | Gap |
| --- | --- | --- |
| Completion gate | Client heuristic: six identity fields non-empty (`useOnboarding.ts`); **fails open** if the check errors | No server record of completion; a student with no education, location or photo is "complete" |
| Prefill | `ProfileForm` starts empty; registration keeps names only inside `nick_name` | Applicant is created on first save, so nothing can be prefilled |
| Names | "First name / Last name" on register; stored as typed | Copy must be Given / Family, passport-exact, stored UPPERCASE |
| Date of birth | Any date accepted | 17+ and never in the future, enforced server-side |
| Profile fields | `nad_applicant` has names, dob, nationality, passport no, email, phone | gender, countries, native language, WeChat/WhatsApp, passport dates, email verification, `onboarded_at` |
| Email change | No authenticated OTP flow (purposes: `REGISTER`, `PASSWORD_RESET`) | Verify-new-email flow |
| Photo / passport | Client-only cards, no upload | PROTECTED media upload + MRZ read + comparison |
| Education / Interests / Location / Contact | Education CRUD exists (no wizard form); rest are placeholders | Forms, tables, endpoints |
| Work experience | none | New section incl. China work visa |
| Student dashboard | 60-line `DashboardShell` | Admin-quality shell, welcome celebration |

## Assumptions (flag if wrong)

1. "Contact" is the phone number; "WeChat or WhatsApp" means **at least one** of the two is required.
2. The verified **applicant contact email** may differ from the account (login) email; the login email is unchanged.
3. Gender values: `FEMALE`, `MALE`, `UNSPECIFIED` (passport sex field is M/F/X).
4. Names: letters (any script), spaces, hyphens, apostrophes; stored trimmed and UPPERCASE (`Locale.ROOT`).
5. The 15-second Finish screen is a UX hold; the server completes the onboarding at click, so closing the tab during the hold still leaves the student onboarded (the celebration is tracked separately).
6. Non-English copy (fr, es, zh, ar) is written by me with the same key set (the parity test requires it) and needs native review.

## Slice 1: Foundation and the gate (this slice)

Backend
- **V56** `nad_applicant`: `gender`, `country_of_origin`, `country_of_residence`, `native_language`, `wechat_id`, `whatsapp`, `email_verified_at`, `onboarded_at`.
- Pure rules, unit-tested: `NameRules` (normalise + validate + UPPERCASE), `AgeRules` (not future, >= 17).
- `ApplicantService` applies the rules on create/update (self and staff); new fields on request/response/mapper.
- `StudentRegistered` event (identity) -> applicant listener creates the primary applicant (names split, UPPERCASE, verified email, owner grant) in the registration transaction. Prefill becomes real.
- Applicant email change: `OtpPurpose.APPLICANT_EMAIL`; `POST /api/student/applicants/{id}/email/otp` and `/email/verify`; `PUT` rejects a changed, unverified email.
- `OnboardingService`: `GET /{id}/onboarding` (sections + missing) and `POST /{id}/onboarding/complete` (validates, sets `onboarded_at` once, idempotent). `ApplicantResponse` gains `onboardingComplete`, `emailVerified`.
- Tests: rules (unit), service (mapper mocked), authorization (no grant cannot complete or verify another applicant), Flyway counts 53/52.

Frontend
- Register copy: Given name / Family name + passport notice; i18n in all five locales.
- Profile step: new fields, country and language pickers (`Intl.DisplayNames`), date input `min`/`max` for 17+, email Verify flow.
- `useOnboarding` reads the **server** flag and **fails closed**; middleware keeps `/dashboard/**` behind it.
- Finish calls the complete endpoint.

## Slice 2: Identity documents
Photo upload (crop, PROTECTED media). Passport upload, `PassportReader` SPI (browser MRZ implementation), passport number/issue/expiry, expiry > 6 months from today, server comparison of DOB / given / family name against the profile, mismatch message, edited-value flag for staff. `PassportNumber`/dates columns in V57.

## Slice 3: Sections — DONE (backend and web)
Education wizard form (multiple, high school to current). Interests + preferred level + preferred cities. Location (+ China branch: city, current level, visa expiry, ...). Guardian / emergency contact. Work experience (+ China work visa). Tables and endpoints per `APPLICANT_ONBOARDING.md` §5, adapted.

## Slice 4: Review, finish, dashboard — DONE, with one gap
Step-by-step review with edit-jump (`ReviewStep`/`ReviewSection`), Finish -> 15 s `OnboardingFinishing`
hold -> dashboard, welcome/celebration (`WelcomeCelebration`, once, tracked server-side via
`welcomePending` + `POST /onboarding/welcomed`). The five section components also back their own
`/dashboard/*` editors, so nothing is duplicated between the wizard and the dashboard.

**Not done:** the "student dashboard shell to admin quality" bullet's notification piece — the header
still only has the locale switcher and account menu. `server/api/student-notifications/[...path].ts`
proxies `/api/notifications/*`, but no bell icon, unread-count badge or notifications list page consumes
it yet. Left for a follow-up pass rather than rushed here.

## Cross-cutting
- Every slice: TDD, docs updated (`APPLICANT_ONBOARDING.md`, `DATABASE_DESIGN.md`, `API_DESIGN.md`, `SECURITY.md` where relevant), full verify green, one logical commit each.
- **Migration numbering:** this work takes V56+ first. The application-engine plan on `feat/application-engine` (V56 to V60) must be renumbered when that branch is next touched.
