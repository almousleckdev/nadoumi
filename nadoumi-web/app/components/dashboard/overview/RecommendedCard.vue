<script setup lang="ts">
import type { ScholarshipCard as ScholarshipCardDto } from '~/types/catalog'

defineProps<{ items: ScholarshipCardDto[], pending: boolean, error: unknown }>()

const { t } = useI18n()
const localePath = useLocalePath()

const columns = computed(() => [
  { key: 'title', label: t('dashboard.home.colScholarship') },
  { key: 'country', label: t('dashboard.home.colCountry') },
  { key: 'fundingModel', label: t('dashboard.home.colFunding') },
  { key: 'deadline', label: t('dashboard.home.colDeadline') },
])
</script>

<template>
  <SectionCard :title="t('dashboard.home.recommended')">
    <template #actions>
      <NTextLink :to="localePath('/scholarships')">{{ t('dashboard.home.viewAll') }}</NTextLink>
    </template>
    <div v-if="pending" class="grid gap-3">
      <NSkeleton v-for="n in 3" :key="n" class="h-10 w-full rounded-lg" />
    </div>
    <NAlert v-else-if="error" tone="danger">{{ t('errors.loadSection') }}</NAlert>
    <p v-else-if="!items.length" class="py-4 text-center text-sm text-slate-500">{{ t('dashboard.home.recommendedEmpty') }}</p>
    <NTable v-else :columns="columns" :rows="items" row-key="id" data-test="recommended-table">
      <template #cell-title="{ row }">
        <NuxtLink :to="localePath(`/scholarships/${row.slug}`)" class="font-medium text-slate-900 underline-offset-2 hover:underline">{{ row.title }}</NuxtLink>
      </template>
      <template #cell-fundingModel="{ row }">{{ t(`scholarships.funding.${row.fundingModel}`) }}</template>
      <template #cell-deadline="{ row }">{{ row.deadline ?? t('catalog.rollingDeadline') }}</template>
    </NTable>
  </SectionCard>
</template>
