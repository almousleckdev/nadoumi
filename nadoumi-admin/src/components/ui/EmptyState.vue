<template>
  <div
    class="empty"
    role="status"
  >
    <el-icon
      class="empty__icon"
      :size="40"
    >
      <component :is="icon" />
    </el-icon>
    <p class="empty__title">
      {{ title }}
    </p>
    <p
      v-if="description"
      class="empty__desc"
    >
      {{ description }}
    </p>
    <div
      v-if="$slots.action"
      class="empty__action"
    >
      <slot name="action" />
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  title: string
  description?: string
  icon?: string
}>(), { description: undefined, icon: 'Box' })
</script>

<style scoped>
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 40px 24px;
  color: var(--nad-ink-soft);
}
.empty__icon {
  color: var(--nad-ink-faint);
  margin-bottom: 12px;
}
.empty__title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--nad-ink);
}
.empty__desc {
  margin: 4px 0 0;
  font-size: 13px;
  max-width: 44ch;
}
.empty__action {
  margin-top: 16px;
}
</style>
