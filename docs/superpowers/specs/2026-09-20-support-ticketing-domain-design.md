# Nadoumi — Help & Support Ticketing Domain — Design

Status: **PROPOSED**. Author: Claude Sonnet 5. Date: 2026-09-20.

> One of four domain designs drafted in parallel this session (Application,
> Document, Messaging, Support) for review before any implementation starts.
> Nothing here has been approved yet — every `DECISION REQUIRED` item below needs
> a human answer before this goes to `writing-plans`.

---

## 1. What exists today (EXISTING)

Verified by search — **nothing exists for authenticated support ticketing**:

- No `nad_support*` table, no `SupportTicket`/`Ticket` class, no design doc, no
  phase-tracker row anywhere (`docs/PLATFORM_ARCHITECTURE.md` §8 covers Steps
  6–10; nothing named Support/Help/Ticket appears in it).
- The only hits for "ticket" in the codebase are the OTP verification ticket
  concept in `nadoumi-identity` (`TicketService`, `nad:ticket:*` Redis keys,
  `EmailOtpVerifyRequest.ticket`) — an unrelated, single-use auth artifact, not a
  support work item.
- The closest real precedent is `nad_contact_inquiry` (`ContactService`,
  `ContactInquiry`, `ContactInquiryStatus`) in `nadoumi-identity`: an **anonymous**,
  **one-shot** form submission with a shallow triage lifecycle
  (`NEW → READ → RESPONDED → ARCHIVED`), single `handledBy`/`handledAt` fields (no
  reassignment history), that emits `ContactInquiryReceived` to the existing
  notification outbox. Useful as the outbox-wiring pattern; not reusable as the
  ticket model itself — it has no thread, no authenticated owner, no
  priority/category/SLA concept, and per `docs/PLATFORM_ARCHITECTURE.md` Step 10
  it's already slated to move to a future `nadoumi-content` module for CMS-style
  triage, which is a different concern from student support.
- RuoYi/`ruoyi-framework` has no workflow engine, ticket, or SLA concept to reuse
  (confirmed: no Flowable/Activiti dependency, no priority/assignment scaffolding
  outside `sys_job`). Nothing here is RuoYi's to give us.
- The Messaging/Communication domain (being designed in parallel this session) has
  a **detailed, already-BASELINE, D6-approved** spec at
  `docs/COMMUNICATION_AND_NOTIFICATIONS.md` §3 — not just a rough draft. Read in
  full for this design:
  ```
  nad_conversation             id, subject?, application_id?, status(OPEN|CLOSED), create_by/time
  nad_conversation_participant id, conversation_id, user_id, role(STAFF|APPLICANT|AGENT|GUARDIAN),
                               added_at, removed_at?, last_read_message_id?, muted
  nad_message                  id, conversation_id, sender_user_id, body, created_at, edited_at?, deleted_at?
  nad_message_attachment       id, message_id, promoted_document_id?, storage_key, content_type, size_bytes
  ```
  `application_id` is **nullable** — a conversation is not required to be
  application-bound. Realtime (SSE + Redis fan-out), read-status, attachments,
  and authorization (`nad_user_applicant_access` grant externally,
  `nad:conversation:participate` + assignment for staff) are all already designed
  there, D6-approved, not open questions.

## 2. Requirements (from `CLAUDE.md` §12 and the session's dashboard spec)

A ticketing system for authenticated students to raise and track support
requests with Nadoumi staff: create, view own, reply; staff see a queue, can
categorize/prioritize/assign/reassign, change status, and reply — with every
significant state change (`CLAUDE.md` general auditability principle) recorded,
not silently overwritten.

---

## 3. The core design question: ticket vs. conversation

This is the decision that most affects Messaging's own design, so it's presented
first, on purpose.

### Option A — thin ticket entity that *has a* conversation (RECOMMENDED)

`nad_support_ticket` is a **work-item wrapper**: status/priority/category/
assignment. It holds a 1:1 `conversation_id` FK into the already-designed
`nad_conversation`, and reuses `nad_message` / `nad_conversation_participant` /
`nad_message_attachment` / SSE / read-status entirely as-is — zero duplication.

