<script setup lang="ts">
import type { StudentApplicationDto } from '~/types/catalog'
import { formatTimestamp } from '~/utils/dates'

/**
 * The application the student is following: where it is on the path and what happens next.
 * With nothing submitted it points at the unfinished application, or at the catalogue.
 */
const props = defineProps<{ latest: StudentApplicationDto | null, draft: StudentApplicationDto | null, pending: boolean }>()

const { t, te, locale } = useI18n()
const localePath = useLocalePath()

const note = computed(() => {
  const key = `dashboard.applications.statusNote.${props.latest?.currentStatus ?? ''}`
  return te(key) ? t(key) : t('dashboard.applications.statusNote.default')
})
</script>

<template>
  <SectionCard :title="t('dashboard.home.focusTitle')">
    <template v-if="latest" #actions>
      <NTextLink :to="localePath('/dashboard/applications')">{{ t('dashboard.home.allApplications') }}</NTextLink>
    </template>
    <NSkeleton v-if="pending" class="h-28 w-full rounded-lg" />

    <div v-else-if="latest" class="grid gap-5" data-test="focus-application">
      <div class="flex flex-wrap items-start justify-between gap-3">
        <div class="min-w-0">
          <p class="font-display text-lg font-semibold text-slate-900">{{ latest.title }}</p>
          <p class="text-sm text-slate-600">{{ latest.universityName }}</p>
          <p class="mt-1 text-xs tabular-nums text-slate-500">
            {{ [latest.reference, latest.submittedAt ? t('dashboard.applications.submittedOn', { date: formatTimestamp(latest.submittedAt, locale) }) : null].filter(Boolean).join(' | ') }}
          </p>
        </div>
        <NButton size="sm" :to="localePath(`/dashboard/applications/${latest.id}`)">{{ t('dashboard.home.viewApplication') }}</NButton>
      </div>
      <ApplicationTracker :status="latest.currentStatus" />
      <div v-if="latest.paymentNeeded" class="flex flex-wrap items-center justify-between gap-3 rounded-lg bg-red-50 px-4 py-3" data-test="focus-payment-needed">
        <p class="text-sm font-medium text-red-700">{{ t('dashboard.applications.paymentNeededNote') }}</p>
        <NButton size="sm" :to="localePath(`/dashboard/applications/${latest.id}`)">{{ t('dashboard.applications.payAgain') }}</NButton>
      </div>
      <p v-else class="rounded-lg bg-slate-50 px-4 py-3 text-sm text-slate-700">{{ note }}</p>
    </div>

    <div v-else-if="draft" class="grid gap-3" data-test="focus-draft">
      <p class="text-sm text-slate-600">{{ t('dashboard.home.focusDraft', { title: draft.title ?? '' }) }}</p>
      <div><NButton size="sm" :to="localePath(`/apply/${draft.id}`)">{{ t('dashboard.applications.continue') }}</NButton></div>
    </div>

    <div v-else class="grid gap-3" data-test="focus-empty">
      <p class="text-sm text-slate-600">{{ t('dashboard.home.focusEmpty') }}</p>
      <div class="flex flex-wrap gap-2">
        <NButton size="sm" :to="localePath('/scholarships')">{{ t('dashboard.home.browseScholarships') }}</NButton>
        <NButton size="sm" variant="secondary" :to="localePath('/programs')">{{ t('dashboard.home.browseProgrammes') }}</NButton>
      </div>
    </div>
  </SectionCard>
</template>
