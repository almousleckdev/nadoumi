<template>
  <ul
    v-if="rows.length"
    class="bk"
  >
    <li
      v-for="r in rows"
      :key="r.label"
      class="bk__row"
    >
      <span class="bk__label">{{ r.label }}</span>
      <span class="bk__bar"><span
        class="bk__fill"
        :class="{ 'bk__fill--muted': tone === 'muted' }"
        :style="{ width: width(r.total) }"
      /></span>
      <span class="bk__val">{{ currency }} {{ Number(r.total).toLocaleString('en-US', { maximumFractionDigits: 2 }) }}</span>
    </li>
  </ul>
  <p
    v-else
    class="bk__empty"
  >
    {{ empty }}
  </p>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  rows: { label: string, total: number, count: number }[]
  currency: string
  empty: string
  tone?: 'muted'
}>()

const max = computed(() => Math.max(1, ...props.rows.map(r => Number(r.total))))
const width = (v: number) => `${Math.max(2, (Number(v) / max.value) * 100)}%`
</script>

<style scoped>
.bk { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 10px; }
.bk__row { display: grid; grid-template-columns: minmax(90px, 150px) 1fr auto; align-items: center; gap: 10px; font-size: 13px; }
.bk__label { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.bk__bar { height: 8px; background: var(--nad-line, #e5e7eb); border-radius: 4px; overflow: hidden; }
.bk__fill { display: block; height: 100%; background: var(--el-color-primary, #4f46e5); }
.bk__fill--muted { background: #94a3b8; }
.bk__val { font-variant-numeric: tabular-nums; color: var(--nad-ink-soft, #64748b); }
.bk__empty { margin: 0; font-size: 13px; color: var(--nad-ink-faint, #9ca3af); }
</style>
