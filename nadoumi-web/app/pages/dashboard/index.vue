<script setup lang="ts">
import type { ApplicantDto } from '~/types/catalog'

definePageMeta({ layout: 'dashboard', middleware: ['auth', 'onboarding'] })
const { t } = useI18n()
const localePath = useLocalePath()
const { user } = useSession()
const { listMine } = useApplicant()

const { data, pending, error } = await useAsyncData('dash-applicants', () => listMine())
const mine = computed<ApplicantDto[]>(() => data.value ?? [])
const primary = computed(() => mine.value[0] ?? null)
const loadError = computed(() => (error.value ? t('auth.genericError') : ''))

const PROFILE_FIELDS: (keyof ApplicantDto)[] = [
  'givenName', 'familyName', 'dob', 'nationality', 'passportNo', 'email', 'phone',
]

function completeness(a: ApplicantDto): number {
  const filled = PROFILE_FIELDS.filter(k => Boolean(a[k])).length
  return Math.round((filled / PROFILE_FIELDS.length) * 100)
}

const comingSoon = computed(() => [
  { key: 'apps', title: t('dashboard.sectionApplications') },
  { key: 'activity', title: t('dashboard.sectionActivity') },
  { key: 'notifications', title: t('dashboard.sectionNotifications') },
])

useSeo(t('dashboard.nav.overview'), t('dashboard.overviewBlurb'))
</script>

<template>
  <div class="grid gap-6">
    <header class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h1 class="font-display text-2xl font-bold text-slate-900">
          {{ t('dashboard.welcome', { name: user?.nickName ?? user?.username ?? '' }) }}
        </h1>
        <p class="mt-1 text-sm text-slate-500">{{ t('dashboard.overviewBlurb') }}</p>
      </div>
      <span
        v-if="primary"
        class="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white px-3 py-1 text-sm font-medium text-slate-700"
      >
        <span class="h-2 w-2 rounded-full bg-brand-500" />
        {{ primary.givenName }} {{ primary.familyName }}
      </span>
    </header>

    <AsyncState :pending="pending" :error="loadError">
      <SectionCard v-if="!primary" :title="t('dashboard.createProfileTitle')">
        <p class="text-sm text-slate-600">{{ t('dashboard.createProfileBlurb') }}</p>
        <NButton class="mt-4" :to="localePath('/dashboard/profile')">{{ t('dashboard.quickProfile') }}</NButton>
      </SectionCard>

      <div v-else class="grid gap-6">
        <div class="grid gap-6 sm:grid-cols-2">
          <SectionCard :title="t('dashboard.completeness')">
            <div class="flex items-center gap-3">
              <div class="h-2 flex-1 rounded-full bg-slate-100">
                <div class="h-2 rounded-full bg-brand-500" :style="{ width: `${completeness(primary)}%` }" />
              </div>
              <span class="text-sm font-semibold text-slate-700">{{ completeness(primary) }}%</span>
            </div>
          </SectionCard>

          <SectionCard :title="t('dashboard.onboardingProgress')">
            <p class="text-sm text-slate-600">
              {{ completeness(primary) }}% · {{ t('dashboard.overviewBlurb') }}
            </p>
            <div class="mt-3 flex gap-2">
              <NButton size="sm" :to="localePath('/dashboard/profile')">{{ t('dashboard.quickProfile') }}</NButton>
              <NButton size="sm" variant="secondary" :to="localePath('/dashboard/education')">
                {{ t('dashboard.quickEducation') }}
              </NButton>
            </div>
          </SectionCard>
        </div>

        <div class="grid gap-6 sm:grid-cols-3">
          <SectionCard v-for="s in comingSoon" :key="s.key" :title="s.title">
            <p class="text-sm text-slate-500">{{ t('dashboard.comingSoon') }}</p>
          </SectionCard>
        </div>
      </div>
    </AsyncState>
  </div>
</template>
