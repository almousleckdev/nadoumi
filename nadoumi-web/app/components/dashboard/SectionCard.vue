<script setup lang="ts">
defineProps<{ title: string }>()
</script>

<template>
  <NCard>
    <template #header>
      <div class="flex items-center justify-between gap-3">
        <span>{{ title }}</span>
        <slot name="actions" />
      </div>
    </template>
    <slot />
  </NCard>
</template>
