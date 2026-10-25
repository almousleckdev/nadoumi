<template>
  <div
    class="kpi"
    :class="{ 'kpi--link': to }"
    :role="to ? 'link' : undefined"
    :tabindex="to ? 0 : undefined"
    @click="go"
    @keyup.enter="go"
  >
    <span class="kpi__label">{{ label }}</span>
    <span
      class="kpi__value"
      :class="tone ? `kpi__value--${tone}` : ''"
      data-test="kpi-value"
    >{{ value }}</span>
    <span
      v-if="delta !== null"
      class="kpi__delta"
      :class="deltaGood ? 'kpi__delta--good' : 'kpi__delta--bad'"
      data-test="kpi-delta"
    >{{ delta }}</span>
    <span
      v-else-if="hint"
      class="kpi__hint"
    >{{ hint }}</span>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const props = withDefaults(defineProps<{
  label: string
  value: string
  tone?: 'good' | 'bad'
  /** Pre-formatted change against the previous period, e.g. "+12% vs last month". */
  delta?: string | null
  deltaGood?: boolean
  hint?: string
  to?: string
}>(), { tone: undefined, delta: null, deltaGood: true, hint: undefined, to: undefined })

const router = useRouter()
function go() {
  if (props.to) router.push(props.to)
}
</script>

<style scoped>
.kpi {
  display: flex; flex-direction: column; gap: 6px; padding: 16px 18px;
  border: 1px solid var(--nad-line, #e5e7eb); border-radius: 12px; background: var(--nad-surface, #fff);
  min-width: 0;
}
.kpi--link { cursor: pointer; }
.kpi--link:hover, .kpi--link:focus-visible { border-color: var(--el-color-primary); outline: none; }
.kpi__label { font-size: 12px; color: var(--nad-ink-soft, #64748b); }
.kpi__value { font-size: 22px; font-weight: 700; font-variant-numeric: tabular-nums; color: var(--nad-ink, #0f172a); overflow-wrap: anywhere; }
.kpi__value--good { color: var(--el-color-success, #16a34a); }
.kpi__value--bad { color: var(--el-color-danger, #dc2626); }
.kpi__delta { font-size: 12px; font-weight: 600; }
.kpi__delta--good { color: var(--el-color-success, #16a34a); }
.kpi__delta--bad { color: var(--el-color-danger, #dc2626); }
.kpi__hint { font-size: 12px; color: var(--nad-ink-faint, #9ca3af); }
</style>
