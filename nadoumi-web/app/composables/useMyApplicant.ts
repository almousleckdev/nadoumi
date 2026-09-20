import type { ApplicantDto } from '~/types/catalog'

/**
 * The signed-in student's applicant list and their currently active one.
 * Shared across the dashboard shell (header) and any dashboard page via one
 * `useLazyAsyncData` key ('dash-applicants') — Nuxt dedupes calls sharing a
 * key, so mounting the header alongside a page that also needs this data
 * fires a single request, not one each.
 */
export function useMyApplicant() {
  const { activeApplicantId } = useSession()
  const { listMine } = useApplicant()
  const { data, pending, error, refresh } = useLazyAsyncData<ApplicantDto[]>(
    'dash-applicants', () => listMine(), { default: () => [] },
  )

  const primary = computed<ApplicantDto | null>(() =>
    data.value?.find(a => a.id === activeApplicantId.value) ?? data.value?.[0] ?? null)

  return { applicants: data, pending, error, primary, refresh }
}
