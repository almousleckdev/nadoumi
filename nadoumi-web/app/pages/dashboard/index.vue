<script setup lang="ts">
import type { ApplicantDto, ScholarshipCard as ScholarshipCardDto, Page } from '~/types/catalog'
import { DOCUMENT_STATUS_TONES, type DocumentStatus } from '~/constants/onboarding'

definePageMeta({ layout: 'dashboard', middleware: ['auth', 'onboarding'] })
const { t } = useI18n()
const localePath = useLocalePath()
const { markWelcomed, photoUrl, passportStatus } = useApplicant()
const { publicGet } = useApi()
const { list: listNotifications } = useNotifications()
const { list: listApplications } = useApplications()

const { applicants: mine, primary, pending, error } = useMyApplicant()
const loadError = computed(() => (error.value ? t('auth.genericError') : ''))

const showWelcome = computed(() => primary.value?.welcomePending === true)
async function dismissWelcome() {
  const applicant = primary.value
  if (!applicant) return
  // Optimistic: hide immediately, the server call is best-effort and never shown again either way.
  mine.value = (mine.value ?? []).map((a: ApplicantDto) => (a.id === applicant.id ? { ...a, welcomePending: false } : a))
  await markWelcomed(applicant.id).catch(() => undefined)
}

const PROFILE_FIELDS: (keyof ApplicantDto)[] = [
  'givenName', 'familyName', 'dob', 'nationality', 'passportNo', 'email', 'phone',
]
function completeness(a: ApplicantDto): number {
  const filled = PROFILE_FIELDS.filter(k => Boolean(a[k])).length
  return Math.round((filled / PROFILE_FIELDS.length) * 100)
}

/* ---- Documents summary: real Photo/Passport state, nothing else exists yet ---- */
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

/* ---- Upcoming deadlines: real scholarship catalog, soonest first ---- */
const { data: deadlineData, pending: deadlinesPending, error: deadlinesError } = useLazyAsyncData(
  'dash-deadlines',
  () => publicGet<Page<ScholarshipCardDto>>('scholarships', { sort: 'deadline', size: 8 }),
  { default: () => null },
)
const deadlines = computed(() =>
  (deadlineData.value?.content ?? [])
    .filter(s => { const d = daysUntilDeadline(s.deadline); return d !== null && d >= 0 })
    .slice(0, 4))

/* ---- Recommended scholarships: real catalog, recommended flag ---- */
const { data: recommendedData, pending: recommendedPending, error: recommendedError } = useLazyAsyncData(
  'dash-recommended',
  () => publicGet<Page<ScholarshipCardDto>>('scholarships', { recommended: true, size: 4 }),
  { default: () => null },
)
const recommended = computed(() => recommendedData.value?.content ?? [])

/* ---- Applications: the student's real applications, newest first ---- */
const APPLICATION_PREVIEW_COUNT = 3
const { data: applicationsData, pending: applicationsPending, error: applicationsError } = useLazyAsyncData(
  'dash-applications',
  () => listApplications(),
  { default: () => null },
)
const applications = computed(() => (applicationsData.value ?? []).slice(0, APPLICATION_PREVIEW_COUNT))

/* ---- Recent activity: real notification feed ---- */
const { data: activityData, pending: activityPending, error: activityError } = useLazyAsyncData(
  'dash-activity',
  () => listNotifications({ page: 0, size: 5 }),
  { default: () => null },
)
const activity = computed(() => activityData.value?.content ?? [])

useSeo(t('dashboard.nav.overview'), t('dashboard.overviewBlurb'))
</script>

