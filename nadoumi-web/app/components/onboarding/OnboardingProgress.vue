<script setup lang="ts">
import type { OnboardingStepView } from '~/types/onboarding'

/** The compact progress bar shown on phones (the sidebar takes over from `lg`). Each segment opens its step. */
const props = defineProps<{ steps: OnboardingStepView[], current: number }>()
const emit = defineEmits<{ select: [index: number] }>()
const { t } = useI18n()

const filled = (i: number) => i === props.current || props.steps[i]?.done === true
</script>

<template>
  <nav :aria-label="t('onboarding.progressLabel')">
    <ol class="flex gap-1.5">
      <li v-for="(s, i) in steps" :key="s.key" class="flex-1">
        <button
          type="button"
          class="block h-3 w-full py-[3px] focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand-500"
          :aria-label="s.label"
          :aria-current="i === current ? 'step' : undefined"
          @click="emit('select', i)"
        >
          <span class="block h-1.5 w-full rounded-full transition-colors" :class="filled(i) ? 'bg-brand-600' : 'bg-slate-200'" />
        </button>
      </li>
    </ol>
    <p class="mt-1 flex items-baseline justify-between text-xs">
      <span class="text-slate-500">{{ t('onboarding.stepCount', { n: current + 1, total: steps.length }) }}</span>
      <span class="font-semibold text-slate-800">{{ steps[current]?.label }}</span>
    </p>
  </nav>
</template>
