/** `/api/student/applications/{id}/interviews`: one interview Nadoumi scheduled. */
export interface InterviewDto {
  id: number
  /** ISO-8601 instant in UTC. */
  startsAt: string
  durationMinutes: number
  platform: string | null
  meetingUrl: string
  instructions: string | null
  status: 'SCHEDULED' | 'CANCELLED'
}

export type LetterKind = 'ADMISSION_LETTER' | 'JW202'

/** `/api/student/applications/{id}/letters`: a document released after the service fee. */
export interface LetterDto {
  kind: LetterKind
  /** `null` while the letter is locked behind the service fee. */
  fileName: string | null
  locked: boolean
  uploadedAt: string | null
}
