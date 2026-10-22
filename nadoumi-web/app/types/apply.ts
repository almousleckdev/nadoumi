/** What a student is applying for; resolved from the `programme` or `scholarship` query on the apply page. */
export type ApplyTarget =
  | { kind: 'programme', id: number, slug: string, title: string, universityName: string | null, intakes: { id: number, term: string }[] }
  | { kind: 'scholarship', id: number, slug: string, title: string }

/** Body of `POST /api/student/applications`. A scholarship has no programme or intake. */
export interface StartApplicationBody {
  applicantId: number
  applicationType: 'PROGRAM_ONLY' | 'SCHOLARSHIP_ONLY'
  programId?: number
  scholarshipId?: number
  intakeId?: number
}
