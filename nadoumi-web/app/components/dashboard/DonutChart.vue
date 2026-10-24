<script setup lang="ts">
/** A donut of neutral slate shades; the centre slot carries the total. Segments are drawn as stroke arcs on one circle. */
export interface DonutSegment { key: string, label: string, value: number }

const props = defineProps<{ segments: DonutSegment[] }>()

const RADIUS = 42
const CIRCUMFERENCE = 2 * Math.PI * RADIUS
// darkest first so the earliest stage reads strongest; swatch classes mirror the stroke colours
const SHADES = [
  { stroke: 'stroke-slate-900', swatch: 'bg-slate-900' },
  { stroke: 'stroke-slate-700', swatch: 'bg-slate-700' },
  { stroke: 'stroke-slate-500', swatch: 'bg-slate-500' },
  { stroke: 'stroke-slate-400', swatch: 'bg-slate-400' },
  { stroke: 'stroke-slate-300', swatch: 'bg-slate-300' },
  { stroke: 'stroke-slate-200', swatch: 'bg-slate-200' },
  { stroke: 'stroke-red-500', swatch: 'bg-red-500' },
]

const total = computed(() => props.segments.reduce((sum, s) => sum + s.value, 0))
const arcs = computed(() => {
  let offset = 0
  return props.segments.map((s, i) => {
    const length = total.value ? (s.value / total.value) * CIRCUMFERENCE : 0
    const arc = { ...s, length, offset, ...SHADES[Math.min(i, SHADES.length - 1)] }
    offset += length
    return arc
  })
})
</script>

<template>
  <div class="flex flex-wrap items-center gap-6">
    <div class="relative h-36 w-36 shrink-0">
      <svg viewBox="0 0 100 100" class="h-full w-full -rotate-90" role="img" :aria-label="segments.map(s => `${s.label}: ${s.value}`).join(', ')">
        <circle cx="50" cy="50" :r="RADIUS" fill="none" class="stroke-slate-100" stroke-width="12" />
        <circle
          v-for="a in arcs" :key="a.key" cx="50" cy="50" :r="RADIUS" fill="none" stroke-width="12"
          :class="a.stroke" :stroke-dasharray="`${a.length} ${CIRCUMFERENCE - a.length}`" :stroke-dashoffset="-a.offset"
          data-test="donut-arc"
        />
      </svg>
      <div class="absolute inset-0 grid place-items-center text-center"><slot /></div>
    </div>
    <ul class="grid min-w-0 flex-1 gap-2 text-sm">
      <li v-for="a in arcs" :key="a.key" class="flex items-center gap-2">
        <span class="h-2.5 w-2.5 shrink-0 rounded-full" :class="a.swatch" aria-hidden="true" />
        <span class="min-w-0 flex-1 truncate text-slate-600">{{ a.label }}</span>
        <span class="font-semibold tabular-nums text-slate-900">{{ a.value }}</span>
      </li>
    </ul>
  </div>
</template>
