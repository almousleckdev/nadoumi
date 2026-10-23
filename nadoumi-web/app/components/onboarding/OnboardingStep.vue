<script setup lang="ts">
/** One step's card: an eyebrow, title and blurb, an Optional badge when the step can be skipped, then its form. */
withDefaults(defineProps<{
  title: string
  blurb?: string
  eyebrow?: string
  optional?: boolean
}>(), { blurb: '', eyebrow: '', optional: false })
const { t } = useI18n()
</script>

<template>
  <section>
    <header class="mb-6 border-b border-slate-200 pb-5">
      <div class="flex items-center gap-2">
        <p v-if="eyebrow" class="text-xs font-semibold uppercase tracking-[0.12em] text-slate-500">{{ eyebrow }}</p>
        <NBadge v-if="optional" tone="neutral">{{ t('onboarding.optionalTag') }}</NBadge>
      </div>
      <h2 class="mt-1 font-display text-2xl font-bold tracking-tight text-slate-900">{{ title }}</h2>
      <p v-if="blurb" class="mt-1.5 max-w-xl text-sm text-slate-600">{{ blurb }}</p>
    </header>
    <slot />
  </section>
</template>
