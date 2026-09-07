<template>
  <div
    class="dc"
    @mouseleave="hover = null"
  >
    <div class="dc__plot">
      <svg
        class="dc__svg"
        :viewBox="`0 0 ${SIZE} ${SIZE}`"
        role="img"
        :aria-label="ariaLabel"
      >
        <circle
          class="dc__track"
          :cx="SIZE / 2"
          :cy="SIZE / 2"
          :r="R"
          :stroke-width="STROKE"
        />
        <circle
          v-for="(seg, i) in segments"
          :key="seg.label + i"
          class="dc__seg"
          :class="{ 'is-dim': hover && hover.i !== i }"
          :cx="SIZE / 2"
          :cy="SIZE / 2"
          :r="R"
          :stroke="seg.color"
          :stroke-width="hover && hover.i === i ? STROKE + 4 : STROKE"
          :stroke-dasharray="`${grown ? seg.len : 0} ${C}`"
          :stroke-dashoffset="-seg.offset"
          @mousemove="onHover(i, $event)"
          @mouseenter="onHover(i, $event)"
        />
        <text
          class="dc__total"
          :x="SIZE / 2"
          :y="SIZE / 2 - 2"
          text-anchor="middle"
        >{{ (hover ? segments[hover.i].value : total).toLocaleString('en-US') }}</text>
        <text
          class="dc__caption"
          :x="SIZE / 2"
          :y="SIZE / 2 + 16"
          text-anchor="middle"
        >{{ hover ? segments[hover.i].label : caption }}</text>
      </svg>
      <div
        v-if="hover"
        class="dc__tip"
        :style="{ left: `${hover.x}px`, top: `${hover.y}px` }"
      >
        <span
          class="dc__tip-dot"
          :style="{ background: segments[hover.i].color }"
        />
        {{ segments[hover.i].label }}: <b>{{ segments[hover.i].value.toLocaleString('en-US') }}</b>
        ({{ Math.round((segments[hover.i].value / (total || 1)) * 100) }}%)
      </div>
    </div>
    <ul class="dc__legend">
      <li
        v-for="(seg, i) in segments"
        :key="seg.label + i"
        :class="{ 'is-active': hover && hover.i === i }"
        @mouseenter="onHover(i, $event)"
      >
        <span
          class="dc__dot"
          :style="{ background: seg.color }"
        />
        <span class="dc__leg-label">{{ seg.label }}</span>
        <span class="dc__leg-val">{{ seg.value.toLocaleString('en-US') }}</span>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { colorAt } from './ChartPalette'

const props = withDefaults(defineProps<{
  slices: { label: string, value: number }[]
  caption?: string
}>(), { caption: '' })

const SIZE = 168
const STROKE = 22
const R = (SIZE - STROKE - 6) / 2
const C = 2 * Math.PI * R

const grown = ref(false)
const hover = ref<{ i: number, x: number, y: number } | null>(null)
const total = computed(() => props.slices.reduce((s, x) => s + x.value, 0))

const segments = computed(() => {
  const t = total.value || 1
  let acc = 0
  return props.slices
    .filter(s => s.value > 0)
    .map((s, i) => {
      const len = (s.value / t) * C
      const seg = { label: s.label, value: s.value, color: colorAt(i), len, offset: acc }
      acc += len
      return seg
    })
})

const ariaLabel = computed(() => props.slices.map(s => `${s.label}: ${s.value}`).join(', '))

function onHover(i: number, e: MouseEvent) {
  const host = (e.currentTarget as SVGElement).closest('.dc__plot') as HTMLElement | null
  const box = host?.getBoundingClientRect()
  hover.value = {
    i,
    x: box ? e.clientX - box.left + 12 : 12,
    y: box ? e.clientY - box.top + 12 : 12,
  }
}

async function animate() {
  grown.value = false
  await nextTick()
  requestAnimationFrame(() => { grown.value = true })
}
onMounted(animate)
watch(() => props.slices, animate, { deep: true })
</script>

<style scoped>
.dc {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 8px 0;
}
.dc__plot {
  position: relative;
}
.dc__svg {
  width: 168px;
  height: 168px;
  transform: rotate(-90deg);
}
.dc__track {
  fill: none;
  stroke: var(--nad-line, #eef1f5);
}
.dc__seg {
  fill: none;
  stroke-linecap: round;
  cursor: pointer;
  transition: stroke-dasharray 0.8s cubic-bezier(0.22, 1, 0.36, 1), stroke-width 0.15s ease, opacity 0.15s ease;
}
.dc__seg.is-dim {
  opacity: 0.35;
}
.dc__total {
  transform: rotate(90deg);
  transform-origin: center;
  font-size: 22px;
  font-weight: 700;
  fill: var(--nad-ink, #1f2937);
}
.dc__caption {
  transform: rotate(90deg);
  transform-origin: center;
  font-size: 10px;
  fill: var(--nad-ink-faint, #9ca3af);
  text-transform: uppercase;
  letter-spacing: 0.4px;
}
.dc__tip {
  position: absolute;
  z-index: 5;
  pointer-events: none;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 9px;
  border-radius: 8px;
  background: var(--nad-ink, #1f2937);
  color: #fff;
  font-size: 12px;
  white-space: nowrap;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.22);
}
.dc__tip-dot {
  width: 8px;
  height: 8px;
  border-radius: 2px;
}
.dc__legend {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 6px 16px;
  max-width: 340px;
}
.dc__legend li {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 12px;
  padding: 2px 4px;
  border-radius: 6px;
  cursor: default;
  transition: background 0.12s ease;
}
.dc__legend li.is-active {
  background: var(--nad-surface-2, #f1f5f9);
}
.dc__dot {
  width: 10px;
  height: 10px;
  border-radius: 3px;
  flex-shrink: 0;
}
.dc__leg-label {
  color: var(--nad-ink, #1f2937);
}
.dc__leg-val {
  color: var(--nad-ink-soft, #64748b);
  font-variant-numeric: tabular-nums;
}
@media (prefers-reduced-motion: reduce) {
  .dc__seg { transition: stroke-width 0.15s ease, opacity 0.15s ease; }
}
</style>
