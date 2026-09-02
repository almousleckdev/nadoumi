<template>
  <div class="tabs">
    <div
      class="tabs__list"
      role="tablist"
    >
      <button
        v-for="tab in tabs"
        :key="tab.key"
        type="button"
        role="tab"
        :aria-selected="tab.key === modelValue"
        class="tabs__tab"
        :class="{ 'tabs__tab--active': tab.key === modelValue }"
        @click="emit('update:modelValue', tab.key)"
      >
        {{ tab.label }}
        <span
          v-if="tab.count !== undefined"
          class="tabs__count"
        >{{ tab.count }}</span>
      </button>
    </div>
    <div class="tabs__panel">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Tab } from './types'

defineProps<{ tabs: Tab[], modelValue: string }>()
const emit = defineEmits<{ 'update:modelValue': [key: string] }>()
</script>

<style scoped>
.tabs__list {
  display: flex;
  gap: 4px;
  border-bottom: 1px solid var(--nad-line);
  overflow-x: auto;
}
.tabs__tab {
  position: relative;
  border: none;
  background: transparent;
  padding: 10px 14px;
  font-size: 14px;
  font-weight: 550;
  color: var(--nad-ink-soft);
  cursor: pointer;
  white-space: nowrap;
  transition: color 0.14s ease;
}
.tabs__tab:hover {
  color: var(--nad-ink);
}
.tabs__tab--active {
  color: var(--nad-brand-700);
}
.tabs__tab--active::after {
  content: '';
  position: absolute;
  left: 10px;
  right: 10px;
  bottom: -1px;
  height: 2px;
  border-radius: 2px;
  background: var(--nad-brand-600);
}
.tabs__count {
  margin-left: 6px;
  font-size: 12px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 999px;
  background: var(--nad-canvas);
  color: var(--nad-ink-faint);
}
.tabs__tab--active .tabs__count {
  background: var(--nad-brand-100);
  color: var(--nad-brand-700);
}
.tabs__panel {
  padding-top: 20px;
}
</style>
