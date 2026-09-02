<template>
  <section class="dg">
    <header class="dg__head">
      <h2 class="dg__title">
        {{ title }}
      </h2>
      <span
        v-if="subtitle"
        class="dg__subtitle"
      >{{ subtitle }}</span>
      <span class="dg__spacer" />
      <slot name="actions" />
    </header>
    <div
      class="dg__grid"
      :style="{ '--min': `${minCol}px` }"
    >
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ title: string; subtitle?: string; minCol?: number }>(), { subtitle: undefined, minCol: 220 })
</script>

<style scoped>
.dg {
  margin-bottom: 28px;
}
.dg__head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 12px;
}
.dg__title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}
.dg__subtitle {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.dg__spacer {
  flex: 1;
}
.dg__grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(auto-fill, minmax(var(--min), 1fr));
}
</style>
