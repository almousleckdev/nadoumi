<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { setLocale } from '@/lang'

const { locale } = useI18n()
const label = computed(() => (locale.value === 'en' ? 'Switch to Chinese' : '切换为英文'))

function toggle() {
  setLocale(locale.value === 'en' ? 'zh' : 'en')
}
</script>

<template>
  <button
    class="topbar__locale"
    type="button"
    :aria-label="label"
    @click="toggle"
  >
    <Transition
      name="locale-swap"
      mode="out-in"
    >
      <span :key="locale">{{ locale === 'en' ? '中文' : 'EN' }}</span>
    </Transition>
  </button>
</template>

<style scoped>
.topbar__locale {
  display: grid;
  place-items: center;
  min-width: 44px;
  height: 30px;
  padding: 0 10px;
  border: 1px solid var(--nad-border, #e2e5ea);
  border-radius: 999px;
  background: transparent;
  color: var(--nad-ink-soft);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.14s ease, border-color 0.14s ease;
}
.topbar__locale:hover {
  background: var(--nad-canvas);
  border-color: var(--nad-brand-500);
  color: var(--nad-ink);
}
.locale-swap-enter-active,
.locale-swap-leave-active {
  transition: opacity 140ms ease, transform 140ms ease;
}
.locale-swap-enter-from {
  opacity: 0;
  transform: translateY(3px);
}
.locale-swap-leave-to {
  opacity: 0;
  transform: translateY(-3px);
}
</style>
