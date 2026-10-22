<script setup lang="ts">
import type { UniversityDetail } from '~/types/catalog'

defineProps<{ university: UniversityDetail }>()

const { t } = useI18n()
</script>

<template>
  <DetailSidebarCard v-if="university.rankings.length" :title="t('university.rankings')">
    <ul class="space-y-2 text-sm">
      <li v-for="r in university.rankings" :key="r.id" class="flex items-baseline justify-between gap-3">
        <span class="text-slate-700">{{ r.source }}<span v-if="r.rankYear" class="text-slate-500"> · {{ r.rankYear }}</span></span>
        <span class="font-display font-semibold text-slate-900">#{{ r.rankPosition }}</span>
      </li>
    </ul>
  </DetailSidebarCard>

  <DetailSidebarCard v-if="university.website || university.admissionsEmail || university.officePhone" :title="t('university.contact')">
    <ul class="space-y-2 text-sm text-slate-700">
      <li v-if="university.admissionsEmail" class="break-words">{{ t('university.admissionsEmail') }}: {{ university.admissionsEmail }}</li>
      <li v-if="university.officePhone">{{ t('university.officePhone') }}: {{ university.officePhone }}</li>
    </ul>
    <NButton
      v-if="university.website"
      :to="university.website"
      variant="secondary"
      size="sm"
      class="mt-3"
      target="_blank"
      rel="noopener"
    >{{ t('university.website') }} ↗</NButton>
  </DetailSidebarCard>
</template>
