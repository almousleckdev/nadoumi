<script setup lang="ts">
export interface ReviewRow { label: string, value: string }

/** One reviewed section: its status, what the student entered, and a way back to change it. */
defineProps<{ title: string, rows: ReviewRow[], complete: boolean, optional?: boolean, empty: string }>()
defineEmits<{ edit: [] }>()
const { t } = useI18n()
</script>

<template>
  <section class="rounded-lg border border-slate-200 bg-white p-4">
    <header class="mb-3 flex items-center justify-between gap-3">
      <div class="flex items-center gap-2">
        <h3 class="font-display text-base font-semibold text-slate-900">{{ title }}</h3>
        <NBadge :tone="complete ? 'success' : optional ? 'neutral' : 'warning'">
          {{ complete ? t('onboarding.review.complete') : optional ? t('onboarding.review.optional') : t('onboarding.review.missing') }}
        </NBadge>
      </div>
      <button type="button" class="text-sm font-medium text-brand-700 hover:underline" @click="$emit('edit')">{{ t('common.edit') }}</button>
    </header>
    <dl v-if="rows.length" class="grid gap-x-6 gap-y-2 text-sm sm:grid-cols-2">
      <div v-for="row in rows" :key="row.label + row.value" class="min-w-0">
        <dt class="text-xs text-slate-500">{{ row.label }}</dt>
        <dd class="break-words font-medium text-slate-900">{{ row.value }}</dd>
      </div>
    </dl>
    <p v-else class="text-sm text-slate-500">{{ empty }}</p>
  </section>
</template>
