<script setup lang="ts">
import { computed } from 'vue'

/**
 * The vertical step rail of the onboarding sidebar. A finished step shows a check, the current one is
 * highlighted, and a finished step can be clicked to go back to it. The line between steps fills as the student
 * moves on.
 */
const props = defineProps<{
  steps: { key: string, label: string, optional?: boolean }[]
  current: number
  /** The furthest step the student has reached; steps up to it can be revisited. */
  reached: number
}>()
const emit = defineEmits<{ select: [index: number] }>()
const { t } = useI18n()

const percent = computed(() => Math.round((props.current / Math.max(props.steps.length - 1, 1)) * 100))
const stateOf = (i: number) => (i < props.current ? 'done' : i === props.current ? 'current' : 'upcoming')
</script>

<template>
  <nav :aria-label="t('onboarding.progressLabel')">
    <div class="mb-6">
      <div class="flex items-baseline justify-between text-xs text-slate-300">
        <span>{{ t('onboarding.stepCount', { n: current + 1, total: steps.length }) }}</span>
        <span class="font-semibold tabular-nums text-white" data-test="percent">{{ percent }}%</span>
      </div>
      <div class="mt-2 h-1.5 overflow-hidden rounded-full bg-white/10" role="progressbar" :aria-valuenow="percent" aria-valuemin="0" aria-valuemax="100">
        <div class="h-full origin-left rounded-full bg-brand-500 transition-transform duration-500 ease-out motion-reduce:transition-none" :style="{ transform: `scaleX(${percent / 100})` }" />
      </div>
    </div>

    <ol class="relative grid gap-1">
      <li v-for="(s, i) in steps" :key="s.key" class="relative">
        <span
          v-if="i < steps.length - 1"
          class="absolute start-[19px] top-10 h-[calc(100%-1.5rem)] w-px overflow-hidden bg-white/15"
          aria-hidden="true"
        >
          <span class="block w-full origin-top bg-brand-500 transition-transform duration-500 ease-out motion-reduce:transition-none" :class="i < current ? 'h-full scale-y-100' : 'h-full scale-y-0'" />
        </span>
        <button
          type="button"
          class="group relative flex w-full items-center gap-3 rounded-xl px-2 py-2 text-start transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand-400 disabled:cursor-default"
          :class="stateOf(i) === 'current' ? 'bg-white/10' : i <= reached ? 'hover:bg-white/5' : ''"
          :disabled="i > reached || i === current"
          :aria-current="i === current ? 'step' : undefined"
          :data-state="stateOf(i)"
          data-test="step"
          @click="emit('select', i)"
        >
          <span
            class="grid h-8 w-8 shrink-0 place-items-center rounded-full text-sm font-semibold transition-all duration-300"
            :class="{
              'bg-brand-500 text-white': stateOf(i) === 'done',
              'bg-white text-slate-900 shadow-[0_0_0_4px_rgba(249,115,22,0.35)]': stateOf(i) === 'current',
              'bg-white/10 text-slate-300': stateOf(i) === 'upcoming',
            }"
          >
            <svg v-if="stateOf(i) === 'done'" viewBox="0 0 24 24" class="h-4 w-4 motion-safe:animate-[pop_.3s_ease-out]" fill="none" stroke="currentColor" stroke-width="3" aria-hidden="true"><path d="m5 12 4.5 4.5L19 7" stroke-linecap="round" stroke-linejoin="round" /></svg>
            <template v-else>{{ i + 1 }}</template>
          </span>
          <span class="min-w-0 flex-1">
            <span class="block truncate text-sm" :class="stateOf(i) === 'upcoming' ? 'text-slate-300' : 'font-semibold text-white'">{{ s.label }}</span>
            <span v-if="s.optional" class="block text-[11px] text-slate-400">{{ t('onboarding.optionalTag') }}</span>
          </span>
        </button>
      </li>
    </ol>
  </nav>
</template>

<style scoped>
@keyframes pop { 0% { transform: scale(0.4); opacity: 0; } 70% { transform: scale(1.15); } 100% { transform: scale(1); opacity: 1; } }
</style>
