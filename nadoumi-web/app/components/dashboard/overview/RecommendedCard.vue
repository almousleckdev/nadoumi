<script setup lang="ts">
import type { ScholarshipCard as ScholarshipCardDto } from '~/types/catalog'

defineProps<{ items: ScholarshipCardDto[], pending: boolean, error: unknown }>()

const { t } = useI18n()
const localePath = useLocalePath()
</script>

<template>
  <SectionCard :title="t('dashboard.home.recommended')">
    <template #actions>
      <NuxtLink :to="localePath('/scholarships')" class="text-sm font-medium text-brand-700 hover:underline">
        {{ t('dashboard.home.viewAll') }}
      </NuxtLink>
    </template>
    <NSkeleton v-if="pending" class="h-40 w-full rounded-lg" />
    <NAlert v-else-if="error" tone="danger">{{ t('errors.loadSection') }}</NAlert>
    <p v-else-if="!items.length" class="py-4 text-center text-sm text-slate-500">{{ t('dashboard.home.recommendedEmpty') }}</p>
    <div v-else class="grid gap-4 sm:grid-cols-2">
      <ScholarshipCard v-for="s in items" :key="s.id" :scholarship="s" />
    </div>
  </SectionCard>
</template>
