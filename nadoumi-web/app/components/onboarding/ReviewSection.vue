<script setup lang="ts">
export interface ReviewRow { label: string, value: string }

/** One reviewed section: its status, what the student entered (and any picture), and a way back to change it. */
defineProps<{ title: string, rows: ReviewRow[], complete: boolean, optional?: boolean, empty: string }>()
defineEmits<{ edit: [] }>()
const { t } = useI18n()
</script>

<template>
  <section class="rounded-xl border border-slate-200 bg-white p-4 sm:p-5" data-test="review-section">
    <header class="mb-4 flex items-center justify-between gap-3">
      <div class="flex items-center gap-2">
        <h3 class="font-display text-base font-semibold text-slate-900">{{ title }}</h3>
        <NBadge :tone="complete ? 'success' : optional ? 'neutral' : 'warning'">
          {{ complete ? t('onboarding.review.complete') : optional ? t('onboarding.review.optional') : t('onboarding.review.missing') }}
        </NBadge>
      </div>
      <button type="button" class="rounded-md px-2 py-1 text-sm font-semibold text-brand-700 hover:bg-brand-50 focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand-500" @click="$emit('edit')">{{ t('common.edit') }}</button>
    </header>
    <div v-if="$slots.media" class="mb-4"><slot name="media" /></div>
    <dl v-if="rows.length" class="grid gap-x-8 gap-y-3 text-sm sm:grid-cols-2">
      <div v-for="row in rows" :key="row.label + row.value" class="min-w-0">
        <dt class="text-xs font-medium uppercase tracking-wide text-slate-500">{{ row.label }}</dt>
        <dd class="mt-0.5 break-words font-medium text-slate-900">{{ row.value }}</dd>
      </div>
    </dl>
    <p v-else-if="!$slots.media" class="text-sm text-slate-500">{{ empty }}</p>
  </section>
</template>
