<script setup lang="ts">
import { ref, computed, watch } from 'vue'
/** A person's photo (or initials) with an optional presence dot. `online: null` hides the dot. */
const props = withDefaults(defineProps<{
  name?: string | null
  src?: string | null
  size?: 'sm' | 'md' | 'lg'
  online?: boolean | null
}>(), { name: null, src: null, size: 'md', online: null })

const initials = computed(() =>
  (props.name ?? '').split(/\s+/).filter(Boolean).slice(0, 2).map(s => s[0]!.toUpperCase()).join(''),
)
const dims: Record<string, string> = { sm: 'h-8 w-8 text-xs', md: 'h-10 w-10 text-sm', lg: 'h-12 w-12 text-base' }
const dotDims: Record<string, string> = { sm: 'h-2.5 w-2.5', md: 'h-3 w-3', lg: 'h-3.5 w-3.5' }

// A photo that cannot be loaded (moved, expired, blocked) falls back to initials instead of a broken image.
const failed = ref(false)
watch(() => props.src, () => { failed.value = false })
</script>

<template>
  <span class="relative inline-flex shrink-0">
    <img
      v-if="src && !failed"
      :src="src"
      alt=""
      loading="lazy"
      decoding="async"
      class="rounded-full object-cover"
      :class="dims[size]"
      @error="failed = true"
    >
    <span
      v-else
      aria-hidden="true"
      class="inline-flex items-center justify-center rounded-full bg-brand-100 font-semibold text-brand-800"
      :class="dims[size]"
    >{{ initials }}</span>
    <span
      v-if="online !== null"
      class="absolute bottom-0 end-0 rounded-full ring-2 ring-white"
      :class="[dotDims[size], online ? 'bg-emerald-500' : 'bg-slate-300']"
      data-test="presence-dot"
    />
  </span>
</template>
