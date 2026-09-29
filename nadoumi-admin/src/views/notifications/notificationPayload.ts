const HIDDEN_KEYS = new Set(['recipientUserIds'])
const DELIVERED = new Set(['SENT', 'DELIVERED'])
const FAILED = new Set(['FAILED', 'BOUNCED'])

export function parsePayload(dataJson: string | null | undefined): Record<string, unknown> {
  if (!dataJson) return {}
  try {
    const parsed: unknown = JSON.parse(dataJson)
    return parsed !== null && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed as Record<string, unknown> : {}
  }
  catch {
    return {}
  }
}

export function payloadRows(payload: Record<string, unknown>): { k: string, v: string }[] {
  return Object.entries(payload)
    .filter(([k, v]) => !HIDDEN_KEYS.has(k) && v !== '' && v != null)
    .map(([k, v]) => ({ k, v: String(v) }))
}

export function taskIdOf(payload: Record<string, unknown>): number | null {
  const id = Number(payload.taskId)
  return Number.isFinite(id) && id > 0 ? id : null
}

export function deliveryTone(status: string): string {
  if (DELIVERED.has(status)) return 'ACTIVE'
  if (FAILED.has(status)) return 'FAILED'
  return 'PENDING'
}
