import type { ApplicantDto, Page, ScholarshipCard, StudentApplicationDto } from '~/types/catalog'
import type { DocumentStatus } from '~/constants/onboarding'
import { isSubmitted } from '~/utils/applicationView'

const APPLICATION_PREVIEW_COUNT = 5
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
  const allApplications = computed<StudentApplicationDto[]>(() => applicationsData.value ?? [])
  const latest = computed(() => allApplications.value
    .filter((a: StudentApplicationDto) => isSubmitted(a.currentStatus))
    .sort((a: StudentApplicationDto, b: StudentApplicationDto) => (b.submittedAt ?? '').localeCompare(a.submittedAt ?? ''))[0] ?? null)
  const draft = computed(() => allApplications.value.find((a: StudentApplicationDto) => a.currentStatus === 'DRAFT') ?? null)
  const applications = computed(() => (applicationsData.value ?? []).filter(a => isSubmitted(a.currentStatus)).slice(0, APPLICATION_PREVIEW_COUNT))

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
    focus: reactive({ latest, draft, pending: applicationsPending }),
    applications: reactive({ items: applications, pending: applicationsPending, error: applicationsError }),
    activity: reactive({ items: activity, pending: activityPending, error: activityError }),
  }
}
