<script setup lang="ts">
import type { StudentApplicationDto } from '~/types/catalog'
import { isClosed, safeTimeline } from '~/utils/applicationView'
import { formatTimestamp } from '~/utils/dates'

definePageMeta({ layout: 'dashboard', middleware: ['auth', 'onboarding'] })

const { t, te, locale } = useI18n()
const localePath = useLocalePath()
const route = useRoute()
const { get, formPreview } = useApplications()
const { busy, error: actionError, run } = useAsyncAction()
const { file: previewFile, open: previewOpen, showBlob } = useDocumentPreview()

const id = Number(route.params.id)
const formatDate = (iso: string) => formatTimestamp(iso, locale.value)

const application = ref<StudentApplicationDto | null>(null)
const pending = ref(true)
const loadError = ref('')
const notFound = ref(false)

async function load() {
  pending.value = true
  loadError.value = ''
  notFound.value = false
  try {
    application.value = await get(id)
  }
  catch (e) {
    const status = (e as { statusCode?: number, status?: number }).statusCode ?? (e as { status?: number }).status
    if (status === 403 || status === 404) notFound.value = true
    else loadError.value = t('errors.loadSection')
  }
  finally {
    pending.value = false
  }
}
await load()
// an unsubmitted application is completed in the step-by-step flow, not here
if (application.value?.currentStatus === 'DRAFT') await navigateTo(localePath(`/apply/${id}`), { replace: true })

const timeline = computed(() => safeTimeline(application.value?.timeline ?? []))
const closed = computed(() => isClosed(application.value?.currentStatus ?? null))
const statusNote = computed(() => {
  const key = `dashboard.applications.statusNote.${application.value?.currentStatus ?? ''}`
  return te(key) ? t(key) : t('dashboard.applications.statusNote.default')
})

const facts = computed(() => {
  const a = application.value
  if (!a) return []
  return [
    { label: t('dashboard.applications.colStudent'), value: a.applicantName },
    { label: t('dashboard.applications.colOpportunity'), value: a.title },
    { label: t('dashboard.applications.university'), value: a.universityName },
    { label: t('dashboard.applications.intake'), value: a.intake && te(`program.intake.${a.intake}`) ? t(`program.intake.${a.intake}`) : a.intake },
    { label: t('dashboard.applications.colStage'), value: a.currentStageName },
    { label: t('dashboard.applications.colSubmitted'), value: a.submittedAt ? formatDate(a.submittedAt) : null },
  ].filter(f => f.value)
})

async function viewForm() {
  await run(async () => {
    showBlob(t('dashboard.applications.formTitle'), await formPreview(id))
    return true
  })
}

useSeo(t('dashboard.applications.detailTitle'), t('dashboard.applications.blurb'))
</script>

<template>
  <div class="grid min-w-0 gap-6">
    <NuxtLink :to="localePath('/dashboard/applications')" class="text-sm font-medium text-slate-700 no-underline hover:underline">
      {{ t('dashboard.applications.backToList') }}
    </NuxtLink>

    <AsyncState :pending="pending" :error="loadError" :empty="notFound">
      <template #loading>
        <div class="grid gap-3" data-test="application-loading">
          <NSkeleton class="h-24 w-full rounded-xl" />
          <NSkeleton class="h-40 w-full rounded-xl" />
        </div>
      </template>
      <template #error>
        <NAlert tone="danger">
          {{ loadError }}
          <button type="button" class="ms-2 font-medium underline" @click="load()">{{ t('common.retry') }}</button>
        </NAlert>
      </template>
      <template #empty>
        <NAlert tone="warning">{{ t('dashboard.applications.notFound') }}</NAlert>
      </template>

      <div v-if="application" class="grid min-w-0 gap-6">
        <div class="rounded-xl border border-slate-200 bg-white p-5">
          <div class="flex flex-wrap items-start justify-between gap-3">
            <div class="min-w-0">
              <h1 class="font-display text-xl font-bold tabular-nums text-slate-900" data-test="application-reference">
                {{ application.reference ?? '' }}
              </h1>
              <p class="mt-1 text-sm font-medium text-slate-700">{{ application.title }}</p>
              <p v-if="application.universityName" class="text-sm text-slate-500">{{ application.universityName }}</p>
            </div>
            <ApplicationStatusBadge :status="application.currentStatus" />
          </div>
          <p class="mt-4 text-sm text-slate-600" data-test="status-note">{{ statusNote }}</p>
        </div>

        <NAlert v-if="actionError" tone="danger">{{ actionError }}</NAlert>

        <SectionCard :title="t('dashboard.applications.detailsTitle')">
          <dl class="grid gap-x-8 gap-y-3 sm:grid-cols-2" data-test="facts">
            <div v-for="f in facts" :key="f.label" class="min-w-0">
              <dt class="text-xs font-medium uppercase tracking-wide text-slate-500">{{ f.label }}</dt>
              <dd class="mt-0.5 break-words text-sm text-slate-900">{{ f.value }}</dd>
            </div>
          </dl>
          <div class="mt-4">
            <NButton variant="secondary" size="sm" :loading="busy" data-test="view-form" @click="viewForm">
              {{ t('dashboard.applications.viewForm') }}
            </NButton>
          </div>
        </SectionCard>

        <SectionCard :title="t('dashboard.applications.paymentTitle')">
          <PaymentPanel :application-id="id" />
          <p class="mt-3 text-xs text-slate-500">{{ t('dashboard.applications.feeNote') }}</p>
        </SectionCard>

        <DocumentChecklistCard v-if="!closed" :application-id="id" :applicant-id="application.applicantId" />

        <SectionCard :title="t('dashboard.applications.timeline')">
          <p v-if="!timeline.length" class="text-sm text-slate-500">{{ t('dashboard.applications.timelineEmpty') }}</p>
          <ol v-else class="grid gap-3" data-test="timeline">
            <li v-for="(key, i) in timeline" :key="i" class="flex items-start gap-3 text-sm text-slate-700">
              <span class="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-slate-400" aria-hidden="true" />
              {{ t(`dashboard.applications.event.${key}`) }}
            </li>
          </ol>
        </SectionCard>
      </div>
    </AsyncState>
    <DocumentViewer v-model="previewOpen" :file="previewFile" />
  </div>
</template>
