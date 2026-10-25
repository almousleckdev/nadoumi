<script setup lang="ts">
import type { StudentApplicationDto } from '~/types/catalog'
import { applicationStats, isDraft, isSubmitted } from '~/utils/applicationView'
import { formatTimestamp } from '~/utils/dates'

definePageMeta({ layout: 'dashboard', middleware: ['auth', 'onboarding'] })

const { t, locale } = useI18n()
const localePath = useLocalePath()
const { list } = useApplications()

const formatDate = (iso: string | null) => (iso ? formatTimestamp(iso, locale.value) : '')

const items = ref<StudentApplicationDto[]>([])
const pending = ref(true)
const error = ref('')

async function load() {
  pending.value = true
  error.value = ''
  try {
    items.value = await list()
  }
  catch {
    error.value = t('errors.loadSection')
  }
  finally {
    pending.value = false
  }
}
await load()

const submitted = computed<StudentApplicationDto[]>(() => items.value.filter((a: StudentApplicationDto) => isSubmitted(a.currentStatus)))
const drafts = computed<StudentApplicationDto[]>(() => items.value.filter((a: StudentApplicationDto) => isDraft(a.currentStatus)))

// the table is generic; inside AsyncState the slot's row type is not inferred, so name it once here
const asApplication = (row: unknown) => row as StudentApplicationDto

const needPayment = computed(() => submitted.value.some((a: StudentApplicationDto) => a.paymentNeeded))
const stats = computed(() => applicationStats(items.value.map((a: StudentApplicationDto) => a.currentStatus)))

const columns = computed(() => [
  { key: 'title', label: t('dashboard.applications.colOpportunity') },
  { key: 'reference', label: t('dashboard.applications.colReference') },
  { key: 'currentStatus', label: t('dashboard.applications.colStatus') },
  { key: 'progress', label: t('dashboard.applications.colProgress') },
  { key: 'submittedAt', label: t('dashboard.applications.colSubmitted') },
])

useSeo(t('dashboard.applications.title'), t('dashboard.applications.blurb'))
</script>

<template>
  <div class="grid min-w-0 gap-6">
    <header>
      <h1 class="font-display text-xl font-bold text-slate-900">{{ t('dashboard.applications.title') }}</h1>
      <p class="mt-1 text-sm text-slate-500">{{ t('dashboard.applications.blurb') }}</p>
    </header>

    <AsyncState :pending="pending" :error="error" :empty="!items.length">
      <template #loading>
        <div class="grid gap-3" data-test="applications-loading">
          <NSkeleton class="h-12 w-full rounded-lg" />
          <NSkeleton class="h-12 w-full rounded-lg" />
        </div>
      </template>
      <template #error>
        <NAlert tone="danger">
          {{ error }}
          <button type="button" class="ms-2 font-medium underline" @click="load()">{{ t('common.retry') }}</button>
        </NAlert>
      </template>
      <template #empty>
        <SectionCard :title="t('dashboard.applications.title')">
          <div class="grid gap-3 py-2 text-center">
            <p class="text-sm text-slate-500">{{ t('dashboard.applications.empty') }}</p>
            <div>
              <NButton size="sm" :to="localePath('/scholarships')">{{ t('dashboard.applications.browse') }}</NButton>
            </div>
          </div>
        </SectionCard>
      </template>

      <div class="grid min-w-0 gap-6">
        <div class="grid grid-cols-2 gap-3 sm:gap-4 xl:grid-cols-4" data-test="applications-stats">
          <StatTile icon="applications" :label="t('dashboard.home.statSubmitted')" :value="stats.submitted" />
          <StatTile icon="clock" :label="t('dashboard.home.statInProgress')" :value="stats.inProgress" />
          <StatTile icon="award" :label="t('dashboard.home.statOffers')" :value="stats.offers" />
          <StatTile icon="documents" :label="t('dashboard.applications.statUnfinished')" :value="stats.drafts" />
        </div>

        <section v-if="drafts.length" class="grid gap-3" data-test="drafts">
          <div>
            <h2 class="font-display text-base font-semibold text-slate-900">{{ t('dashboard.applications.draftsTitle') }}</h2>
            <p class="text-sm text-slate-500">{{ t('dashboard.applications.draftsBlurb') }}</p>
          </div>
          <ul class="grid gap-2">
            <li v-for="d in drafts" :key="d.id">
              <NuxtLink
                :to="localePath(`/apply/${d.id}`)"
                class="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-slate-200 bg-white px-4 py-3 text-sm no-underline hover:bg-slate-50"
              >
                <span class="min-w-0">
                  <span class="block truncate font-medium text-slate-900">{{ d.title }}</span>
                  <span class="block truncate text-slate-500">{{ [d.universityName, d.applicantName].filter(Boolean).join(' · ') }}</span>
                </span>
                <span class="font-semibold text-slate-900">{{ t('dashboard.applications.continue') }}</span>
              </NuxtLink>
            </li>
          </ul>
        </section>

        <NAlert v-if="needPayment" tone="danger" data-test="payment-banner">{{ t('dashboard.applications.paymentNeededBanner') }}</NAlert>
        <p v-if="!submitted.length" class="text-sm text-slate-500" data-test="no-submitted">{{ t('dashboard.applications.noneSubmitted') }}</p>
        <NTable v-else :columns="columns" :rows="submitted" row-key="id" data-test="applications-table">
          <template #cell-reference="{ row }">
            <NuxtLink
              :to="localePath(`/dashboard/applications/${asApplication(row).id}`)"
              class="font-semibold tabular-nums text-slate-900 underline-offset-2 hover:underline"
              data-test="application-reference"
            >{{ asApplication(row).reference }}</NuxtLink>
          </template>
          <template #cell-title="{ row }">
            <span class="block font-medium text-slate-900">{{ asApplication(row).title }}</span>
            <span class="block text-slate-500">{{ [asApplication(row).universityName, asApplication(row).applicantName].filter(Boolean).join(' · ') }}</span>
          </template>
          <template #cell-progress="{ row }"><StageMeter :status="asApplication(row).currentStatus" /></template>
          <template #cell-currentStatus="{ row }">
            <ApplicationStatusBadge :status="asApplication(row).currentStatus" />
            <NuxtLink
              v-if="asApplication(row).paymentNeeded"
              :to="localePath(`/dashboard/applications/${asApplication(row).id}`)"
              class="mt-1 block text-xs font-semibold text-red-600 underline-offset-2 hover:underline"
              data-test="payment-needed"
            >{{ t('dashboard.applications.paymentNeeded') }}</NuxtLink>
            <span class="mt-1 block text-xs text-slate-500">{{ asApplication(row).currentStageName }}</span>
          </template>
          <template #cell-submittedAt="{ row }">{{ formatDate(asApplication(row).submittedAt) }}</template>
        </NTable>
        <p v-if="submitted.length" class="text-xs text-slate-500">{{ t('dashboard.applications.feeNote') }}</p>
      </div>
    </AsyncState>
  </div>
</template>
