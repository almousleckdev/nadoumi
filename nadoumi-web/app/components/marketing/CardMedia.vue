<script setup lang="ts">
const props = defineProps<{
  /** Resolved image reference; the placeholder shows when empty or when it fails to load. */
  src?: string | null
  /** Text whose first letter fills the placeholder. */
  label: string
  /** A square thumbnail (list rows) instead of the 16:9 card header. */
  square?: boolean
}>()

const failed = ref(false)
watch(() => props.src, () => { failed.value = false })

const initial = computed(() => props.label.trim().charAt(0).toUpperCase() || 'N')
</script>

<template>
  <div class="relative w-full overflow-hidden bg-slate-100" :class="square ? 'aspect-square' : 'aspect-[16/9]'">
    <img
      v-if="src && !failed"
      :src="mediaUrl(src)"
      alt=""
      loading="lazy"
      decoding="async"
      class="h-full w-full object-cover transition duration-300 group-hover:scale-[1.03]"
      @error="failed = true"
    >
    <div
      v-else
      class="flex h-full w-full items-center justify-center bg-slate-100 font-display font-bold text-slate-400" :class="square ? 'text-xl' : 'text-4xl'"
      aria-hidden="true"
    >
      {{ initial }}
    </div>
    <div v-if="$slots.default" class="absolute inset-x-3 top-3 flex flex-wrap items-center gap-1.5">
      <slot />
    </div>
  </div>
</template>
