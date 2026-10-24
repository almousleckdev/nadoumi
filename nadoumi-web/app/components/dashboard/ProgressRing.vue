<script setup lang="ts">
/** A single-value ring (0 to 100) with the value written in the middle. */
const props = defineProps<{ percent: number, label: string }>()

const RADIUS = 42
const CIRCUMFERENCE = 2 * Math.PI * RADIUS
const clamped = computed(() => Math.max(0, Math.min(100, props.percent)))
</script>

<template>
  <div class="relative h-28 w-28 shrink-0" role="img" :aria-label="`${label}: ${clamped}%`">
    <svg viewBox="0 0 100 100" class="h-full w-full -rotate-90">
      <circle cx="50" cy="50" :r="RADIUS" fill="none" class="stroke-slate-100" stroke-width="10" />
      <circle
        cx="50" cy="50" :r="RADIUS" fill="none" class="stroke-slate-900" stroke-width="10" stroke-linecap="round"
        :stroke-dasharray="`${(clamped / 100) * CIRCUMFERENCE} ${CIRCUMFERENCE}`"
      />
    </svg>
    <span class="absolute inset-0 grid place-items-center font-display text-xl font-bold tabular-nums text-slate-900">{{ clamped }}%</span>
  </div>
</template>
