<script setup lang="ts">
import type { StudentApplicationDto } from '~/types/catalog'

definePageMeta({ layout: 'dashboard', middleware: ['auth', 'onboarding'] })

const { t, locale } = useI18n()
const localePath = useLocalePath()
const { list } = useApplications()

const dtf = computed(() => new Intl.DateTimeFormat(locale.value, { dateStyle: 'medium' }))
const formatDate = (iso: string) => dtf.value.format(new Date(iso))

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

const typeLabel = (type: string | null) => (type ? t(`dashboard.applications.type.${type}`, t('dashboard.applications.type.UNKNOWN')) : t('dashboard.applications.type.UNKNOWN'))

useSeo(t('dashboard.applications.title'), t('dashboard.applications.blurb'))
</script>

<template>
  <div class="grid gap-6">
    <header>
      <h1 class="font-display text-xl font-bold text-slate-900">{{ t('dashboard.applications.title') }}</h1>
      <p class="mt-1 text-sm text-slate-500">{{ t('dashboard.applications.blurb') }}</p>
    </header>

    <AsyncState :pending="pending" :error="error" :empty="!items.length">
      <template #loading>
        <div class="grid gap-3" data-test="applications-loading">
          <NSkeleton class="h-20 w-full rounded-xl" />
          <NSkeleton class="h-20 w-full rounded-xl" />
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

      <ul class="grid gap-3">
        <li v-for="a in items" :key="a.id">
          <NuxtLink
            :to="localePath(`/dashboard/applications/${a.id}`)"
            class="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-200 bg-white p-4 no-underline transition-colors hover:border-brand-300"
          >
            <span class="min-w-0">
              <span class="block font-semibold text-slate-900">{{ typeLabel(a.applicationType) }}</span>
              <span class="mt-0.5 block text-sm text-slate-500">
                <template v-if="a.currentStageName">{{ a.currentStageName }}</template>
                <template v-if="a.submittedAt">
                  <span v-if="a.currentStageName"> · </span>{{ t('dashboard.applications.submittedOn', { date: formatDate(a.submittedAt) }) }}
                </template>
              </span>
            </span>
            <ApplicationStatusBadge :status="a.currentStatus" />
          </NuxtLink>
        </li>
      </ul>
    </AsyncState>
  </div>
</template>
