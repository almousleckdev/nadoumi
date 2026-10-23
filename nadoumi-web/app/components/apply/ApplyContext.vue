<script setup lang="ts">
/** The sidebar header of the application: what it is for, whom for, and its reference once it exists. */
defineProps<{ title: string | null, universityName?: string | null, studentName: string | null, reference: string | null }>()
const { t } = useI18n()
</script>

<template>
  <div class="border-b border-white/10 px-5 py-4 text-sm" data-test="apply-context">
    <p v-if="reference" class="font-display text-base font-semibold tabular-nums text-white" data-test="context-reference">{{ reference }}</p>
    <p v-if="title" class="mt-1 text-white">{{ title }}</p>
    <p v-if="universityName" class="text-[var(--sidebar-fg)]">{{ universityName }}</p>
    <p v-if="studentName" class="mt-2 text-[var(--sidebar-fg)]">{{ t('applyFlow.context.student') }}: <span class="text-white">{{ studentName }}</span></p>
  </div>
</template>
