<template>
  <el-card
    class="donut"
    :body-style="{ padding: '18px 20px' }"
    shadow="never"
  >
    <div class="donut__label">
      {{ label }}
    </div>

    <div
      v-if="state === 'loading'"
      class="donut__skeleton"
    />
    <div
      v-else-if="state === 'error'"
      class="donut__error"
    >
      {{ errorText || t('dashboard.loadError') }}
    </div>
    <div
      v-else
      class="donut__body"
    >
      <svg
        class="donut__svg"
        viewBox="0 0 42 42"
        role="img"
        :aria-label="ariaLabel"
      >
        <circle
          class="donut__track"
          cx="21"
          cy="21"
          r="15.915"
          fill="transparent"
          stroke-width="4.5"
        />
        <circle
          v-for="(seg, i) in arcs"
          :key="i"
          :cx="21"
          :cy="21"
          r="15.915"
          fill="transparent"
          stroke-width="4.5"
          :stroke="seg.color"
          :stroke-dasharray="`${seg.len} ${100 - seg.len}`"
          :stroke-dashoffset="seg.offset"
          class="donut__arc"
        />
        <text
          x="21"
          y="20.5"
          class="donut__center-num"
        >{{ centerValue }}</text>
        <text
          x="21"
          y="26"
          class="donut__center-cap"
        >{{ centerCaption }}</text>
      </svg>

      <ul class="donut__legend">
        <li
          v-for="(seg, i) in segments"
          :key="i"
        >
          <span
            class="donut__dot"
            :style="{ background: seg.color }"
          />
          <span class="donut__legend-label">{{ seg.label }}</span>
          <span class="donut__legend-val">{{ fmt(seg.value) }}</span>
        </li>
      </ul>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

interface Segment { label: string, value: number, color: string }

const props = withDefaults(defineProps<{
  label: string
  segments: Segment[]
  /** big number in the middle; defaults to the total of all segments */
  centerValue?: number | string
  centerCaption?: string
  state?: 'ok' | 'loading' | 'error'
  errorText?: string
}>(), { centerValue: undefined, centerCaption: undefined, state: 'ok', errorText: undefined })

const { t } = useI18n()
const nf = new Intl.NumberFormat()
const fmt = (n: number) => nf.format(n)

const total = computed(() => props.segments.reduce((s, x) => s + Math.max(0, x.value), 0))

const centerValue = computed(() =>
  props.centerValue ?? (total.value > 0 ? fmt(total.value) : '—'))
const centerCaption = computed(() => props.centerCaption ?? '')

const arcs = computed(() => {
  const sum = total.value
  let acc = 25 // start at 12 o'clock
  return props.segments.map((seg) => {
    const len = sum > 0 ? (Math.max(0, seg.value) / sum) * 100 : 0
    const offset = acc
    acc = (acc - len + 100) % 100
    return { color: seg.color, len, offset }
  })
})

const ariaLabel = computed(() =>
  `${props.label}: ${props.segments.map(s => `${s.label} ${fmt(s.value)}`).join(', ')}`)
</script>

<style scoped>
.donut {
  border: 1px solid var(--nad-line);
  border-radius: var(--nad-radius);
}
.donut__label {
  font-size: 13px;
  font-weight: 600;
  color: var(--nad-ink-soft);
  margin-bottom: 10px;
}
.donut__body {
  display: flex;
  align-items: center;
  gap: 18px;
}
.donut__svg {
  width: 108px;
  height: 108px;
  flex-shrink: 0;
}
.donut__track {
  stroke: var(--nad-line);
}
.donut__arc {
  transition: stroke-dasharray 0.4s ease;
}
.donut__center-num {
  fill: var(--nad-ink);
  font-size: 8px;
  font-weight: 700;
  text-anchor: middle;
}
.donut__center-cap {
  fill: var(--nad-ink-faint);
  font-size: 3.2px;
  font-weight: 600;
  text-anchor: middle;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}
.donut__legend {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 8px;
  min-width: 0;
}
.donut__legend li {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}
.donut__dot {
  width: 9px;
  height: 9px;
  border-radius: 3px;
  flex-shrink: 0;
}
.donut__legend-label {
  color: var(--nad-ink-soft);
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.donut__legend-val {
  font-weight: 700;
  color: var(--nad-ink);
}
.donut__error {
  font-size: 13px;
  color: var(--el-color-danger);
  padding: 20px 0;
}
.donut__skeleton {
  height: 108px;
  border-radius: 10px;
  background: linear-gradient(90deg, var(--nad-canvas) 25%, #eef0f3 37%, var(--nad-canvas) 63%);
  background-size: 400% 100%;
  animation: donut-shimmer 1.4s ease infinite;
}
@keyframes donut-shimmer {
  0% { background-position: 100% 50%; }
  100% { background-position: 0 50%; }
}
@media (prefers-reduced-motion: reduce) {
  .donut__arc,
  .donut__skeleton {
    transition: none;
    animation: none;
  }
}
</style>
