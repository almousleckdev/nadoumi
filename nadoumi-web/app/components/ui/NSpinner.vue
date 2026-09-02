<script setup lang="ts">
withDefaults(defineProps<{ size?: 'sm' | 'md'; label?: string }>(), { size: 'sm', label: 'Loading' })
const dim = { sm: 'h-4 w-4', md: 'h-6 w-6' }
</script>

<template>
  <span role="status" class="inline-flex items-center">
    <svg class="animate-spin text-current" :class="dim[size]" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
    </svg>
    <span class="sr-only">{{ label }}</span>
  </span>
</template>
