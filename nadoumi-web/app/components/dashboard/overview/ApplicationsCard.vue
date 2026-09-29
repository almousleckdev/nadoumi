<script setup lang="ts">
import type { StudentApplicationDto } from '~/types/catalog'

defineProps<{ items: StudentApplicationDto[], pending: boolean, error: unknown }>()

const { t } = useI18n()
const localePath = useLocalePath()
</script>

<template>
  <SectionCard :title="t('dashboard.home.applications')">
    <template #actions>
      <NuxtLink :to="localePath('/dashboard/applications')" class="text-sm font-medium text-brand-700 hover:underline">
        {{ t('dashboard.home.viewAll') }}
      </NuxtLink>
    </template>
    <NSkeleton v-if="pending" class="h-20 w-full rounded-lg" />
    <NAlert v-else-if="error" tone="danger">{{ t('errors.loadSection') }}</NAlert>
    <div v-else-if="!items.length" class="grid gap-3 py-2 text-center">
      <p class="text-sm text-slate-500">{{ t('dashboard.home.applicationsEmpty') }}</p>
      <div>
        <NButton size="sm" :to="localePath('/scholarships')">{{ t('dashboard.home.browseScholarships') }}</NButton>
      </div>
    </div>
    <ul v-else class="grid gap-3" data-test="applications-summary">
      <li v-for="a in items" :key="a.id">
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
</template>
