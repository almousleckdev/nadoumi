export type DocumentStatus = 'done' | 'pending' | 'attention'

/** Badge tone for each document status. */
export const DOCUMENT_STATUS_TONES = { done: 'success', pending: 'neutral', attention: 'warning' } as const

/** Onboarding section keys as the server reports them. */
export const SECTION_PHOTO = 'PHOTO'
export const SECTION_PASSPORT = 'PASSPORT'
