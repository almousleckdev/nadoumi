<script setup lang="ts">
import type { ApplicationStats } from '~/utils/applicationView'

defineProps<{ stats: ApplicationStats, documentsReady: number, documentsTotal: number }>()

const { t } = useI18n()
</script>

<template>
  <div class="grid grid-cols-1 gap-4 min-[480px]:grid-cols-2 xl:grid-cols-4">
    <StatTile icon="applications" :label="t('dashboard.home.statSubmitted')" :value="stats.submitted" :hint="stats.drafts ? t('dashboard.home.statDrafts', { n: stats.drafts }) : undefined" />
    <StatTile icon="clock" :label="t('dashboard.home.statInProgress')" :value="stats.inProgress" />
    <StatTile icon="award" :label="t('dashboard.home.statOffers')" :value="stats.offers" />
    <StatTile icon="documents" :label="t('dashboard.home.statDocuments')" :value="`${documentsReady}/${documentsTotal}`" />
  </div>
</template>
