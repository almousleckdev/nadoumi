<script setup lang="ts">
/** Shown once the application is sent: its reference, what happens next, and where to go from here. */
defineProps<{ reference: string | null }>()
const { t } = useI18n()
const localePath = useLocalePath()
</script>

<template>
  <div class="mx-auto grid max-w-xl gap-5 px-4 py-16 text-center" data-test="submitted">
    <span class="mx-auto grid h-14 w-14 place-items-center rounded-full bg-brand-600 text-white" aria-hidden="true">
      <DashboardIcon name="check" :size="28" />
    </span>
    <h1 class="font-display text-3xl font-bold tracking-tight text-slate-900">{{ t('applyFlow.submitted.title') }}</h1>
    <p v-if="reference" class="font-display text-xl font-semibold tabular-nums text-slate-900" data-test="submitted-reference">{{ reference }}</p>
    <p class="text-slate-600">{{ t('applyFlow.submitted.body') }}</p>
    <div class="flex flex-wrap justify-center gap-3">
      <NButton :to="localePath('/dashboard/applications')" data-test="to-applications">{{ t('applyFlow.submitted.viewApplications') }}</NButton>
      <NButton variant="secondary" :to="localePath('/programs')">{{ t('applyFlow.submitted.applyAgain') }}</NButton>
    </div>
  </div>
</template>
