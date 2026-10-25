<template>
  <div class="trend">
    <svg
      :viewBox="`0 0 ${W} ${H}`"
      class="trend__svg"
      role="img"
      :aria-label="summary"
    >
      <g
        v-for="g in grid"
        :key="g.y"
      >
        <line
          :x1="PAD_L"
          :x2="W - PAD_R"
          :y1="g.y"
          :y2="g.y"
          class="trend__grid"
        />
        <text
          :x="PAD_L - 8"
          :y="g.y + 4"
          class="trend__tick"
          text-anchor="end"
        >{{ g.label }}</text>
      </g>
      <g
        v-for="(m, i) in months"
        :key="m.month"
      >
        <rect
          :x="bandX(i) + 4"
          :y="yOf(m.revenue)"
          :width="barW"
          :height="Math.max(0, zeroY - yOf(m.revenue))"
          rx="3"
          class="trend__rev"
          data-test="bar-revenue"
        ><title>{{ tip(m, 'revenue') }}</title></rect>
        <rect
          :x="bandX(i) + 4 + barW + 3"
          :y="yOf(m.expenses)"
          :width="barW"
          :height="Math.max(0, zeroY - yOf(m.expenses))"
          rx="3"
          class="trend__exp"
        ><title>{{ tip(m, 'expenses') }}</title></rect>
        <text
          :x="bandX(i) + band / 2"
          :y="H - 8"
          class="trend__tick"
          text-anchor="middle"
        >{{ monthLabel(m.month) }}</text>
      </g>
      <line
        :x1="PAD_L"
        :x2="W - PAD_R"
        :y1="zeroY"
        :y2="zeroY"
        class="trend__zero"
      />
      <polyline
        :points="netPoints"
        class="trend__net"
        fill="none"
      />
      <circle
        v-for="(m, i) in months"
        :key="`n${m.month}`"
        :cx="bandX(i) + band / 2"
        :cy="yOf(m.net)"
        r="3"
        class="trend__net-dot"
      ><title>{{ tip(m, 'net') }}</title></circle>
    </svg>
    <ul class="trend__legend">
      <li><span class="trend__swatch trend__sw-rev" />{{ revenueLabel }}</li>
      <li><span class="trend__swatch trend__sw-exp" />{{ expensesLabel }}</li>
      <li><span class="trend__swatch trend__sw-net" />{{ netLabel }}</li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { FinanceMonth } from '@/api/finance'

const props = defineProps<{
  months: FinanceMonth[]
  currency: string
  revenueLabel: string
  expensesLabel: string
  netLabel: string
}>()

const W = 760
const H = 280
const PAD_L = 56
const PAD_R = 12
const PAD_T = 12
const PAD_B = 30
const GRID_LINES = 4

const baseline = H - PAD_B
const plotH = baseline - PAD_T
const band = computed(() => (W - PAD_L - PAD_R) / Math.max(1, props.months.length))
const barW = computed(() => Math.max(4, (band.value - 14) / 2))

// the scale covers the tallest bar and a negative net month, so nothing is clipped
const hi = computed(() => Math.max(1, ...props.months.flatMap(m => [m.revenue, m.expenses, m.net])))
const lo = computed(() => Math.min(0, ...props.months.map(m => m.net)))
const yOf = (v: number) => PAD_T + plotH - ((v - lo.value) / (hi.value - lo.value)) * plotH
const zeroY = computed(() => yOf(0))
const bandX = (i: number) => PAD_L + i * band.value

const grid = computed(() => Array.from({ length: GRID_LINES + 1 }, (_, i) => {
  const v = lo.value + ((hi.value - lo.value) * i) / GRID_LINES
  return { y: yOf(v), label: short(v) }
}))
const netPoints = computed(() => props.months.map((m, i) => `${bandX(i) + band.value / 2},${yOf(m.net)}`).join(' '))

function short(v: number): string {
  const abs = Math.abs(v)
  if (abs >= 1_000_000) return `${(v / 1_000_000).toFixed(1)}M`
  if (abs >= 1_000) return `${Math.round(v / 1_000)}k`
  return String(Math.round(v))
}
function monthLabel(month: string): string {
  const [y, m] = month.split('-').map(Number)
  return new Date(y!, m! - 1, 1).toLocaleString('en-US', { month: 'short' })
}
function tip(m: FinanceMonth, key: 'revenue' | 'expenses' | 'net'): string {
  return `${m.month}: ${props.currency} ${Number(m[key]).toLocaleString('en-US', { maximumFractionDigits: 2 })}`
}
const summary = computed(() => `${props.revenueLabel}, ${props.expensesLabel} and ${props.netLabel} for the last ${props.months.length} months in ${props.currency}`)
</script>

<style scoped>
.trend__svg { width: 100%; height: auto; display: block; }
.trend__grid { stroke: var(--nad-line, #e5e7eb); stroke-width: 1; }
.trend__tick { font-size: 11px; fill: var(--nad-ink-soft, #64748b); }
.trend__rev { fill: var(--el-color-primary, #4f46e5); }
.trend__exp { fill: #cbd5e1; }
.trend__net { stroke: var(--el-color-success, #16a34a); stroke-width: 2; stroke-linejoin: round; }
.trend__net-dot { fill: var(--el-color-success, #16a34a); }
.trend__legend { display: flex; flex-wrap: wrap; gap: 16px; list-style: none; margin: 8px 0 0; padding: 0; font-size: 12px; color: var(--nad-ink-soft, #64748b); }
.trend__legend li { display: flex; align-items: center; gap: 6px; }
.trend__swatch { width: 10px; height: 10px; border-radius: 2px; display: inline-block; }
.trend__zero { stroke: #94a3b8; stroke-width: 1.5; }
.trend__sw-rev { background: var(--el-color-primary, #4f46e5); }
.trend__sw-exp { background: #cbd5e1; }
.trend__sw-net { background: var(--el-color-success, #16a34a); }
</style>
