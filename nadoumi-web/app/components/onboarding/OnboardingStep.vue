<script setup lang="ts">
withDefaults(defineProps<{
  title: string
  blurb?: string
  planned?: boolean
  showLegend?: boolean
}>(), { blurb: '', planned: false, showLegend: false })
const { t } = useI18n()
</script>

<template>
  <section class="motion-safe:animate-[fade-in_.15s_ease-out]">
    <div class="mb-4 flex items-start justify-between gap-3">
      <div>
        <h2 class="font-display text-xl font-bold text-slate-900">{{ title }}</h2>
        <p v-if="blurb" class="mt-1 text-sm text-slate-500">{{ blurb }}</p>
      </div>
      <NBadge v-if="planned" tone="warning">{{ t('onboarding.badge.plannedBackend') }}</NBadge>
    </div>

    <p v-if="showLegend" class="mb-4 flex flex-wrap gap-3 text-xs text-slate-400">
      <span><span class="text-brand-700">●</span> {{ t('onboarding.legendRequired') }}</span>
      <span><span class="text-slate-400">●</span> {{ t('onboarding.legendRecommended') }}</span>
      <span><span class="text-slate-300">○</span> {{ t('onboarding.legendOptional') }}</span>
    </p>

    <NAlert v-if="planned" tone="warning" class="mb-4">{{ t('onboarding.comingSoon') }}</NAlert>

    <div :class="planned ? 'pointer-events-none select-none opacity-60' : ''">
      <slot />
    </div>
  </section>
</template>

<style scoped>
@keyframes fade-in { from { opacity: 0; transform: translateY(4px); } to { opacity: 1; transform: none; } }
</style>
