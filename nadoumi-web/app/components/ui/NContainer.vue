<script setup lang="ts">
withDefaults(defineProps<{ size?: 'marketing' | 'app' }>(), { size: 'marketing' })
</script>

<template>
  <div class="mx-auto w-full px-5" :class="size === 'app' ? 'max-w-app' : 'max-w-marketing'">
    <slot />
  </div>
</template>
