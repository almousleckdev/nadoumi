<template>
  <el-card
    class="stat-card"
    :body-style="{ padding: '18px 20px' }"
    shadow="never"
  >
    <div class="stat-card__label">
      {{ label }}
    </div>

    <div
      v-if="state === 'loading'"
      class="stat-card__skeleton"
    />
    <div
      v-else-if="state === 'error'"
      class="stat-card__error"
    >
      {{ errorText || t('dashboard.loadError') }}
    </div>
    <div
      v-else
      class="stat-card__value"
    >
      {{ formatted }}
    </div>

    <div
      v-if="hint"
      class="stat-card__hint"
    >
      {{ hint }}
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = withDefaults(defineProps<{
  label: string
  value?: number | string | null
  hint?: string
  state?: 'ok' | 'loading' | 'error'
  errorText?: string
}>(), { value: null, hint: undefined, state: 'ok', errorText: undefined })

const { t } = useI18n()

const formatted = computed(() => {
  const v = props.value
  if (v === null || v === undefined || v === '') return '—'
  return typeof v === 'number' ? new Intl.NumberFormat().format(v) : v
})
</script>

<style scoped>
.stat-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  transition: box-shadow 0.15s ease, transform 0.15s ease;
}
.stat-card:hover {
  box-shadow: 0 6px 18px rgba(31, 42, 68, 0.08);
}
.stat-card__label {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  font-weight: 500;
}
.stat-card__value {
  margin-top: 6px;
  font-size: 26px;
  font-weight: 700;
  line-height: 1.2;
  color: var(--el-text-color-primary);
}
.stat-card__hint {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
.stat-card__error {
  margin-top: 6px;
  font-size: 13px;
  color: var(--el-color-danger);
}
.stat-card__skeleton {
  margin-top: 10px;
  height: 24px;
  width: 60%;
  border-radius: 6px;
  background: linear-gradient(90deg, var(--el-fill-color-light) 25%, var(--el-fill-color) 37%, var(--el-fill-color-light) 63%);
  background-size: 400% 100%;
  animation: stat-shimmer 1.4s ease infinite;
}
@keyframes stat-shimmer {
  0% { background-position: 100% 50%; }
  100% { background-position: 0 50%; }
}
@media (prefers-reduced-motion: reduce) {
  .stat-card, .stat-card__skeleton { transition: none; animation: none; }
}
</style>