- **Pro:** zero duplicated machinery (DRY, matches this project's explicit
  "avoid duplicated business logic" rule). Messaging's schema stays exactly as
  already approved — no changes needed there for Support to exist.
  Ticket-specific concepts (priority, SLA-ish fields, category, reassignment
  history) never leak onto `nad_conversation`, which also carries
  application-bound chat threads that have no business having a "priority."
- **Con:** one small new module/table pair to own the ticket-vs-thread mapping.

### Option B — fully independent schema

Ticketing gets its own `nad_support_message`/`nad_support_thread` tables,
independent of `nad_conversation`.

- **Con:** duplicates everything Messaging already specifies in detail
  (attachments, read status, realtime delivery, participant roles). No
  technical reason forces this — rejected.

### Option C — no separate entity, just a `conversation_type` tag on `nad_conversation`

- **Con:** a ticket queue needs to filter/sort by status, priority, category,
  and assignee — putting those columns on the general-purpose `nad_conversation`
  means every application-bound chat thread also carries nullable
  priority/SLA columns it will never use. Blurs a clean domain boundary for no
  real savings over Option A. Rejected.

**Recommendation: Option A.** Messaging's own design does not need to change to
accommodate this — `nad_conversation.application_id` being nullable already
makes a non-application-bound "support" conversation a natural fit.

