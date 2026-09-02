<template>
  <div
    class="skeleton"
    role="status"
    :aria-label="t('state.loading')"
  >
    <div
      v-for="n in rows"
      :key="n"
      class="skeleton__row"
      :style="{ width: widthFor(n) }"
    />
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const props = withDefaults(defineProps<{ rows?: number }>(), { rows: 4 })
const { t } = useI18n()

// Vary the last row so the block does not read as a solid rectangle.
function widthFor(n: number): string {
  return n === props.rows ? '60%' : '100%'
}
</script>

<style scoped>
.skeleton {
  display: grid;
  gap: 12px;
  padding: 8px 0;
}
.skeleton__row {
  height: 14px;
  border-radius: 6px;
  background: linear-gradient(90deg, var(--nad-canvas) 25%, #eceef1 37%, var(--nad-canvas) 63%);
  background-size: 400% 100%;
  animation: skeleton-shimmer 1.4s ease infinite;
}
@keyframes skeleton-shimmer {
  0% { background-position: 100% 50%; }
  100% { background-position: 0 50%; }
}
@media (prefers-reduced-motion: reduce) {
  .skeleton__row {
    animation: none;
  }
}
</style>
