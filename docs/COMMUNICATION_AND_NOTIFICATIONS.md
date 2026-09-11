# Nadoumi — Communication & Notifications

Status: **BASELINE** · **EXISTING** · **PLANNED** · **OPEN**.

Two separate concerns (CLAUDE.md §12): **Chat** (people ↔ people) vs **Notifications**
(system-generated, multi-channel).

> Reconciled with Rev 3 (D6 approved). Contradiction fixes from `DOMAIN_MODEL.md` §8
> (Communication) folded in.

---

## 1. What exists today (EXISTING)

- **Announcements only:** `sys_notice` + `sys_notice_read` — staff broadcast board,
  one-way, in-app.
- **No** chat / conversations / messages / participants / attachments.
- **No** notification framework — no in-app store, no SMS/push/WhatsApp, no delivery
  tracking. **Revision 2 (nadoumi-web build) adds the first slice of the email
  channel:** `spring-boot-starter-mail`, a provider-agnostic `MailSender` port with
  `smtp` / `log` adapters, and a minimal `${var}` text-template renderer — used only
  for auth OTP / "account exists" mails so far. The full `nad_notification*` model,
  preferences, per-locale templates, retry sweep, and SMS/push/WhatsApp remain
  PLANNED (§4). See spec §15.
- **No** realtime transport (no WebSocket/SSE dep or endpoint).
- Reusable: `AsyncManager` / `AsyncFactory` / `ThreadPoolConfig`; Redis (pub/sub
  available); Quartz (JDBC store to be enabled — Phase 2).

### 1a. Notifications — EXISTING (Step 5, slices 1–3 + 5)

`nadoumi-notification` module. **What is built:**

- **Transactional outbox** — `nad_outbox_event` (`V30`). Producers append via the
  `com.nadoumi.common.outbox.OutboxWriter` SPI **inside their own transaction**
  (`Propagation.MANDATORY`). `OutboxPollerJob` (Quartz `sys_job`, active, 15s —
  `V31`) drains PENDING rows, hands each to `OutboxToNotificationDispatcher`,
  marks `DONE`, or retries with backoff and parks `FAILED` after 10 attempts.
- **Model** — `nad_notification` (+ `source_ref` idempotency key,
  `ON DELETE CASCADE` to `sys_user`) / `nad_notification_delivery`
  (unique `(notification_id, channel)`) / `nad_notification_preference` /
  `nad_notification_template` (`V32`); menu + `nad:notification:*` perms + four
  default `en` templates (`{{var}}` placeholders) (`V33`).
- **`NotificationService.create`** — one row + IN_APP delivery (recorded `SENT`)
  + one PENDING delivery per secondary channel the type declares and the
  preference allows; transactional types ignore the preference. Idempotent on
  `(recipient_user_id, source_ref)`.
- **Channels + dispatch** — `NotificationRenderer` (`{{var}}`, `locale='en'`
  fallback); `EmailNotificationChannel` over the `MailSender` port (`smtp` /
  `log`); `NotificationDeliveryDispatcher` + `notificationDispatchJob` (Quartz,
  active, 30s — `V34`) with per-delivery backoff / dead-letter.
- **Producers wired** — `ContactService` emits `ContactInquiryReceived` (its old
  direct support-inbox mail is removed); `ScholarshipAdminService` emits
  `ScholarshipPublished` on the transition to PUBLISHED.
- **APIs** — `/api/notifications` (the caller's own feed: list, unread-count,
  mark-read, mark-all) for any authenticated principal; `/api/staff/notifications`
  (oversight incl. per-channel delivery status) gated by `nad:notification:*`.

**Deferred to slice 4:** SSE `/api/{student,staff}/stream` + Redis pub/sub
fan-out (§5). Until then the admin/notification bell polls
`/api/notifications/unread-count`. SMS / WhatsApp / Push remain schema-only.

## 2. Requirements

Chat: conversations, participants, messages, attachments, read status. Notifications:
in-app, push, email, SMS, WhatsApp. Realtime designed deliberately.

