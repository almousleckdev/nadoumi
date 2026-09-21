<script setup lang="ts">
const props = withDefaults(defineProps<{ name?: string; size?: 'sm' | 'md'; src?: string }>(), { name: undefined, size: 'sm', src: undefined })
const initials = computed(() =>
  (props.name ?? '')
    .split(/\s+/).filter(Boolean).slice(0, 2).map(s => s[0]!.toUpperCase()).join(''),
)
const dim = { sm: 'h-8 w-8 text-xs', md: 'h-10 w-10 text-sm' }
</script>

<template>
  <img v-if="src" :src="src" :alt="name ?? ''" class="inline-block rounded-full object-cover" :class="dim[size]">
  <span v-else class="inline-flex items-center justify-center rounded-full bg-brand-100 font-semibold text-brand-800" :class="dim[size]">
    {{ initials }}
  </span>
</template>
