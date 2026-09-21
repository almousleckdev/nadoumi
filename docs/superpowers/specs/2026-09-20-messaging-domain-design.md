# Nadoumi — Communication / Messaging Domain — Design Status & Implementation Addendum

Status: **ADDENDUM to an existing approved baseline**. Author: Claude Sonnet 5.
Date: 2026-09-20.

> This is **not** a new design. `docs/COMMUNICATION_AND_NOTIFICATIONS.md` §3 (Chat)
> and §5 (Realtime transport, **D6 approved**) already specify this domain in
> detail. This document verifies that baseline against the current codebase, fills
> the gaps it left as placeholders (migration numbers, the exact cross-module
> integration points), and resolves the open coordination points with the sibling
> Document and Support domain designs — so it is ready to hand to `writing-plans`
> without re-litigating decisions already made.

---

## 1. The authoritative design (read this first)

| Document | Status | Covers |
| --- | --- | --- |
| `docs/COMMUNICATION_AND_NOTIFICATIONS.md` §3 | BASELINE | `nad_conversation` / `nad_conversation_participant` / `nad_message` / `nad_message_attachment` model, FK direction, no-SYSTEM-message rule, read-status math, confidentiality rule, authorization sketch |
| `docs/COMMUNICATION_AND_NOTIFICATIONS.md` §5 | **D6 APPROVED** | SSE for v1 (`/api/student/stream`, `/api/staff/stream`), server→client pings only, Redis pub/sub for multi-instance fan-out, WebSocket explicitly deferred |
| `docs/COMMUNICATION_AND_NOTIFICATIONS.md` §6 | PLANNED | Required test list |
| `CLAUDE.md` §12 | Source requirement | Chat is a distinct concept from Notifications; conversations/participants/messages/attachments/read-status |
| `docs/PLATFORM_ARCHITECTURE.md` §8, Step 8 | Confirms sequencing | `nadoumi-communication`: conversations + messages + attachments on an application; SSE — scheduled **after** Step 6 (Application) and Step 7 (Document), which this addendum's migration numbering respects |

**Nothing in this addendum overrides those.** Where this addendum is silent, the
existing baseline governs.

## 2. Verified still accurate (2026-09-20 recheck)

