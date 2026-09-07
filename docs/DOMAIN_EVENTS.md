# Nadoumi — Domain Events (catalogue)

**Status:** PARTIAL — the authoritative list of business events. Fleshed out as
each domain lands (see `docs/PLATFORM_ARCHITECTURE.md` §8). Every event below is
written to `nad_outbox_event` **in the same transaction** as the state change that
produced it; a poller drains the outbox into `NotificationService` + the SSE
fan-out. Consumers must be idempotent (`nad_notification_delivery` uniqueness +
`nad_outbox_event.id` de-dup).

**Infrastructure + first producers (Step 5, slices 1–3/5) — BUILT:**
`nad_outbox_event` (`V30`) + `OutboxWriter` SPI (`nadoumi-common`) +
`OutboxPollerJob` (`V31`, active). `OutboxToNotificationDispatcher` maps a drained
event → one `nad_notification` per recipient (idempotent via
`nad_notification.source_ref = outbox:<id>:<user>`), rendered from
`nad_notification_template`, then `NotificationDeliveryDispatcher` /
`notificationDispatchJob` (`V34`) sends the EMAIL channel via the `MailSender`
port. **`ContactInquiryReceived`** (from `ContactService`) and
**`ScholarshipPublished`** (from `ScholarshipAdminService`, on the transition to
PUBLISHED) are wired and emit in the producer's own transaction. SSE fan-out
(slice 4) is deferred.

## Envelope

```
nad_outbox_event(
  id, aggregate_type, aggregate_id, type, payload_json,
  status PENDING|PROCESSING|DONE|FAILED, retry_count, created_at,
  processed_at?, last_error?
)
```

`payload_json` carries **IDs and safe scalars only** — never PII, never
confidential fields (`docs/SECURITY.md` §6). Renderers/templates resolve details
through authorized queries.

## Event list (grows per step)

| Event `type` | Aggregate | Emitted when | Primary consumers |
| --- | --- | --- | --- |
| `ContactInquiryReceived` ✅ | contact_inquiry | anonymous `POST /api/public/contact` accepts a submission | staff holding `nad:notification:list` — IN_APP + EMAIL (replaces the old direct support-inbox mail) |
| `ScholarshipPublished` ✅ | scholarship | transition to `publish_status = PUBLISHED` (create or update) | v1: staff holding `nad:notification:list` (operational audit). Student targeting by eligibility/opt-in is deferred until a saved-search / opt-in surface exists. |
| `TaskProgressChanged` ✅ | task | any task create / reassignment / priority change / status transition (`TaskService`) | the task creator, its assignee, and `nad:task:approve` holders — IN_APP + EMAIL; the actor is excluded. Recipients travel in the payload as `recipientUserIds`; `OutboxToNotificationDispatcher` uses that list instead of a permission query. |
| `ApplicationSubmitted` | application | student/agent submits (snapshots captured) | assign queue, ack notification |
| `ApplicationStageChanged` | application | workflow transition (engine only) | student in-app + email (sanitised), timeline |
| `ApplicationDecisionRecorded` | application | `nad_application_decision` row added | student notification, workflow guard |
| `DocumentRejected` | document | `verification_status` → `REJECTED` | student notification with `rejection_reason` (SHARED) |
| `DocumentVerified` | document | `verification_status` → `VERIFIED` | checklist recompute, workflow guard |
| `PaymentSettled` | payment | provider confirms / staff marks settled | workflow fee gate, Finance read models |
| `RefundIssued` | payment | refund completed | Finance read models, student notification |
| `MessagePosted` | conversation | `nad_message` insert | recipient SSE ping + notification if offline |

## Rules

- **Transactional**: no event without its state change; no state change silently
  without its event where the table above lists one.
- **At-least-once**: consumers de-dup. Bounded retry with backoff; `FAILED` after
  N attempts raises an operational alert.
- **Sanitised student view**: students see a coarse, safe projection of
  `ApplicationStageChanged` (status label + timestamp), never internal stage
  codes, notes, or assignee identity.
- **No SMS/WhatsApp/PUSH in v1** — channels are schema-ready, dispatch is EMAIL +
  IN_APP only.
