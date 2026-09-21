<script setup lang="ts">
import { DOCUMENT_STATUS_TONES, type DocumentStatus } from '~/constants/onboarding'

/** The shell every onboarding document (photo, passport, later: transcripts) sits in. */
defineProps<{ title: string, guidance: string, status: DocumentStatus }>()
const { t } = useI18n()
</script>

<template>
  <!-- Only ever nested inside the onboarding step's own NCard (identity step) —
       a plain bordered section here, not a second card, avoids double chrome. -->
  <section class="rounded-lg border border-slate-200 p-4 sm:p-5">
    <div class="flex items-start justify-between gap-3">
      <div>
        <h3 class="font-display font-semibold text-slate-900">{{ title }}</h3>
        <p v-if="guidance" class="mt-1 text-sm text-slate-500">{{ guidance }}</p>
      </div>
      <NBadge :tone="DOCUMENT_STATUS_TONES[status]">{{ t(`onboarding.docStatus.${status}`) }}</NBadge>
    </div>
    <div class="mt-4 grid gap-4">
      <slot />
    </div>
  </section>
</template>