---

## 3. Chat / Messaging (BASELINE)

### 3.1 Model — (Communication slice DDL, `V10` region)

```
nad_conversation             id, subject?, application_id?  <-- FK LIVES HERE
                             status(OPEN|CLOSED), create_by/time
nad_conversation_participant id, conversation_id, user_id, role(STAFF|APPLICANT|AGENT|GUARDIAN),
                             added_at, removed_at?, last_read_message_id?, muted
nad_message                  id, conversation_id, sender_user_id, body, created_at,
                             edited_at?, deleted_at?      (AUTHORED messages only)
nad_message_attachment       id, message_id, promoted_document_id?, storage_key,
                             content_type, size_bytes
```

Reconciled rules:
- **FK direction (fix G-A1/G-C1):** `nad_conversation.application_id` holds the link.
  One application → **many** conversations (e.g. one per university, one for
  logistics). `nad_application` stores **no** `conversation_id`.
- **No SYSTEM message rows (fix G-C2).** Workflow/timeline events stay in
  `nad_application_event`; the UI merges authored `nad_message` rows and events
  chronologically. `nad_message` is human-authored only.
- **Attachments are lightweight blobs (fix G-C4)** — own `storage_key`, **not** a
  governed `nad_document` — unless staff "promote to document"
  (`promoted_document_id`; see `DOCUMENT_MANAGEMENT.md` §3.5).
- **Read status:** per participant (`last_read_message_id`); unread =
  `id > last_read_message_id`.
- **Confidential content rule:** chat messages are visible to *all* participants —
  internal-only content is a `nad_application_note` `visibility='INTERNAL'`, never a
  message.
- Access: an application-bound conversation inherits authorization from the
  application (external: `nad_user_applicant_access` `MESSAGE_STAFF`; staff:
  `nad:conversation:participate` + assignment). Participant management is staff-only
  (`nad:conversation:participant:manage`).

### 3.2 Endpoints (PLANNED)

`/api/student/conversations/**` and `/api/staff/conversations/**` — list, open, post,
mark-read, upload attachment, (staff) add/remove participant, close, moderate.

### 3.3 Abuse / safety

`@RateLimiter` on post; body size limit; XSS-escape on render; audit participant
changes; soft-delete messages (`deleted_at`, history kept).

---

## 4. Notifications (BASELINE)

### 4.1 Model — (Notification slice DDL, `V11` region)

```
nad_notification            id, recipient_user_id, type, title, body, data_json?,
                            application_id?, conversation_id?, message_id?,   <-- refs added (fix G-C3)
                            created_at, read_at?
nad_notification_delivery   id, notification_id, channel(IN_APP|EMAIL|SMS|PUSH|WHATSAPP),
                            provider, provider_message_id?, status(PENDING|SENT|
                            DELIVERED|FAILED|BOUNCED), attempts, last_error?, sent_at?
nad_notification_preference id, user_id, type, channel, enabled       UNIQUE(user_id,type,channel)
nad_notification_template   id, type, channel, locale, subject_tpl, body_tpl   UNIQUE(type,channel,locale)
```

### 4.2 Pipeline

```
domain event (ApplicationStageChanged, DocumentRejected, MessagePosted, DeadlineNear, …)
   │  Spring ApplicationEventPublisher (in-process)
   ▼
NotificationService.create(recipient, type, context)
   │  writes nad_notification  (IN_APP is immediate)
   ▼
for each channel enabled in nad_notification_preference:
   enqueue nad_notification_delivery (PENDING)   [UNIQUE (notification_id, channel)]
   ▼
async dispatcher (@Async pool; Quartz JDBC-store retry sweep on (status, attempts))
   ├─ IN_APP  → row already visible; push an SSE "notification" event (Redis fan-out, §5)
   ├─ EMAIL   → NotificationChannel SPI → SMTP abstraction (SES / Postmark / corp SMTP,
   │            chosen at deploy time)                                  ← v1 BUILT
   ├─ SMS / WHATSAPP / PUSH  → enum kept, NO implementation in v1       ← deferred
   ▼
provider webhooks → /api/internal/notifications/webhook/** → reconcile DELIVERED/BOUNCED
```

