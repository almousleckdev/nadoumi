<script setup lang="ts">
import { DOCUMENT_STATUS_TONES, type DocumentStatus } from '~/constants/onboarding'

/** The shell every onboarding document (photo, passport, later: transcripts) sits in. */
defineProps<{ title: string, guidance: string, status: DocumentStatus }>()
const { t } = useI18n()
</script>

<template>
  <NCard>
    <div class="flex items-start justify-between gap-3">
      <div>
        <h3 class="font-display font-semibold text-slate-900">{{ title }}</h3>
        <p class="mt-1 text-sm text-slate-500">{{ guidance }}</p>
      </div>
      <NBadge :tone="DOCUMENT_STATUS_TONES[status]">{{ t(`onboarding.docStatus.${status}`) }}</NBadge>
    </div>
    <div class="mt-4 grid gap-4">
      <slot />
    </div>
  </NCard>
</template>
