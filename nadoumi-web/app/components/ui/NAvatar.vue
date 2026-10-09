<script setup lang="ts">
const props = withDefaults(defineProps<{ name?: string; size?: 'sm' | 'md'; src?: string }>(), { name: undefined, size: 'sm', src: undefined })
const initials = computed(() =>
  (props.name ?? '')
    .split(/\s+/).filter(Boolean).slice(0, 2).map(s => s[0]!.toUpperCase()).join(''),
)
const dim = { sm: 'h-8 w-8 text-xs', md: 'h-10 w-10 text-sm' }

// A photo that cannot be loaded (moved, expired, blocked) falls back to initials instead of a broken image.
const failed = ref(false)
watch(() => props.src, () => { failed.value = false })
</script>

<template>
  <img
    v-if="src && !failed"
    :src="src"
    :alt="name ?? ''"
    loading="lazy"
    decoding="async"
    class="inline-block rounded-full object-cover"
    :class="dim[size]"
    @error="failed = true"
  >
  <span v-else class="inline-flex items-center justify-center rounded-full bg-brand-100 font-semibold text-brand-800" :class="dim[size]">
    {{ initials }}
  </span>
</template>
