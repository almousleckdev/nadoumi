<script setup lang="ts">
/** Keeps a slot that loads its own data (async setup) from leaving a blank gap: a shimmer shows meanwhile. */
withDefaults(defineProps<{ rows?: number }>(), { rows: 4 })
const { t } = useI18n()

// Vary the last row so the block does not read as a solid rectangle.
function widthFor(n: number, total: number): string {
  return n === total ? '60%' : '100%'
}
</script>

<template>
  <Suspense>
    <slot />
    <template #fallback>
      <div class="grid gap-3" role="status" :aria-label="t('common.loading')">
        <NSkeleton v-for="n in rows" :key="n" class="h-3.5 rounded" :style="{ width: widthFor(n, rows) }" />
      </div>
    </template>
  </Suspense>
</template>