- No conversation/message code exists anywhere in the codebase (`grep` for
  `*conversation*`/`*message*` across all `.java`/`.sql` turns up nothing but
  unrelated RuoYi `MessageUtils` and the identity module's `EmailMessage`). The
  baseline is exactly as unbuilt as it says.
- **Redis is already a real, in-use dependency** — `ruoyi-framework`'s
  `RedisConfig` (a `RedisTemplate<Object,Object>` bean) backs RuoYi sessions and
  the rate limiter (`RateLimiterAspect`); `nadoumi-identity`'s `SessionRevoker`
  already does a `SCAN`-based bulk Redis operation. **No
  `RedisMessageListenerContainer` bean exists yet** — that's the one new piece
  needed for pub/sub, a small, well-understood Spring Data Redis addition, not a
  new infrastructure dependency. D6's Redis fan-out plan is confirmed buildable
  on what's already there.
- **The authorization capability this module needs already exists and is
  wired**, not just declared: `ApplicantCapability.MESSAGE_STAFF`
  (`nadoumi-common/.../access/ApplicantCapability.java`) is granted by default to
  `OWNER`, `AGENT`, and `GUARDIAN` roles (`AccessCapabilityMatrix`, not
  `VIEWER`), and `NadoumiAccessService` (implemented in
  `nadoumi-identity/.../NadoumiAccessServiceImpl`) is a live, callable bean —
  `canAccessApplicant(applicantId, "MESSAGE_STAFF")` /
  `canAccessApplication(applicationId, "MESSAGE_STAFF")` are ready to call on
  day one. This is the same choke-point the Application-domain design uses;
  nothing new to build for student-side authorization.
- **`nad_notification` already carries `conversation_id` and `message_id`
  nullable columns** (migration `V32`, no FK — same treatment as its also-nullable,
  also-unbuilt-at-the-time `application_id` column). This module does not need
  to migrate `nad_notification`; it only needs to populate those columns when it
  writes a `MessagePosted` outbox event, exactly mirroring how
  `ApplicationSubmitted`/`ApplicationStatusChanged` will populate
  `application_id`.
- **`nadoumi-media` (P1, shipped) is the attachment storage answer** — confirmed
  by reading `MediaOwnerKind` (`nadoumi-common/.../media/MediaOwnerKind.java`):
  it already reserves `APPLICATION` and `DOCUMENT` values ahead of those modules
  being built, the same anticipatory pattern this domain should follow by adding
  a `MESSAGE` value. `MediaCategory` (same package) has no `MESSAGE_ATTACHMENT`
  entry yet — one needs adding to `MediaCategoryPolicy`'s static rule table
  (`PROTECTED`, image + PDF allow-list, a moderate size cap, `raw`/`image`
  resource type per file, folder `message/attachment`).
- `OutboxEventTypes` (`nadoumi-common`) has no `MESSAGE_POSTED` constant yet —
  needs adding, following the exact pattern of the seven existing constants.
- Migration ledger drift: `docs/PLATFORM_ARCHITECTURE.md`'s closing note says
  "next new migration is V35," and `COMMUNICATION_AND_NOTIFICATIONS.md` §3.1/§4.1
  label their DDL "V10 region" / "V11 region" — both stale. The actual next free
  migration in `ruoyi-admin/src/main/resources/db/migration/` is **V63**. This
  addendum uses **V79–V86**, reserved disjoint from the sibling Application
  (V63–V70) and Document (V71–V78) domain designs written in parallel this
  session. **DECISION REQUIRED:** update `docs/DATABASE_DESIGN.md` §6's ledger
  once an implementation order is picked, so it stops citing V35.

## 3. Realtime transport — confirmed approach

Reaffirms D6: **SSE, not WebSocket.** Concretely:

- `SseEmitter` per authenticated connection (`/api/student/stream`,
  `/api/staff/stream`), no new dependency (Spring MVC ships `SseEmitter`).
- One new bean, `RedisMessageListenerContainer` subscribed to a small, fixed set
  of channels (e.g. `nadoumi:stream:notification`, `nadoumi:stream:conversation`),
  using the existing `RedisTemplate`'s connection factory. On message, the
  listener looks up locally-held `SseEmitter`s for the target `userId` (an
  in-memory `ConcurrentHashMap<Long, List<SseEmitter>>` per instance) and pushes
  a small ping payload (`{type, refId}` — never the message body itself; the
  client re-fetches via the existing REST endpoints, matching "IN_APP → push an
  SSE ping" already specified in §4.2's pipeline diagram).
- Every app instance publishes to Redis on write (`OutboxToNotificationDispatcher`
  for notifications; this module's own message-post path for conversation
  pings) and every instance's listener relays to its own locally-connected
  clients — this is what makes it correct without sticky sessions (§5,
  "Multi-instance from day one").
- Polling fallback: unchanged from §4.2 — the client already polls
  `/api/notifications/unread-count`; the same fallback pattern (poll
  `/api/{student,staff}/conversations?since=`) covers a client that never gets
  an SSE connection (proxy strips `text/event-stream`, etc.).

**Alternatives considered and rejected** (per YAGNI, this being a modular
monolith, not microservices):
- A dedicated message broker (Kafka/RabbitMQ) — no other domain in this system
  needs one; Redis pub/sub is already present and sufficient at this scale.
- WebSocket — explicitly deferred by D6; nothing in this domain's v1 scope
  (typing indicators, presence) needs bidirectional low-latency framing that SSE
  can't do with a plain `POST` for the client→server half.

## 4. Data model (finalizes §3.1, migrations V79–V86)

```
V79__nad_conversation.sql
  nad_conversation (
    id                bigint pk auto_increment,
    subject           varchar(200)  null,
    application_id    bigint        null,   -- soft ref; nad_application doesn't exist yet
    conversation_type varchar(20)   not null default 'GENERAL',  -- GENERAL | SUPPORT (extensible, see §7)
    status            varchar(10)   not null default 'OPEN',     -- OPEN | CLOSED
    create_by, create_time, update_by, update_time  -- BaseEntity audit convention
  )
  index on (application_id), index on (conversation_type, status)

V80__nad_conversation_participant.sql
  nad_conversation_participant (
    id                    bigint pk auto_increment,
    conversation_id       bigint not null references nad_conversation(id) on delete cascade,
    user_id               bigint not null references sys_user(user_id) on delete cascade,
    role                  varchar(12) not null,  -- STAFF | APPLICANT | AGENT | GUARDIAN
    added_at              datetime not null,
    removed_at            datetime null,
    last_read_message_id  bigint null,
    muted                 tinyint(1) not null default 0,
    unique (conversation_id, user_id),
    index on (user_id, removed_at)
  )

V81__nad_message.sql
  nad_message (
    id                bigint pk auto_increment,
    conversation_id   bigint not null references nad_conversation(id) on delete cascade,
    sender_user_id    bigint not null references sys_user(user_id) on delete restrict,
    body              varchar(4000) not null,
    created_at        datetime not null,
    edited_at         datetime null,
    deleted_at        datetime null,  -- soft delete; row + history kept per §3.3
    index on (conversation_id, id)   -- id is the natural cursor for pagination + unread math
  )

V82__nad_message_attachment.sql
  nad_message_attachment (
    id                    bigint pk auto_increment,
    message_id            bigint not null references nad_message(id) on delete cascade,
    media_asset_id        bigint not null references nad_media_asset(id) on delete restrict,
    promoted_document_id  bigint null,  -- soft ref; nad_document doesn't exist yet (Step 7)
    create_time           datetime not null
  )
```

Deliberate change from the §3.1 sketch: attachments reference `nad_media_asset`
(P1, shipped) via `media_asset_id`, **not** an ad-hoc `storage_key` +
`content_type` + `size_bytes` triple — those already live on `nad_media_asset`
and duplicating them here would let the two rows drift. This is a strict
narrowing of the baseline, not a contradiction: §3.1 predates P1 (Media)
landing; "own `storage_key`, not a governed `nad_document`" still holds — the
attachment is a plain `MediaOwnerKind.MESSAGE` asset, never auto-promoted to a
`nad_document` — `promoted_document_id` is set only by the explicit staff
"promote to document" action (§3.5 of `DOCUMENT_MANAGEMENT.md`, a Step 7
follow-up, not built by this module).

```
V83__nad_media_owner_kind_message.sql   -- if MediaOwnerKind needs a matching DB-side check/enum; see open item below
V84__nad_message_category_policy_seed.sql   -- if MediaCategory policy is data-driven rather than the current static EnumMap; likely a no-op migration, verify against MediaCategoryPolicy before writing
V85, V86  -- headroom for the staff `nad:conversation:*` permission rows (menu + perm seed, same shape as V33's notification perms) and any index tuning found during implementation
```

No hard FK from `nad_notification.conversation_id`/`message_id` to
`nad_conversation`/`nad_message` — same soft-reference treatment
`application_id` already has, so this module's migrations don't force a
migration-order dependency onto `nadoumi-notification`, and vice versa.
**DECISION REQUIRED:** confirm this soft-reference choice (recommended) versus
adding the FK once both tables exist.

## 5. Module structure

`nadoumi-modules/nadoumi-communication/` — mirrors `nadoumi-scholarship`'s
layout (`domain/`, `domain/enums/`, `mapper/`, `service/`, `web/` +
`web/request` + `web/response`), same as the Application-domain addendum
recommends.

- **Entities/mappers**: `Conversation`, `ConversationParticipant`, `Message`,
  `MessageAttachment` — one mapper interface + XML each, MyBatis, parameter
  binding only (no string-concatenated SQL, per `database.md`).
- **`ConversationService`** (public interface other modules may call, e.g. a
  future Support module creating a `SUPPORT`-typed conversation): `open`,
  `post`, `markRead`, `close`, `addParticipant`, `removeParticipant`,
  `listForUser`, `listForApplication`.
- **`MessagePublisher`** (internal): on a successful `post`, in the same
  transaction — writes the `nad_message` row, writes an outbox event
  (`MessagePosted`, `Propagation.MANDATORY` via `OutboxWriter`, matching every
  other producer in this codebase) with `recipientUserIds` = other participants
  minus the sender (same payload shape convention `TaskProgressChanged` already
  uses), then (after commit) publishes the Redis ping for online recipients.
- **DTOs split by audience**: student (`/api/student/conversations/**`) never
  sees participants outside their own applicant scope or conversations they
  aren't a participant of; staff (`/api/staff/conversations/**`) gets
  participant-management + moderation actions gated by new
  `nad:conversation:participate` / `nad:conversation:participant:manage`
  permissions (menu + perm seed migration, same shape as `V33`).

## 6. API surface (finalizes §3.2)

| Endpoint | Audience | Notes |
| --- | --- | --- |
| `GET /api/student/conversations` | student | own conversations only (`accessibleApplicantIds()` scope, or direct participant rows) |
| `POST /api/student/conversations` | student | requires `MESSAGE_STAFF` on the target applicant/application |
| `GET /api/student/conversations/{id}/messages` | student | 403 if not a participant |
| `POST /api/student/conversations/{id}/messages` | student | body + optional attachment refs; requires `MESSAGE_STAFF` |
| `POST /api/student/conversations/{id}/read` | student | updates `last_read_message_id` |
| `POST /api/student/conversations/{id}/attachments` | student | proxied upload via `MediaService`, category `MESSAGE_ATTACHMENT` |
| `GET /api/student/stream` | student | SSE, ping-only |
| `GET /api/staff/conversations` | staff | `nad:conversation:participate` + assignment scope |
| `POST /api/staff/conversations/{id}/participants` | staff | `nad:conversation:participant:manage` |
| `DELETE /api/staff/conversations/{id}/participants/{userId}` | staff | same perm |
| `POST /api/staff/conversations/{id}/close` | staff | `nad:conversation:participate` |
| `POST /api/staff/messages/{id}/promote-document` | staff | sets `promoted_document_id` — **stubbed/deferred** until Step 7 (Document) ships; document this as a 501 or feature-flagged no-op until then, not a fake success |
| `GET /api/staff/stream` | staff | SSE, ping-only |

Every mutating endpoint: controller-level `@PreAuthorize` **and** a
service-level `NadoumiAccessService`/perm re-check (defense in depth, per
`NadoumiAccessService`'s own Javadoc contract and `security.md`).

## 7. Coordination note for the Support/Ticketing domain (designed separately)

`nad_conversation.conversation_type` (`GENERAL | SUPPORT`, extensible) is the
hook left open for Support/Ticketing to build a ticket record that **owns** a
conversation rather than reinventing threading: a `nad_support_ticket` row
would hold `conversation_id` (FK into this table) plus ticket-specific fields
(status, priority, assignee). This module does not implement or assume
anything about that ticket table — the Support design agent decides
independently whether to take this path. If it doesn't, `conversation_type`
simply stays `GENERAL` for every row and costs nothing.

## 8. Testing plan (finalizes §6)

Per `testing.md`: unit (participant/read-state math, e.g. "unread = id >
last_read_message_id"), service (authorization branches — non-participant
denied, `MESSAGE_STAFF` capability required to open/post), controller
(`@WebMvcTest`/MockMvc — 401/403 not 500, DTO shape leaks nothing), integration
(real DB — soft-delete keeps history, unique participant constraint).

**Required security tests** (mirrors the Application-domain addendum's list):
prove a non-participant cannot read or post to a conversation; prove a student
without `MESSAGE_STAFF` (e.g. `VIEWER`-role access) is rejected; prove staff
without `nad:conversation:participate` cannot list/open conversations for
applicants outside their assignment.

**SSE-specific plan** (harder to unit-test than REST, explicit approach):
1. Test the *production* side thoroughly and conventionally — `MessagePublisher`
   writes the right outbox row with the right payload (same pattern as
   `OutboxToNotificationDispatcherTest`), and the Redis-publish call happens
   with the right channel/payload, using a mocked `RedisTemplate` — no real
   Redis needed for this layer.
2. One thin integration test proves the actual `SseEmitter` wiring: open a
   stream, publish to the Redis channel it's subscribed to (via a real/embedded
   Redis in a Testcontainers-style test), assert the client receives the
   expected event. Keep this to one or two tests — it is the one place a broader
   integration test earns its cost, not the default for this module.
3. Explicitly test: unauthenticated stream request → 401; a connected user
   receives only pings addressed to their own `userId`, never another user's.

## 9. Migration risks

- Redis pub/sub adds a new failure mode (a dropped Redis connection silently
  stops realtime delivery) — mitigated by the existing polling fallback already
  specified in §4.2; this module inherits it for conversations, not just
  notifications. No new operational risk class, same one already accepted for
  notifications.
- `nad_message.body` cap (`varchar(4000)`) — confirm this is enough for the
  product's real usage (file-sharing-heavy conversations lean on attachments,
  not huge text bodies) before implementation; flagged as **DECISION REQUIRED**
  rather than assumed.
- `promoted_document_id` on `nad_message_attachment` references a table that
  doesn't exist yet (Step 7). Ship it as a nullable, unenforced (no FK) column
  now; add the FK in a Document-domain follow-up migration once `nad_document`
  exists — same soft-reference pattern used throughout this codebase for
  forward-referencing not-yet-built tables.

## 10. Affected modules

New: `nadoumi-communication`. Touched: `nadoumi-common` (`OutboxEventTypes` +1
constant, `MediaOwnerKind` +1 value, `MediaCategory` +1 value +
`MediaCategoryPolicy` +1 rule), `nadoumi-notification` (`OutboxToNotificationDispatcher`
gains a `MessagePosted` mapping to `NotificationType.MESSAGE_POSTED` + one new
`en` template row, following the exact existing `mapType`/`isPublicCatalogAnnouncement`-style
switch pattern — `MessagePosted` is participant-targeted via
`recipientUserIds`, like `TaskProgressChanged`, not audience-resolved by
permission). No changes required to `nadoumi-identity` (`NadoumiAccessService`
and `MESSAGE_STAFF` are already complete).

## 11. Open DECISION REQUIRED items for the user

1. **Migration order/numbering**: confirm V79–V86 for this domain once an
   implementation sequence across Application/Document/Messaging/Support is
   picked (the phase tracker already orders it Step 6 → 7 → 8, i.e. after both
   siblings — renumber down if Messaging ships before Document actually lands).
2. **`nad_message.body` size cap** — 4000 chars assumed; confirm or adjust.
3. **Soft vs. hard FK** from `nad_notification.conversation_id`/`message_id` —
   recommend continuing the soft-reference pattern (no FK), confirm.
4. **`promote-document` endpoint** before Document domain ships — confirm it
   should be explicitly stubbed/501 rather than omitted entirely from v1's API
   surface (keeps the contract stable for `nadoumi-web` to build against even
   before it's functional).

---

## Self-review (brainstorming skill checklist)

- **Placeholder scan**: no TBD/TODO left; every open question is an explicit
  numbered DECISION REQUIRED item.
- **Internal consistency**: §4's DDL matches §5's module/API description;
  attachment storage decision (media_asset_id) is stated once and referenced,
  not redefined; migration ranges cross-checked against the sibling designs'
  stated ranges (V63–V70 Application, V71–V78 Document) — no overlap.
- **Scope check**: single module, single implementation plan once approved —
  does not require decomposition.
- **Ambiguity check**: "conversation_type" extensibility point for Support is
  stated as a hook, explicitly not a commitment on Support's behalf, to avoid
  the two designs contradicting each other if Support goes a different way.
