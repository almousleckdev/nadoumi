import type { ApplicantDto, Page, ScholarshipCard } from '~/types/catalog'
import type { DocumentStatus } from '~/constants/onboarding'

const APPLICATION_PREVIEW_COUNT = 3
const DEADLINE_FETCH_SIZE = 8
const DEADLINE_PREVIEW_COUNT = 4
const RECOMMENDED_COUNT = 4
const ACTIVITY_COUNT = 5

export function useDashboardOverview(primary: Ref<ApplicantDto | null | undefined>) {
  const { publicGet } = useApi()
  const { photoUrl, passportStatus } = useApplicant()
  const { list: listNotifications } = useNotifications()
  const { list: listApplications } = useApplications()

  const { data: photo, pending: photoPending } = useLazyAsyncData(
    'dash-photo',
    () => (primary.value ? photoUrl(primary.value.id).catch(() => null) : Promise.resolve(null)),
    { watch: [primary], default: () => null },
  )
  const { data: passport, pending: passportPending } = useLazyAsyncData(
    'dash-passport',
    () => (primary.value ? passportStatus(primary.value.id).catch(() => null) : Promise.resolve(null)),
    { watch: [primary], default: () => null },
  )
  const docsPending = computed(() => photoPending.value || passportPending.value)
  const photoStatus = computed<DocumentStatus>(() => (photo.value?.url ? 'done' : 'pending'))
  const passportDocStatus = computed<DocumentStatus>(() => {
    const s = passport.value
    if (!s?.passportNo) return 'pending'
    return s.scanUploaded && s.validForAdmission && s.matchesProfile ? 'done' : 'attention'
  })

  const { data: deadlineData, pending: deadlinesPending, error: deadlinesError } = useLazyAsyncData(
    'dash-deadlines',
    () => publicGet<Page<ScholarshipCard>>('scholarships', { sort: 'deadline', size: DEADLINE_FETCH_SIZE }),
    { default: () => null },
  )
  const deadlines = computed(() =>
    (deadlineData.value?.content ?? [])
      .filter((s) => { const d = daysUntilDeadline(s.deadline); return d !== null && d >= 0 })
      .slice(0, DEADLINE_PREVIEW_COUNT))

  const { data: recommendedData, pending: recommendedPending, error: recommendedError } = useLazyAsyncData(
    'dash-recommended',
    () => publicGet<Page<ScholarshipCard>>('scholarships', { recommended: true, size: RECOMMENDED_COUNT }),
    { default: () => null },
  )
  const recommended = computed(() => recommendedData.value?.content ?? [])

  const { data: applicationsData, pending: applicationsPending, error: applicationsError } = useLazyAsyncData(
    'dash-applications',
    () => listApplications(),
    { default: () => null },
  )
  const applications = computed(() => (applicationsData.value ?? []).slice(0, APPLICATION_PREVIEW_COUNT))

  const { data: activityData, pending: activityPending, error: activityError } = useLazyAsyncData(
    'dash-activity',
    () => listNotifications({ page: 0, size: ACTIVITY_COUNT }),
    { default: () => null },
  )
  const activity = computed(() => activityData.value?.content ?? [])

  return {
    documents: reactive({ pending: docsPending, photoStatus, passportStatus: passportDocStatus }),
    deadlines: reactive({ items: deadlines, pending: deadlinesPending, error: deadlinesError }),
    recommended: reactive({ items: recommended, pending: recommendedPending, error: recommendedError }),
    applications: reactive({ items: applications, pending: applicationsPending, error: applicationsError }),
    activity: reactive({ items: activity, pending: activityPending, error: activityError }),
  }
}
