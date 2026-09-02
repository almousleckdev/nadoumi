<template>
  <dl class="dl">
    <div
      v-for="item in items"
      :key="item.label"
      class="dl__row"
    >
      <dt class="dl__label">
        {{ item.label }}
      </dt>
      <dd class="dl__value">
        <slot
          :name="item.slot || item.label"
          :value="item.value"
        >
          <span :class="{ 'dl__value--empty': isEmpty(item.value) }">
            {{ isEmpty(item.value) ? '—' : item.value }}
          </span>
        </slot>
      </dd>
    </div>
  </dl>
</template>

<script setup lang="ts">
import type { DescriptionItem } from './types'

defineProps<{ items: DescriptionItem[] }>()

function isEmpty(v: unknown): boolean {
  return v === null || v === undefined || v === ''
}
</script>

<style scoped>
.dl {
  margin: 0;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 2px 32px;
}
.dl__row {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 10px 0;
  border-bottom: 1px solid var(--nad-line);
}
.dl__label {
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--nad-ink-faint);
}
.dl__value {
  margin: 0;
  font-size: 14px;
  color: var(--nad-ink);
}
.dl__value--empty {
  color: var(--nad-ink-faint);
}
</style>
