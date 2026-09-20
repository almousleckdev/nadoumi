<script setup lang="ts">
const props = defineProps<{ steps: { key: string; label: string }[]; current: number }>()
const { t } = useI18n()

const segmentState = (i: number) => (i < props.current ? 'done' : i === props.current ? 'current' : 'upcoming')
</script>

<template>
  <nav aria-label="Onboarding progress" class="mb-8">
    <ol class="flex gap-1.5">
      <li v-for="(s, i) in steps" :key="s.key" class="h-1.5 flex-1 overflow-hidden rounded-full bg-slate-200">
        <span
          class="block h-full rounded-full bg-brand-500 transition-[width] duration-500 ease-out motion-reduce:transition-none"
          :class="segmentState(i) === 'upcoming' ? 'w-0' : 'w-full'"
          :aria-current="i === current ? 'step' : undefined"
        />
      </li>
    </ol>
    <p class="mt-2.5 flex items-baseline justify-between text-xs">
      <span class="text-slate-400">{{ t('onboarding.stepCount', { n: current + 1, total: steps.length }) }}</span>
      <span class="font-semibold text-slate-700">{{ steps[current]?.label }}</span>
    </p>
  </nav>
</template>
