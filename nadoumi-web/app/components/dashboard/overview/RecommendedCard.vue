<script setup lang="ts">
import type { ScholarshipCard as ScholarshipCardDto } from '~/types/catalog'

defineProps<{ items: ScholarshipCardDto[], pending: boolean, error: unknown }>()

const { t } = useI18n()
const localePath = useLocalePath()
</script>

<template>
  <SectionCard :title="t('dashboard.home.recommended')">
    <template #actions>
      <NTextLink :to="localePath('/scholarships')">{{ t('dashboard.home.viewAll') }}</NTextLink>
    </template>
    <div v-if="pending" class="grid gap-3">
      <NSkeleton v-for="n in 3" :key="n" class="h-20 w-full rounded-xl" />
    </div>
    <NAlert v-else-if="error" tone="danger">{{ t('errors.loadSection') }}</NAlert>
    <p v-else-if="!items.length" class="py-4 text-center text-sm text-slate-500">{{ t('dashboard.home.recommendedEmpty') }}</p>
    <ul v-else class="grid gap-3">
      <li v-for="s in items" :key="s.id">
        <ScholarshipListItem :scholarship="s" />
      </li>
    </ul>
  </SectionCard>
</template>