- **D6 APPROVED:** `NotificationChannel` SPI; **IN_APP + EMAIL only** built for v1.
  Email via a single SMTP abstraction; concrete provider is deploy-time config.
  **Revision 2 partially realises this:** the `MailSender` port + `smtp`/`log`
  adapters exist (spec §15.1). Deploy-time provider = **Mailpit** locally
  (`docker-compose.yml`), **Gmail SMTP** for staging/prod (App Password), SES an
  option later — all on the same port, env-configured. The `NotificationChannel`
  SPI, `nad_notification*` persistence, and templating-per-locale still sit on top
  of this and are PLANNED. SMS / WhatsApp / PUSH: schema-ready, not implemented.
- Idempotency: `(notification_id, channel)` unique; bounded retries with backoff.
- Localization: templates per `locale` (en first; ar / fr / zh as needed), fallback to
  en.
- Preferences: user-configurable per (type, channel); transactional types (security,
  decision, document-rejected) are non-optional.
- **No PII in rendered templates** — IDs + safe fields only (`SECURITY.md` §6).

### 4.3 Email design system — BUILT

One shared, non-boxed Nadoumi email design backs **every** transactional email
(identity and notification alike). Home: `com.nadoumi.identity.service.mail`
(`nadoumi-notification` already depends on this package, so no new module edge).

- **`EmailMessage(to, subject, body, htmlBody)`** — `body` is always the
  plain-text alternative; `htmlBody` is a full HTML document or `null`.
  `SmtpMailSender` sends `multipart/alternative` when `htmlBody` is set;
  `LoggingMailSender` records both parts.
- **`EmailContent`** — the channel-neutral model: preheader, one heading, a list
  of blocks (`Paragraph`, `CodeBlock`, `KeyValues`, `Divider`), an optional CTA,
  optional titled `ItemGroup`s (plain hairline-separated rows — used by the
  welcome email; empty groups are dropped), locale, and a
  `showPreferencesLink` flag (non-transactional only).
- **`EmailLayout`** — the single place email chrome exists. Renders one
  `EmailContent` into **both** the HTML and a parity plain-text alternative
  (a test asserts every URL + the OTP present in the HTML is present in the
  text). Design: plain white full-bleed background (no dark-mode swap — every
  client renders the same fixed light theme), one 600px column, generous
  whitespace, **no card/border/shadow**; a text wordmark header over a hairline;
  a consistent footer on every email — contact block (`nadoumi.brand.contact.*`)
  → social icons (Facebook / Instagram / TikTok / WhatsApp, each shown only if
  its URL is configured) → the Nadoumi logo → legal line → preferences link
  when applicable.
  Table layout, `role="presentation"`, inlined CSS + one `<style>` for
  `@media max-width`, real `alt`, `lang`, semantic headings, WCAG-AA contrast.
- **`BrandProperties`** (`nadoumi.brand.*`) — wordmark, two deliberately
  separate origins (`baseUrl`, shared with `nadoumi.web.baseUrl`, default
  `https://nadoumi.com`, for CTA/content links; `assetBaseUrl`, default
  `https://api.nadoumi.com`, this backend's own origin — falls back to
  `baseUrl` when unset), logo path, contact emails/phones/office/hours, social
  URLs (Facebook/Instagram/TikTok/WhatsApp), preferences path. `contact.*`
  mirrors `nadoumi-web/app/data/contact.ts` (kept in sync by hand until a
  shared API exists). Email assets: `ruoyi-admin/.../static/email/` (real
  brand logo + official Facebook/Instagram/TikTok/WhatsApp icon art), served
  anonymously via `SecurityConfig` `permitAll` on `/email/**`, resolved
  against `assetBaseUrl` and cache-busted with a `?v=<content hash>` query
  param so replacing a file's bytes always gets a fresh URL, not a mail
  provider's stale cached copy of the old one (see `static/email/README.md`).
