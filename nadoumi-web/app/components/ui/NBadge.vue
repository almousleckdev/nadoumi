<script setup lang="ts">
withDefaults(defineProps<{ tone?: 'neutral' | 'brand' | 'success' | 'warning' | 'danger' }>(), { tone: 'neutral' })
const tones = {
  neutral: 'bg-slate-100 text-slate-700',
  brand: 'bg-brand-100 text-brand-800',
  success: 'bg-green-100 text-green-800',
  warning: 'bg-amber-100 text-amber-800',
  danger: 'bg-red-100 text-red-800',
}
</script>

<template>
  <span class="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium" :class="tones[tone]">
    <slot />
  </span>
</template>
