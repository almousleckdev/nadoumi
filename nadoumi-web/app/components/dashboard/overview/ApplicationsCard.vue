<script setup lang="ts">
import type { StudentApplicationDto } from '~/types/catalog'

defineProps<{ items: StudentApplicationDto[], pending: boolean, error: unknown }>()

const { t } = useI18n()
const localePath = useLocalePath()

const columns = computed(() => [
  { key: 'reference', label: t('dashboard.applications.colReference') },
  { key: 'title', label: t('dashboard.applications.colOpportunity') },
  { key: 'currentStatus', label: t('dashboard.applications.colStatus') },
])
</script>

<template>
  <SectionCard :title="t('dashboard.home.applications')">
    <template #actions>
      <NTextLink :to="localePath('/dashboard/applications')">
        {{ t('dashboard.home.viewAll') }}
      </NTextLink>
    </template>
    <NSkeleton v-if="pending" class="h-20 w-full rounded-lg" />
    <NAlert v-else-if="error" tone="danger">{{ t('errors.loadSection') }}</NAlert>
    <div v-else-if="!items.length" class="grid gap-3 py-2 text-center">
      <p class="text-sm text-slate-500">{{ t('dashboard.home.applicationsEmpty') }}</p>
      <div>
        <NButton size="sm" :to="localePath('/scholarships')">{{ t('dashboard.home.browseScholarships') }}</NButton>
      </div>
    </div>
    <NTable v-else :columns="columns" :rows="items" row-key="id" data-test="applications-summary">
      <template #cell-reference="{ row }">
        <NuxtLink :to="localePath(`/dashboard/applications/${row.id}`)" class="font-display font-semibold tabular-nums text-slate-900 underline-offset-2 hover:underline">{{ row.reference }}</NuxtLink>
      </template>
      <template #cell-title="{ row }">
        <span class="block font-medium text-slate-900">{{ row.title }}</span>
        <span v-if="row.universityName" class="block text-slate-500">{{ row.universityName }}</span>
      </template>
      <template #cell-currentStatus="{ row }"><ApplicationStatusBadge :status="row.currentStatus" /></template>
    </NTable>
  </SectionCard>
</template>