- **Identity emails on the shell:** `otp-register`, `otp-password-reset`,
  `account-exists` (subjects standardised, OTP shown as a `CodeBlock`), plus a
  new **`password-changed`** security email fired after a reset / change via a
  `PasswordChangedEvent` + `@TransactionalEventListener(AFTER_COMMIT)` — a
  swallow-on-failure courtesy notice, never a gate.
- **Notification emails on the shell:** `EmailNotificationChannel` wraps the
  rendered `nad_notification_template` body in `EmailLayout` and adds a
  per-type CTA (`SCHOLARSHIP_*` → `/scholarships`, `PROGRAM_PUBLISHED` →
  `/programs`, `UNIVERSITY_PUBLISHED` → `/universities`, `CONTACT_INQUIRY_RECEIVED`
  / `TASK_PROGRESS` → `/admin`, `APPLICATION_*` → `/account/applications`). The
  `NotificationChannel.send` SPI gained a `String type` argument for this.
- **Welcome email:** see §4.4.
- **Application emails:** `NotificationType.APPLICATION_SUBMITTED` /
  `APPLICATION_STATUS_CHANGED` + `en` templates ship in `V55`; **no producer
  yet** — the Application module (Step 6) emits the events (`DOMAIN_EVENTS.md`).

### 4.4 Welcome email — BUILT

`StudentAuthService.register` writes a **`StudentRegistered`** outbox event in its
own transaction. `OutboxToNotificationDispatcher` hands it to
**`WelcomeContentComposer`** (in `nadoumi-notification`, which gains read-only
compile deps on `nadoumi-scholarship` + `nadoumi-program`), which:

1. creates the `WELCOME` notification — **IN_APP only** (the type declares no
   secondary channel), idempotent on `(userId, outbox:<eventId>:<userId>)`;
2. pulls up to 3 most-recently-published programmes and up to 3 soonest-deadline
   scholarships through the **public** catalog services (student-safe DTOs only —
   `SECURITY.md` scholarship confidentiality); **omits a section entirely when
   its query returns nothing — never a placeholder**;
3. renders `EmailContent` (personalised heading, intro from the `WELCOME` EMAIL
   `body_tpl`, the two `ItemGroup`s, an "Explore Nadoumi" CTA) through
   `EmailLayout` and sends it directly via `MailSender`, then records an EMAIL
   `nad_notification_delivery` row (`NotificationService.recordDirectEmailDelivery`)
   so the send shows in the staff console.

This is the one deliberate asymmetry: the welcome email carries structured list
sections the generic flat-string dispatch path does not model, so it bypasses it.

---

## 5. Realtime transport — D6 APPROVED

**SSE for v1.** Endpoints `/api/student/stream` and `/api/staff/stream`
(`text/event-stream`, authenticated). Server → client only: it pushes small
`notification` / `conversation` **pings**; the client then `GET`s the actual resource.
Polling fallback for clients/proxies without SSE. **WebSocket deferred** (revisit if
typing indicators / presence / high-frequency chat become requirements — documented
change per CLAUDE.md §19).

**Multi-instance from day one:** app instances publish stream events to **Redis
pub/sub**; every instance's SSE endpoint subscribes and relays to its connected
clients. No sticky sessions required for correctness (only for connection affinity).

---

## 6. Testing (PLANNED)

- Chat: only participants read/post; read-state math; application-bound thread denies
  non-authorized users; attachment authorization; "promote to document" creates a
  governed `nad_document`.
- Notifications: one domain event → one `nad_notification` + N deliveries per prefs;
  non-optional types ignore a disabled preference; retry/backoff on provider failure;
  webhook reconciliation; no PII in any rendered template; locale fallback.
- Realtime: unauthenticated stream → 401; a user receives only their own events;
  two app instances both deliver an event published by either (Redis fan-out
  integration test).
