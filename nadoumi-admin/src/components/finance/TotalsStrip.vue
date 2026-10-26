<script setup lang="ts">
export interface TotalsItem {
  key: string
  label: string
  /** Already formatted, with its currency. */
  value: string
  tone?: 'good' | 'bad'
}

/** What the rows a filter matches add up to, one group per currency. */
defineProps<{ groups: { currency: string, items: TotalsItem[] }[] }>()
</script>

<template>
  <div
    v-if="groups.length"
    class="ts"
    data-test="totals"
  >
    <div
      v-for="g in groups"
      :key="g.currency"
      class="ts__group"
    >
      <span class="ts__ccy">{{ g.currency }}</span>
      <div
        v-for="i in g.items"
        :key="i.key"
        class="ts__item"
      >
        <span class="ts__label">{{ i.label }}</span>
        <span
          class="ts__value"
          :class="i.tone ? `ts__value--${i.tone}` : ''"
        >{{ i.value }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ts { display: flex; flex-wrap: wrap; gap: 10px; margin: 0 0 14px; }
.ts__group { display: flex; flex-wrap: wrap; align-items: baseline; gap: 6px 22px; padding: 10px 16px; border: 1px solid var(--nad-line, #e2e8f0); border-radius: 10px; background: var(--nad-surface, #fff); }
.ts__ccy { font-size: 12px; font-weight: 700; letter-spacing: 0.06em; color: var(--nad-ink-soft, #64748b); }
.ts__item { display: flex; flex-direction: column; }
.ts__label { font-size: 12px; color: var(--nad-ink-soft, #64748b); }
.ts__value { font-size: 17px; font-weight: 650; font-variant-numeric: tabular-nums; }
.ts__value--good { color: var(--el-color-success, #16a34a); }
.ts__value--bad { color: var(--el-color-danger, #dc2626); }
</style>
