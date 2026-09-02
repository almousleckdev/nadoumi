<script setup lang="ts">
withDefaults(defineProps<{ padding?: 'sm' | 'md' | 'lg' }>(), { padding: 'md' })
const pad = { sm: 'p-4', md: 'p-6', lg: 'p-8' }
</script>

<template>
  <div class="rounded-lg border border-slate-200 bg-white shadow-xs">
    <div v-if="$slots.header" class="border-b border-slate-200 px-6 py-4 font-display font-semibold">
      <slot name="header" />
    </div>
    <div :class="pad[padding]"><slot /></div>
    <div v-if="$slots.footer" class="border-t border-slate-200 px-6 py-4"><slot name="footer" /></div>
  </div>
</template>
