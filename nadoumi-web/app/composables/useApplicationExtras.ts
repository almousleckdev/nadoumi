import type { InterviewDto, LetterDto, LetterKind } from '~/types/applicationExtras'

/** Interviews and the post-admission documents of one application, via the BFF proxy. */
export function useApplicationExtras(applicationId: MaybeRefOrGetter<number>) {
  const { studentFetch } = useApi()
  const base = () => `applications/${toValue(applicationId)}`

  return {
    interviews: () => studentFetch<InterviewDto[]>(`${base()}/interviews`),
    letters: () => studentFetch<LetterDto[]>(`${base()}/letters`),
    /** A letter is private behind the session (and the service fee), so it is fetched as a blob. */
    letter: (kind: LetterKind) => studentFetch<Blob>(`${base()}/letters/${kind}`, { responseType: 'blob' }),
  }
}