<template>
  <div class="grid gap-6">
    <WelcomeCelebration v-if="showWelcome && primary" :name="primary.givenName" @dismiss="dismissWelcome" />

    <AsyncState :pending="pending" :error="loadError">
      <template #loading>
        <div class="grid gap-6">
          <NSkeleton class="h-20 w-full rounded-xl" />
          <div class="grid gap-6 lg:grid-cols-3">
            <div class="grid gap-6 lg:col-span-2">
              <NSkeleton class="h-40 w-full rounded-xl" />
              <NSkeleton class="h-56 w-full rounded-xl" />
            </div>
            <div class="grid gap-6">
              <NSkeleton class="h-32 w-full rounded-xl" />
              <NSkeleton class="h-40 w-full rounded-xl" />
              <NSkeleton class="h-40 w-full rounded-xl" />
            </div>
          </div>
        </div>
      </template>

      <SectionCard v-if="!primary" :title="t('dashboard.createProfileTitle')">
        <p class="text-sm text-slate-600">{{ t('dashboard.createProfileBlurb') }}</p>
        <NButton class="mt-4" :to="localePath('/dashboard/profile')">{{ t('dashboard.quickProfile') }}</NButton>
      </SectionCard>

      <div v-else class="grid gap-6">
        <!-- Header: welcome + profile completeness -->
        <div class="rounded-xl border border-slate-200 bg-white p-5">
          <div class="flex flex-wrap items-start justify-between gap-4">
            <div>
              <h1 class="font-display text-xl font-bold text-slate-900">
                {{ t('dashboard.welcome', { name: primary.givenName }) }}
              </h1>
              <p class="mt-1 text-sm text-slate-500">{{ t('dashboard.home.subtitle') }}</p>
            </div>
            <div v-if="completeness(primary) < 100" class="w-full shrink-0 sm:w-56">
              <div class="mb-1 flex items-center justify-between text-xs">
                <span class="font-medium text-slate-600">{{ t('dashboard.completeness') }}</span>
                <span class="font-semibold text-slate-700">{{ completeness(primary) }}%</span>
              </div>
              <div class="h-1.5 rounded-full bg-slate-100">
                <div class="h-1.5 rounded-full bg-brand-500" :style="{ width: `${completeness(primary)}%` }" />
              </div>
              <NButton class="mt-2" size="sm" variant="secondary" :to="localePath('/dashboard/profile')">
                {{ t('dashboard.quickProfile') }}
              </NButton>
            </div>
          </div>
        </div>

        <div class="grid gap-6 lg:grid-cols-3">
          <!-- Main column -->
          <div class="grid gap-6 lg:col-span-2">
            <SectionCard :title="t('dashboard.home.applications')">
              <template #actions>
                <NuxtLink :to="localePath('/dashboard/applications')" class="text-sm font-medium text-brand-700 hover:underline">
                  {{ t('dashboard.home.viewAll') }}
                </NuxtLink>
              </template>
              <NSkeleton v-if="applicationsPending" class="h-20 w-full rounded-lg" />
              <NAlert v-else-if="applicationsError" tone="danger">{{ t('errors.loadSection') }}</NAlert>
              <div v-else-if="!applications.length" class="grid gap-3 py-2 text-center">
                <p class="text-sm text-slate-500">{{ t('dashboard.home.applicationsEmpty') }}</p>
                <div>
                  <NButton size="sm" :to="localePath('/scholarships')">{{ t('dashboard.home.browseScholarships') }}</NButton>
                </div>
              </div>
              <ul v-else class="grid gap-3" data-test="applications-summary">
                <li v-for="a in applications" :key="a.id">
                  <NuxtLink
                    :to="localePath(`/dashboard/applications/${a.id}`)"
                    class="flex items-center justify-between gap-3 text-sm no-underline hover:text-brand-700"
                  >
                    <span class="min-w-0 truncate text-slate-700">
                      {{ t(`dashboard.applications.type.${a.applicationType ?? 'UNKNOWN'}`, t('dashboard.applications.type.UNKNOWN')) }}
                    </span>
                    <ApplicationStatusBadge :status="a.currentStatus" />
                  </NuxtLink>
                </li>
              </ul>
            </SectionCard>

            <SectionCard :title="t('dashboard.home.recommended')">
              <template #actions>
                <NuxtLink :to="localePath('/scholarships')" class="text-sm font-medium text-brand-700 hover:underline">
                  {{ t('dashboard.home.viewAll') }}
                </NuxtLink>
              </template>
              <NSkeleton v-if="recommendedPending" class="h-40 w-full rounded-lg" />
              <NAlert v-else-if="recommendedError" tone="danger">{{ t('errors.loadSection') }}</NAlert>
              <p v-else-if="!recommended.length" class="py-4 text-center text-sm text-slate-500">{{ t('dashboard.home.recommendedEmpty') }}</p>
              <div v-else class="grid gap-4 sm:grid-cols-2">
                <ScholarshipCard v-for="s in recommended" :key="s.id" :scholarship="s" />
              </div>
            </SectionCard>
          </div>

          <!-- Sidebar column -->
          <div class="grid gap-6">
            <SectionCard :title="t('dashboard.home.documents')">
              <template #actions>
                <NuxtLink :to="localePath('/dashboard/documents')" class="text-sm font-medium text-brand-700 hover:underline">
                  {{ t('dashboard.home.manageDocuments') }}
                </NuxtLink>
              </template>
              <div v-if="docsPending" class="grid gap-2">
                <NSkeleton class="h-6 w-full rounded" />
                <NSkeleton class="h-6 w-full rounded" />
              </div>
              <ul v-else class="grid gap-2">
                <li class="flex items-center justify-between text-sm">
                  <span class="text-slate-700">{{ t('dashboard.home.docPhoto') }}</span>
                  <NBadge :tone="DOCUMENT_STATUS_TONES[photoStatus as DocumentStatus]">{{ t(`onboarding.docStatus.${photoStatus}`) }}</NBadge>
                </li>
                <li class="flex items-center justify-between text-sm">
                  <span class="text-slate-700">{{ t('dashboard.home.docPassport') }}</span>
                  <NBadge :tone="DOCUMENT_STATUS_TONES[passportDocStatus as DocumentStatus]">{{ t(`onboarding.docStatus.${passportDocStatus}`) }}</NBadge>
                </li>
              </ul>
            </SectionCard>

            <SectionCard :title="t('dashboard.home.deadlines')">
              <template #actions>
                <NuxtLink :to="localePath('/scholarships')" class="text-sm font-medium text-brand-700 hover:underline">
                  {{ t('dashboard.home.viewAll') }}
                </NuxtLink>
              </template>
              <div v-if="deadlinesPending" class="grid gap-2">
                <NSkeleton class="h-5 w-full rounded" />
                <NSkeleton class="h-5 w-full rounded" />
              </div>
              <NAlert v-else-if="deadlinesError" tone="danger">{{ t('errors.loadSection') }}</NAlert>
              <p v-else-if="!deadlines.length" class="py-2 text-sm text-slate-500">{{ t('dashboard.home.deadlinesEmpty') }}</p>
              <ul v-else class="grid gap-3">
                <li v-for="s in deadlines" :key="s.id">
                  <NuxtLink :to="localePath(`/scholarships/${s.slug}`)" class="flex items-center justify-between gap-2 text-sm hover:text-brand-700">
                    <span class="min-w-0 truncate text-slate-700">{{ s.title }}</span>
                    <span
                      class="shrink-0 font-medium"
                      :class="{
                        'text-red-600': deadlineTone(daysUntilDeadline(s.deadline)) === 'urgent',
                        'text-amber-600': deadlineTone(daysUntilDeadline(s.deadline)) === 'soon',
                        'text-slate-500': deadlineTone(daysUntilDeadline(s.deadline)) === 'ok',
                      }"
                    >{{ t('catalog.deadline', { date: s.deadline }) }}</span>
                  </NuxtLink>
                </li>
              </ul>
            </SectionCard>

            <SectionCard :title="t('dashboard.home.activity')">
              <template #actions>
                <NuxtLink :to="localePath('/dashboard/notifications')" class="text-sm font-medium text-brand-700 hover:underline">
                  {{ t('dashboard.home.viewAll') }}
                </NuxtLink>
              </template>
              <div v-if="activityPending" class="grid gap-2">
                <NSkeleton class="h-5 w-full rounded" />
                <NSkeleton class="h-5 w-full rounded" />
              </div>
              <NAlert v-else-if="activityError" tone="danger">{{ t('errors.loadSection') }}</NAlert>
              <p v-else-if="!activity.length" class="py-2 text-sm text-slate-500">{{ t('dashboard.home.activityEmpty') }}</p>
              <ul v-else class="grid gap-3">
                <li v-for="n in activity" :key="n.id" class="flex items-start gap-2 text-sm">
                  <span class="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full" :class="n.read ? 'bg-transparent' : 'bg-brand-600'" aria-hidden="true" />
                  <span class="min-w-0 flex-1">
                    <span class="block" :class="n.read ? 'text-slate-600' : 'font-medium text-slate-900'">{{ n.title }}</span>
                  </span>
                </li>
              </ul>
            </SectionCard>
          </div>
        </div>
      </div>
    </AsyncState>
  </div>
</template>
