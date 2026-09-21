<script setup lang="ts">
import type { StudentApplicationDto } from '~/types/catalog'
import { isClosed, safeTimeline } from '~/utils/applicationView'

definePageMeta({ layout: 'dashboard', middleware: ['auth', 'onboarding'] })

const { t, locale } = useI18n()
const localePath = useLocalePath()
const route = useRoute()
const { get, submit, withdraw } = useApplications()
const { busy, notice, error: actionError, run } = useAsyncAction()

const id = Number(route.params.id)
const dtf = computed(() => new Intl.DateTimeFormat(locale.value, { dateStyle: 'medium' }))
const formatDate = (iso: string) => dtf.value.format(new Date(iso))

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

const timeline = computed(() => safeTimeline(application.value?.timeline ?? []))
const isDraft = computed(() => application.value?.currentStatus === 'DRAFT')
const canWithdraw = computed(() => !isClosed(application.value?.currentStatus ?? null))

async function onSubmit() {
  const updated = await run(() => submit(id), t('dashboard.applications.submitted'))
  if (updated) application.value = updated
}

const withdrawing = ref(false)
const reason = ref('')
const reasonMissing = ref(false)
async function onWithdraw() {
  reasonMissing.value = !reason.value.trim()
  if (reasonMissing.value) return
  const updated = await run(() => withdraw(id, reason.value.trim()), t('dashboard.applications.withdrawnNotice'))
  if (updated) {
    application.value = updated
    withdrawing.value = false
    reason.value = ''
  }
}

useSeo(t('dashboard.applications.detailTitle'), t('dashboard.applications.blurb'))
</script>

<template>
  <div class="grid gap-6">
    <NuxtLink :to="localePath('/dashboard/applications')" class="text-sm font-medium text-brand-700 no-underline hover:underline">
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

      <div v-if="application" class="grid gap-6">
        <div class="rounded-xl border border-slate-200 bg-white p-5">
          <div class="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h1 class="font-display text-xl font-bold text-slate-900">
                {{ t(`dashboard.applications.type.${application.applicationType ?? 'UNKNOWN'}`, t('dashboard.applications.type.UNKNOWN')) }}
              </h1>
              <p v-if="application.currentStageName" class="mt-1 text-sm text-slate-500">
                {{ t('dashboard.applications.currentStage', { stage: application.currentStageName }) }}
              </p>
              <p v-if="application.submittedAt" class="mt-0.5 text-sm text-slate-500">
                {{ t('dashboard.applications.submittedOn', { date: formatDate(application.submittedAt) }) }}
              </p>
            </div>
            <ApplicationStatusBadge :status="application.currentStatus" />
          </div>
        </div>

        <NAlert v-if="notice" tone="success">{{ notice }}</NAlert>
        <NAlert v-if="actionError" tone="danger">{{ actionError }}</NAlert>

        <SectionCard :title="t('dashboard.applications.timeline')">
          <p v-if="!timeline.length" class="text-sm text-slate-500">{{ t('dashboard.applications.timelineEmpty') }}</p>
          <ol v-else class="grid gap-3" data-test="timeline">
            <li v-for="(key, i) in timeline" :key="i" class="flex items-start gap-3 text-sm text-slate-700">
              <span class="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-brand-500" aria-hidden="true" />
              {{ t(`dashboard.applications.event.${key}`) }}
            </li>
          </ol>
        </SectionCard>

        <SectionCard v-if="isDraft || canWithdraw" :title="t('dashboard.applications.actions')">
          <div class="grid gap-4">
            <div v-if="isDraft">
              <p class="mb-2 text-sm text-slate-500">{{ t('dashboard.applications.submitHint') }}</p>
              <NButton :loading="busy" data-test="submit" @click="onSubmit">{{ t('dashboard.applications.submit') }}</NButton>
            </div>

            <div v-if="canWithdraw">
              <NButton v-if="!withdrawing" variant="secondary" data-test="withdraw-start" @click="withdrawing = true">
                {{ t('dashboard.applications.withdraw') }}
              </NButton>
              <div v-else class="grid gap-2" data-test="withdraw-form">
                <label for="withdraw-reason" class="text-sm font-medium text-slate-700">{{ t('dashboard.applications.withdrawReason') }}</label>
                <NTextarea id="withdraw-reason" v-model="reason" :rows="3" :invalid="reasonMissing" :maxlength="500" />
                <p v-if="reasonMissing" class="text-sm text-red-600">{{ t('validation.required') }}</p>
                <div class="flex gap-2">
                  <NButton :loading="busy" data-test="withdraw-confirm" @click="onWithdraw">{{ t('dashboard.applications.withdrawConfirm') }}</NButton>
                  <NButton variant="ghost" :disabled="busy" @click="withdrawing = false">{{ t('common.cancel') }}</NButton>
                </div>
              </div>
            </div>
          </div>
        </SectionCard>
      </div>
    </AsyncState>
  </div>
</template>
