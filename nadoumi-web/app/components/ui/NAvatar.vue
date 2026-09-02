<script setup lang="ts">
const props = withDefaults(defineProps<{ name?: string; size?: 'sm' | 'md' }>(), { name: undefined, size: 'sm' })
const initials = computed(() =>
  (props.name ?? '')
    .split(/\s+/).filter(Boolean).slice(0, 2).map(s => s[0]!.toUpperCase()).join(''),
)
const dim = { sm: 'h-8 w-8 text-xs', md: 'h-10 w-10 text-sm' }
</script>

<template>
  <span class="inline-flex items-center justify-center rounded-full bg-brand-100 font-semibold text-brand-800" :class="dim[size]">
    {{ initials }}
  </span>
</template>
