<template>
  <header class="ph">
    <div class="ph__text">
      <h1 class="ph__title">
        {{ title }}
      </h1>
      <p
        v-if="subtitle"
        class="ph__subtitle"
      >
        {{ subtitle }}
      </p>
    </div>
    <div
      v-if="$slots.actions"
      class="ph__actions"
    >
      <slot name="actions" />
    </div>
  </header>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ title: string, subtitle?: string }>(), { subtitle: undefined })
</script>

<style scoped>
.ph {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}
.ph__title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--nad-ink);
}
.ph__subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--nad-ink-soft);
  max-width: 60ch;
}
.ph__actions {
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>
