<script setup lang="ts">
// A single shimmer block. Compose with width/height utility classes on the
// instance (e.g. `class="h-4 w-2/3 rounded-md"`) to match the real content's shape.
</script>

<template>
  <div class="n-skeleton" role="presentation" />
</template>

<style scoped>
.n-skeleton {
  background: linear-gradient(90deg, var(--surface-subtle) 25%, #eef1f5 37%, var(--surface-subtle) 63%);
  background-size: 400% 100%;
  animation: n-skeleton-shimmer 1.4s ease infinite;
}
@keyframes n-skeleton-shimmer {
  0% { background-position: 100% 50%; }
  100% { background-position: 0 50%; }
}
</style>
