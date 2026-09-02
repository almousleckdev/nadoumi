<template>
  <section class="fs">
    <header
      v-if="title"
      class="fs__head"
    >
      <h3 class="fs__title">
        {{ title }}
      </h3>
      <p
        v-if="description"
        class="fs__desc"
      >
        {{ description }}
      </p>
    </header>
    <div class="fs__body">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ title?: string, description?: string }>(), {
  title: undefined,
  description: undefined,
})
</script>

<style scoped>
.fs + .fs {
  margin-top: 22px;
}
.fs__head {
  margin-bottom: 12px;
}
.fs__title {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--nad-ink-faint);
}
.fs__desc {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--nad-ink-soft);
}
</style>
