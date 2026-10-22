<script setup lang="ts">
import type { UniversityDetail } from '~/types/catalog'
import { linkHref, mapUrl } from '~/utils/universityDisplay'

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

  <DetailSidebarCard
    v-if="university.address || university.website || university.admissionsEmail || university.officePhone"
    :title="t('university.contact')"
  >
    <ul class="space-y-3 text-sm text-slate-700">
      <li v-if="university.address">
        <span class="block text-xs font-medium uppercase tracking-wide text-slate-500">{{ t('university.address') }}</span>
        <span class="block break-words">{{ university.address }}</span>
        <a :href="mapUrl(university.address)" target="_blank" rel="noopener noreferrer" class="text-slate-900 underline">{{ t('university.viewOnMap') }} ↗</a>
      </li>
      <li v-if="university.admissionsEmail" class="break-words">
        <span class="block text-xs font-medium uppercase tracking-wide text-slate-500">{{ t('university.admissionsEmail') }}</span>
        {{ university.admissionsEmail }}
      </li>
      <li v-if="university.officePhone">
        <span class="block text-xs font-medium uppercase tracking-wide text-slate-500">{{ t('university.officePhone') }}</span>
        {{ university.officePhone }}
      </li>
    </ul>
    <NButton
      v-if="university.website"
      :to="university.website"
      variant="secondary"
      size="sm"
      class="mt-4"
      target="_blank"
      rel="noopener noreferrer"
    >{{ t('university.website') }} ↗</NButton>
  </DetailSidebarCard>

  <DetailSidebarCard v-if="university.links?.length" :title="t('university.channels')">
    <ul class="space-y-2 text-sm">
      <li v-for="l in university.links" :key="l.kind" class="break-words">
        <a
          v-if="linkHref(l.value)"
          :href="linkHref(l.value)!"
          target="_blank"
          rel="noopener noreferrer"
          class="font-medium text-slate-900 underline"
        >{{ t(`university.link.${l.kind}`) }} ↗</a>
        <template v-else>
          <span class="text-slate-500">{{ t(`university.link.${l.kind}`) }}:</span>
          <span class="ms-1 font-medium text-slate-900">{{ l.value }}</span>
        </template>
      </li>
    </ul>
  </DetailSidebarCard>
</template>
