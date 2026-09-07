<script setup lang="ts">
withDefaults(defineProps<{
  src: string
  alt: string
  /** CSS aspect-ratio, e.g. "16/9", "4/3", "1/1". */
  ratio?: string
  /** Responsive sizes hint for <NuxtImg>. */
  sizes?: string
  rounded?: 'none' | 'md' | 'lg' | 'xl' | '2xl'
  /** Load without lazy — use for above-the-fold hero art only. */
  eager?: boolean
  /** Dim the image so overlaid text stays readable. */
  overlay?: boolean
  /**
   * `src` is a bundled local asset URL (import from `~/assets/...`), not an
   * Unsplash path — render a plain <img> instead of <NuxtImg>.
   */
  local?: boolean
  /** Which part of the image to keep when it is cropped to the ratio box. */
  position?: 'center' | 'top' | 'bottom'
}>(), {
  ratio: '16/9',
  sizes: '100vw',
  rounded: 'lg',
  eager: false,
  overlay: false,
  local: false,
  position: 'center',
})

const radius = {
  none: '', md: 'rounded-md', lg: 'rounded-lg', xl: 'rounded-xl', '2xl': 'rounded-2xl',
}
const objectPos = {
  center: 'object-center', top: 'object-top', bottom: 'object-bottom',
}
</script>

<template>
  <figure
    class="relative isolate overflow-hidden bg-slate-100"
    :class="radius[rounded]"
    :style="{ aspectRatio: ratio }"
  >
    <img
      v-if="local"
      :src="src"
      :alt="alt"
      :loading="eager ? 'eager' : 'lazy'"
      :fetchpriority="eager ? 'high' : 'auto'"
      decoding="async"
      class="absolute inset-0 h-full w-full object-cover"
      :class="objectPos[position]"
    >
    <NuxtImg
      v-else
      :src="src"
      :alt="alt"
      :sizes="sizes"
      :loading="eager ? 'eager' : 'lazy'"
      :fetchpriority="eager ? 'high' : 'auto'"
      decoding="async"
      class="absolute inset-0 h-full w-full object-cover"
      :class="objectPos[position]"
      :modifiers="{ fit: 'crop', auto: 'format' }"
    />
    <div
      v-if="overlay"
      class="absolute inset-0 bg-gradient-to-t from-slate-950/70 via-slate-950/25 to-transparent"
      aria-hidden="true"
    />
    <div v-if="$slots.default" class="absolute inset-0">
      <slot />
    </div>
    <figcaption v-if="$slots.caption" class="absolute bottom-0 inset-x-0 p-3 text-xs text-white/90">
      <slot name="caption" />
    </figcaption>
  </figure>
</template>