**Coordination note for the Messaging domain fork:** `nad_conversation_participant.role`
currently enumerates `STAFF|APPLICANT|AGENT|GUARDIAN`. A student opening a
support ticket may have **no applicant profile at all yet** (e.g. "how do I get
started" before onboarding) — `APPLICANT` may not fit. Flagging this as an open
question for Messaging's own design rather than resolving it here, since it's
their enum to own; Support's participant rows will use whatever role Messaging
settles on for "the external, non-staff party in a conversation that isn't
necessarily about one specific applicant."

---

## 4. Data model (PROPOSED)

Migration numbers **V87–V89** (Application uses V63-V68, Document V71-V78,
Messaging V79-V86 — confirmed disjoint against this session's other three
designs). V90-V92 are reserved headroom, not pre-assigned to any specific
change.

```sql
-- V87__nad_support_ticket.sql
create table nad_support_ticket (
  id                 bigint auto_increment primary key,
  conversation_id    bigint not null,
  applicant_id       bigint null,
  opened_by_user_id  bigint not null,
  subject            varchar(200) not null,
  category           varchar(40) not null,   -- ACCOUNT|APPLICATION|DOCUMENT|PAYMENT|TECHNICAL|OTHER
  priority           varchar(10) not null default 'NORMAL',  -- LOW|NORMAL|HIGH|URGENT
  status             varchar(20) not null default 'OPEN',    -- OPEN|IN_PROGRESS|WAITING_ON_STUDENT|RESOLVED|CLOSED
  assigned_staff_id  bigint null,
  resolved_at        datetime null,
  closed_at          datetime null,
  create_by          varchar(64) default '',
  create_time        datetime,
  update_by          varchar(64) default '',
  update_time        datetime,
  constraint uq_ticket_conversation unique (conversation_id),
  constraint fk_ticket_conversation foreign key (conversation_id) references nad_conversation(id) on delete restrict,
  constraint fk_ticket_applicant foreign key (applicant_id) references nad_applicant(id) on delete set null,
  constraint fk_ticket_opened_by foreign key (opened_by_user_id) references sys_user(id) on delete restrict,
  constraint fk_ticket_assignee foreign key (assigned_staff_id) references sys_user(id) on delete set null,
  index idx_ticket_status (status),
  index idx_ticket_assignee (assigned_staff_id),
  index idx_ticket_opened_by (opened_by_user_id),
  index idx_ticket_applicant (applicant_id)
);

create table nad_support_ticket_event (
  id              bigint auto_increment primary key,
  ticket_id       bigint not null,
  event_type      varchar(30) not null,  -- STATUS_CHANGED|ASSIGNED|REASSIGNED|PRIORITY_CHANGED|CATEGORY_CHANGED
  old_value       varchar(100) null,
  new_value       varchar(100) null,
  actor_user_id   bigint not null,
  created_at      datetime not null,
  constraint fk_ticket_event_ticket foreign key (ticket_id) references nad_support_ticket(id) on delete cascade,
  constraint fk_ticket_event_actor foreign key (actor_user_id) references sys_user(id) on delete restrict,
  index idx_ticket_event_ticket (ticket_id)
);
```

- `nad_support_ticket_event` is a system audit log of **work-item state
  transitions**, deliberately separate from `nad_message` — mirrors the same
  principle Messaging's own spec already states for `nad_application_event`
  ("no SYSTEM message rows... the UI merges authored messages and events
  chronologically"). Ticket status/priority/assignment history is not a chat
  message.
- `conversation_id` is `ON DELETE RESTRICT`, not `CASCADE` — a ticket must never
  silently lose its thread; deleting a conversation with a live ticket should be
  refused at the DB level, matching `database.md`'s "CASCADE only for true
  ownership" rule (the ticket owns its *event log*, not the conversation).
- `applicant_id` is nullable and `ON DELETE SET NULL` — a ticket can exist
  without ever being about a specific applicant case.
- Money/SLA-as-decimal is out of scope here — no SLA timers in v1 (see
  DECISION REQUIRED #3).

```sql
-- V88__nad_support_ticket_menu_perm.sql
-- RuoYi sys_menu rows for a staff "Support Tickets" console screen +
-- nad:support:ticket:view / :manage / :assign permission rows, following the
-- exact seeding pattern V33 used for nad:notification:*.
```

```sql
-- V89__nad_support_notification_types.sql
-- Seed NotificationType rows + en templates for TICKET_OPENED / TICKET_ASSIGNED /
-- TICKET_STATUS_CHANGED, following the V33/V55 seeding pattern exactly.
```

---

## 5. Module structure (PROPOSED)

New module `nadoumi-support`, alongside the existing `nadoumi-applicant`,
`nadoumi-scholarship`, etc. Depends on:

- `nadoumi-communication`'s **service interface** for conversation/message
  creation and posting (never its mapper or entities directly —
  `architecture.md`'s cross-module rule).
- `nadoumi-common` (`OutboxWriter` SPI) + `nadoumi-notification`'s existing
  pipeline for `TicketOpened` / `TicketAssigned` / `TicketStatusChanged`.
- `nadoumi-identity` for user lookups (assignee display name, etc.) via its
  service interface.

```
nadoumi-support/
  domain/            SupportTicket, SupportTicketEvent, enums (Category, Priority, Status, TicketEventType)
  mapper/            SupportTicketMapper, SupportTicketEventMapper (+ MyBatis XML)
  service/           SupportTicketService (student ops), StaffSupportTicketService (staff ops)
  web/
    student/         StudentSupportTicketController — /api/student/support/tickets/**
    staff/           StaffSupportTicketController   — /api/staff/support/tickets/**
    request/         CreateTicketRequest, AssignTicketRequest, ChangeStatusRequest, ...
    response/        TicketSummaryResponse (queue row), TicketDetailResponse (incl. thread)
```

### Why a separate module rather than folding into `nadoumi-communication`

A judgment call, not a hard requirement — see DECISION REQUIRED #4. Leaning
separate because the *authorization* and *workflow* shape genuinely differs from
generic chat: ticket assignment/priority/category is a staff-workflow concern
(closer in spirit to how `nadoumi-scholarship` differs from `nadoumi-university`
despite both being "catalog" domains) rather than a messaging-transport concern.
Keeping them separate also means Messaging's spec, already D6-approved, needs
**zero changes** to support this domain.

---

## 6. API surface (PROPOSED)

**Student** (`/api/student/support/tickets/**`, any authenticated student,
principal = `sys_user.id`):
- `POST /` — create ticket (subject, category, initial message body →
  delegates to Communication to create the conversation + first message in one
  transaction). Response: `TicketDetailResponse`.
- `GET /` — list own tickets (`opened_by_user_id = principal`), paginated,
  filterable by status.
- `GET /{id}` — detail incl. the conversation thread (delegates message list to
  Communication). 404 (not 403 — don't leak existence) if not the opener/participant.
- `POST /{id}/messages` — convenience wrapper delegating to Communication's
  post-message endpoint for this ticket's conversation.

**Staff** (`/api/staff/support/tickets/**`):
- `GET /` — queue, filterable by status/priority/category/assignee, gated
  `nad:support:ticket:view`.
- `GET /{id}` — detail, same gate.
- `PATCH /{id}/status` — gated `nad:support:ticket:manage` **and** (assignee OR
  holder of a broader manage permission) — mirrors "staff responsibility" from
  `CLAUDE.md` §13. Writes a `nad_support_ticket_event` row, never silently
  overwrites `status`.
- `PATCH /{id}/assign` — gated `nad:support:ticket:assign`. Writes an
  `ASSIGNED`/`REASSIGNED` event row. Triggers `TicketAssigned` notification to
  the new assignee.
- `PATCH /{id}/priority`, `PATCH /{id}/category` — gated `nad:support:ticket:manage`,
  each writes its own event row.
- `POST /{id}/messages` — staff reply, delegates to Communication.

No public/anonymous surface — this domain is authenticated-student-only,
unlike `nad_contact_inquiry` which stays anonymous.

DTOs never expose the entity directly (`architecture.md` rule) — `TicketSummaryResponse`
for queue rows, `TicketDetailResponse` (adds the message thread + event history)
for detail.

---

## 7. Notification integration (PROPOSED)

Reuses the existing, fully-built `OutboxWriter` → `OutboxPollerJob` →
`OutboxToNotificationDispatcher` pipeline (`nadoumi-notification`) — no new
infrastructure, same pattern every other domain in this session follows.

| Event | Written by | Recipients | Payload |
| --- | --- | --- | --- |
| `TicketOpened` | `SupportTicketService.create`, same transaction | staff holding `nad:support:ticket:view` (mirrors `ContactInquiryReceived`'s staff-permission-query recipient resolution) | `ticketId, subject, category, openedByName` |
| `TicketAssigned` | assignment service method, same transaction | explicit `recipientUserIds: [assigneeId]` (mirrors `TaskProgressChanged`'s explicit-recipient pattern) | `ticketId, subject, assignedByName` |
| `TicketStatusChanged` | status-change service method, same transaction | explicit `recipientUserIds: [openedByUserId]`, only fired for RESOLVED / WAITING_ON_STUDENT transitions (mirrors `ApplicationStatusChanged`'s "sanitised student view" — status label only, no internal notes) | `ticketId, subject, status` |

A reply posted through Communication's `MessagePosted` event already gets SSE +
offline-notification handling per Messaging's own design — Support does not
need to reinvent this.

Three new `NotificationType` enum values + `en` templates ship in **V89**,
following the exact seeding pattern `V33`/`V55` already established.

---

## 8. Testing plan (per `.claude/rules/testing.md`)

- **Unit** (`SupportTicketServiceTest`): status-transition guards (no direct
  OPEN→CLOSED skip without going through RESOLVED — or state the allowed
  transition matrix explicitly, see DECISION REQUIRED #5); every
  status/priority/category/assignment change writes exactly one event row with
  correct old/new values.
- **Controller** (`@WebMvcTest` + MockMvc): student list only returns own
  tickets; detail endpoint 404s (not 403) for another student's ticket; staff
  queue endpoint 401/403 without `nad:support:ticket:view`.
- **Required authorization tests** (`testing.md`'s explicit requirement):
  prove a student cannot view or reply to another student's ticket; prove staff
  without `nad:support:ticket:manage` cannot change status; prove staff without
  `nad:support:ticket:assign` cannot assign/reassign.
- **Integration**: create ticket → conversation created 1:1 → first message
  persisted → `TicketOpened` row in `nad_outbox_event` → drained → notification
  created for staff (mirrors the existing `OutboxPipelineTest` pattern already
  used for Contact/Scholarship events).

---

## 9. Migration risks / affected modules

- **New module** `nadoumi-support` — no existing code touched to introduce it.
- Depends on Messaging (`nadoumi-communication`) existing first, or at minimum
  landing in the same release — this domain cannot ship before Messaging's
  `nad_conversation`/`nad_message` tables exist. Sequencing dependency, not a
  migration risk per se.
- Adds three `NotificationType` enum values — confirm the enum is a plain Java
  enum (additive, no migration risk) vs. a DB-checked value list before assuming
  V89 is sufficient; if `NotificationType` values are validated against a
  fixed DB list anywhere, that needs updating too (recommend the implementer
  grep for existing `NotificationType.values()` DB constraints before writing V89).
- No changes required to any other domain's schema — the whole point of Option A.

---

## 10. DECISION REQUIRED

1. **Ticket-vs-conversation boundary** (§3): recommend Option A (thin ticket +
   owned conversation). This is the one decision that would force a rework of
   Messaging's design too if reversed later — worth confirming before either
   domain is implemented.
2. **Assignment model**: pool (any staff with `nad:support:ticket:manage` can
   reply/claim an unassigned OPEN ticket) vs. strict pre-assignment-required
   before any staff reply. Recommend **pool model** — closer to
   `nad_contact_inquiry`'s any-staff-can-handle triage than Application's
   strict single-assignee-per-stage model, since support is inherently a queue,
   not a case-owned workflow.
3. **SLA**: out of scope for v1 (no due-by timers, no escalation). Confirm this
   is acceptable, or flag if SLA tracking is actually a launch requirement —
   would add real scope (a scheduled job, an SLA-breach notification type).
4. **Module placement**: standalone `nadoumi-support` (recommended, §5) vs.
   folding into `nadoumi-communication`. Either is buildable; recommend
   standalone for a cleaner authorization/workflow boundary.
5. **Status transition matrix**: exact allowed transitions between
   OPEN/IN_PROGRESS/WAITING_ON_STUDENT/RESOLVED/CLOSED (e.g. can a CLOSED
   ticket reopen, or must the student open a new one?) needs an explicit answer
   before `SupportTicketService`'s guard logic can be written — proposing
   OPEN→IN_PROGRESS→{WAITING_ON_STUDENT⇄IN_PROGRESS}→RESOLVED→CLOSED, with
   CLOSED→OPEN allowed only within some short window (or not at all) as a
   follow-up decision, not blocking the rest of this design.

---

## 11. Self-review

- Placeholder scan: none found — every section has a concrete recommendation,
  no TBDs.
- Internal consistency: schema (§4), API (§6), and notification table (§7) all
  reference the same status/event vocabulary; module dependency direction (§5)
  matches the "service interface, not mapper" rule stated once and followed
  throughout.
- Scope check: single bounded domain, ready for `writing-plans` once the five
  DECISION REQUIRED items get an answer — items #2, #3, #5 are default-able
  (this doc states a recommendation for each) if the user wants to proceed
  without a live round-trip on every one.
- Ambiguity check: the one term that could be read two ways — "assignment" —
  is used consistently to mean *staff-to-ticket* assignment throughout; never
  conflated with `nad_user_applicant_access` (applicant-access) assignment,
  which this domain does not touch.

---

## 12. Implementation status (2026-09-20)

Backend implemented on branch `feat/support-domain-backend` (stacked on the Messaging
branch). Decisions adopted from §10 without a live round-trip: **#1 Option A**, **#2 pool
model**, **#3 no SLA in v1**, **#4 standalone `nadoumi-support`**, **#5** the proposed matrix
with `CLOSED` terminal (a student opens a new ticket). Deviations from this document, following
the real code:

- FKs reference `sys_user (user_id)`, not `sys_user(id)`.
- `ConversationService.open` demands `MESSAGE_STAFF` on an applicant, which blocks students without
  an applicant profile. Added the smallest additive methods in `nadoumi-communication`:
  `openSupport(subject, body)` (SUPPORT-typed, no applicant required) and
  `listSupportMessagesForStaff` (staff read of a SUPPORT thread without joining it; refuses
  GENERAL conversations).
- `TicketOpened` targets `nad:support:ticket:view` holders. The dispatcher's staff audience was
  hard-coded to `nad:notification:list`, so it now honours an optional `audiencePermission` payload key.
- Students may reply only while a ticket is `OPEN`, `IN_PROGRESS` or `WAITING_ON_STUDENT`; a reply to
  `WAITING_ON_STUDENT` moves it back to `IN_PROGRESS`. The first staff reply on an unassigned ticket
  claims it and starts work.
- Student DTOs omit priority and assignee (internal triage data stays staff-only).
- Not built: `@WebMvcTest` controller tests and an integration test of the outbox pipeline (needs Docker /
  full context); frontend (student help page, admin queue).
