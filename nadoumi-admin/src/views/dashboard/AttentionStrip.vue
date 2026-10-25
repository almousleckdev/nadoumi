<script setup lang="ts">
import { useRouter } from 'vue-router'

export interface AttentionItem {
  key: string
  label: string
  count: number | null
  /** Extra line, for example "2 overdue". */
  detail?: string
  to: string
  /** Counts above zero are flagged. */
  urgent?: boolean
}

defineProps<{ items: AttentionItem[] }>()
const router = useRouter()
</script>

<template>
  <div
    class="att"
    role="list"
  >
    <button
      v-for="item in items"
      :key="item.key"
      type="button"
      class="att__card"
      :class="{ 'att__card--flag': item.urgent && (item.count ?? 0) > 0 }"
      role="listitem"
      :data-test="`attention-${item.key}`"
      @click="router.push(item.to)"
    >
      <span class="att__count">{{ item.count ?? '–' }}</span>
      <span class="att__text">
        <span class="att__label">{{ item.label }}</span>
        <span
          v-if="item.detail"
          class="att__detail"
        >{{ item.detail }}</span>
      </span>
    </button>
  </div>
</template>

<style scoped>
.att { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 12px; margin-bottom: 16px; }
.att__card {
  display: flex; align-items: center; gap: 14px; padding: 14px 16px; text-align: start; cursor: pointer;
  border: 1px solid var(--nad-line, #e5e7eb); border-radius: 12px; background: var(--nad-surface, #fff);
  border-inline-start-width: 4px; font: inherit; color: inherit;
}
.att__card:hover, .att__card:focus-visible { border-color: var(--el-color-primary); outline: none; }
.att__card--flag { border-inline-start-color: var(--el-color-danger, #dc2626); }
.att__count { font-size: 28px; font-weight: 700; line-height: 1; min-width: 34px; font-variant-numeric: tabular-nums; }
.att__card--flag .att__count { color: var(--el-color-danger, #dc2626); }
.att__text { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.att__label { font-size: 13px; font-weight: 600; }
.att__detail { font-size: 12px; color: var(--el-color-danger, #dc2626); }
</style>
