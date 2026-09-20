<template>
  <div
    class="dcs"
    role="status"
    :aria-label="t('state.loading')"
  >
    <div class="dcs__ring" />
    <ul class="dcs__legend">
      <li
        v-for="n in legendRows"
        :key="n"
        class="dcs__row"
        :style="{ width: n === legendRows ? '55%' : '85%' }"
      />
    </ul>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

withDefaults(defineProps<{ legendRows?: number }>(), { legendRows: 3 })
const { t } = useI18n()
</script>

<style scoped>
/* Mirrors DonutChart's own footprint (168px ring + legend list) so the
   shimmer doesn't cause layout shift once the real chart replaces it. */
.dcs {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 8px 0;
}
.dcs__ring {
  width: 168px;
  height: 168px;
  border-radius: 50%;
  background: linear-gradient(90deg, var(--nad-canvas) 25%, #eceef1 37%, var(--nad-canvas) 63%);
  background-size: 400% 100%;
  animation: dcs-shimmer 1.4s ease infinite;
}
.dcs__legend {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 8px;
  width: 100%;
  max-width: 220px;
}
.dcs__row {
  height: 12px;
  border-radius: 6px;
  background: linear-gradient(90deg, var(--nad-canvas) 25%, #eceef1 37%, var(--nad-canvas) 63%);
  background-size: 400% 100%;
  animation: dcs-shimmer 1.4s ease infinite;
}
@keyframes dcs-shimmer {
  0% { background-position: 100% 50%; }
  100% { background-position: 0 50%; }
}
@media (prefers-reduced-motion: reduce) {
  .dcs__ring, .dcs__row {
    animation: none;
  }
}
</style>
