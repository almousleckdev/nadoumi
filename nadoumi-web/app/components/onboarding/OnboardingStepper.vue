<script setup lang="ts">
import { computed } from 'vue'
import type { OnboardingStepView } from '~/types/onboarding'

/**
 * The onboarding sidebar content: overall progress and the list of steps. Every step can be opened at any
 * time; a step shows a check once the server says it is complete. Steps are sidebar rows, the same
 * component the dashboard uses, so both sidebars look identical.
 */
const props = defineProps<{ steps: OnboardingStepView[], current: number }>()
const emit = defineEmits<{ select: [index: number] }>()
const { t } = useI18n()

const tracked = computed(() => props.steps.filter(s => s.done !== null))
const percent = computed(() =>
  tracked.value.length ? Math.round((tracked.value.filter(s => s.done).length / tracked.value.length) * 100) : 0)
const stateOf = (i: number) => (i === props.current ? 'current' : props.steps[i]?.done ? 'done' : 'upcoming')
</script>

<template>
  <nav :aria-label="t('onboarding.progressLabel')" class="flex flex-1 flex-col overflow-y-auto px-3 py-4">
    <div class="mb-4 px-2.5">
      <div class="flex items-baseline justify-between text-xs">
        <span class="text-[var(--sidebar-fg)]">{{ t('onboarding.stepCount', { n: current + 1, total: steps.length }) }}</span>
        <span class="font-semibold tabular-nums text-white" data-test="percent">{{ percent }}%</span>
      </div>
      <div class="mt-2 h-1.5 overflow-hidden rounded-full bg-white/10" role="progressbar" :aria-valuenow="percent" aria-valuemin="0" aria-valuemax="100">
        <div class="h-full origin-left rounded-full bg-brand-600 transition-transform duration-500 ease-out motion-reduce:transition-none" :style="{ transform: `scaleX(${percent / 100})` }" />
      </div>
    </div>

    <ol>
      <li v-for="(s, i) in steps" :key="s.key">
        <SidebarItem
          :active="i === current"
          data-test="step"
          :data-state="stateOf(i)"
          @click="emit('select', i)"
        >
          <template #icon>
            <span
              class="grid h-6 w-6 shrink-0 place-items-center rounded-full text-xs font-semibold"
              :class="stateOf(i) === 'upcoming' ? 'bg-white/10 text-[var(--sidebar-fg)]' : 'bg-white/20 text-white'"
            >
              <DashboardIcon v-if="stateOf(i) === 'done'" name="check" :size="14" />
              <template v-else>{{ i + 1 }}</template>
            </span>
          </template>
          {{ s.label }}
          <template v-if="s.optional" #trailing>
            <span class="text-[11px] font-normal opacity-70">{{ t('onboarding.optionalTag') }}</span>
          </template>
        </SidebarItem>
      </li>
    </ol>
  </nav>
</template>
