# Nadoumi — Domain Events (catalogue)

**Status:** STUB — the authoritative list of business events. Fleshed out as each
domain lands (see `docs/PLATFORM_ARCHITECTURE.md` §8). Every event below is
written to `nad_outbox_event` **in the same transaction** as the state change that
produced it; a poller drains the outbox into `NotificationService` + the SSE
fan-out. Consumers must be idempotent (`nad_notification_delivery` uniqueness +
`nad_outbox_event.id` de-dup).

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
| `ScholarshipPublished` | scholarship | status → `PUBLISHED` | notify targeted students (eligibility/opt-in) |
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
