import type { ChecklistItemDto } from '~/types/checklist'

/** Uploaded and not turned down: staff have it, or will review it. */
const ATTACHED: readonly ChecklistItemDto['status'][] = ['SUBMITTED', 'IN_REVIEW', 'VERIFIED']

export function isAttached(item: ChecklistItemDto): boolean {
  return ATTACHED.includes(item.status)
}

/** True when every mandatory row has a usable document. An empty checklist asks for nothing. */
export function allMandatoryAttached(items: ChecklistItemDto[]): boolean {
  return items.every(item => !item.mandatory || isAttached(item))
}

/** A rejected or expired document is replaced with a new version; otherwise a new document is created. */
export function replacesExisting(item: ChecklistItemDto): boolean {
  return item.documentId !== null && (item.status === 'REJECTED' || item.status === 'EXPIRED')
}
