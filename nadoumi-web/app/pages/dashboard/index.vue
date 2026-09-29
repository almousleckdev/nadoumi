<script setup lang="ts">
import type { ApplicantDto } from '~/types/catalog'
import OverviewProfileHeader from '~/components/dashboard/overview/ProfileHeader.vue'
import OverviewApplicationsCard from '~/components/dashboard/overview/ApplicationsCard.vue'
import OverviewRecommendedCard from '~/components/dashboard/overview/RecommendedCard.vue'
import OverviewDocumentsCard from '~/components/dashboard/overview/DocumentsCard.vue'
import OverviewDeadlinesCard from '~/components/dashboard/overview/DeadlinesCard.vue'
import OverviewActivityCard from '~/components/dashboard/overview/ActivityCard.vue'

definePageMeta({ layout: 'dashboard', middleware: ['auth', 'onboarding'] })
const { t } = useI18n()
const localePath = useLocalePath()
const { markWelcomed } = useApplicant()

const { applicants: mine, primary, pending, error } = useMyApplicant()
const loadError = computed(() => (error.value ? t('auth.genericError') : ''))

const showWelcome = computed(() => primary.value?.welcomePending === true)
async function dismissWelcome() {
  const applicant = primary.value
  if (!applicant) return
  mine.value = (mine.value ?? []).map((a: ApplicantDto) => (a.id === applicant.id ? { ...a, welcomePending: false } : a))
  await markWelcomed(applicant.id).catch(() => undefined)
}

const { documents, deadlines, recommended, applications, activity } = useDashboardOverview(primary)

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
        <OverviewProfileHeader :applicant="primary" />

        <div class="grid gap-6 lg:grid-cols-3">
          <div class="grid gap-6 lg:col-span-2">
            <OverviewApplicationsCard v-bind="applications" />
            <OverviewRecommendedCard v-bind="recommended" />
          </div>

          <div class="grid gap-6">
            <OverviewDocumentsCard
              :pending="documents.pending"
              :photo-status="documents.photoStatus"
              :passport-status="documents.passportStatus"
            />
            <OverviewDeadlinesCard v-bind="deadlines" />
            <OverviewActivityCard v-bind="activity" />
          </div>
        </div>
      </div>
    </AsyncState>
  </div>
</template>
