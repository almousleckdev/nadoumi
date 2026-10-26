<script setup lang="ts">
import { TRACKER_STEPS, trackerIndex } from '~/utils/applicationView'

/** Where an application is on its way: done steps filled, the current one marked, the rest empty. */
const props = defineProps<{ status: string | null }>()

const { t } = useI18n()
const current = computed(() => trackerIndex(props.status))
const unsuccessful = computed(() => props.status === 'CLOSED_UNSUCCESSFUL')
</script>

<template>
  <div>
    <p v-if="unsuccessful" class="mb-3 text-sm font-medium text-red-600" data-test="tracker-unsuccessful">
      {{ t('dashboard.applications.status.CLOSED_UNSUCCESSFUL') }}
    </p>
    <ol class="grid grid-cols-2 gap-x-3 gap-y-4 sm:grid-cols-3 lg:grid-cols-8" data-test="tracker">
      <li v-for="(step, i) in TRACKER_STEPS" :key="step" class="min-w-0" :aria-current="i === current ? 'step' : undefined">
        <div class="flex items-center gap-2">
          <span
            class="grid h-6 w-6 shrink-0 place-items-center rounded-full border text-xs font-semibold"
            :class="i < current
              ? 'border-slate-900 bg-slate-900 text-white'
              : i === current ? 'border-slate-900 bg-white text-slate-900 ring-2 ring-slate-900/20' : 'border-slate-300 bg-white text-slate-400'"
            :data-state="i < current ? 'done' : i === current ? 'current' : 'todo'"
          >{{ i < current ? '✓' : i + 1 }}</span>
          <span class="hidden h-px flex-1 lg:block" :class="i < current ? 'bg-slate-900' : 'bg-slate-200'" aria-hidden="true" />
        </div>
        <p class="mt-2 text-xs leading-snug" :class="i === current ? 'font-semibold text-slate-900' : 'text-slate-500'">
          {{ t(`dashboard.applications.status.${step}`) }}
        </p>
      </li>
    </ol>
  </div>
</template>
