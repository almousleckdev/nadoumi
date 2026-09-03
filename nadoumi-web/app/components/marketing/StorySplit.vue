<script setup lang="ts">
import type { CuratedImage } from '~/data/imagery'

withDefaults(defineProps<{
  image: CuratedImage
  eyebrow?: string
  title: string
  body: string
  /** put the image on the left instead of the right */
  reverse?: boolean
}>(), { eyebrow: undefined, reverse: false })
</script>

<template>
  <section class="py-12 sm:py-16">
    <div
      class="grid items-center gap-8 lg:grid-cols-2 lg:gap-14"
      :class="reverse ? 'lg:[&>figure]:order-first' : ''"
    >
      <MediaFigure
        :src="image.src"
        :alt="image.alt"
        ratio="4/3"
        rounded="2xl"
        sizes="(min-width: 1024px) 46vw, 100vw"
      />
      <div class="max-w-lg">
        <p v-if="eyebrow" class="text-xs font-semibold uppercase tracking-[0.14em] text-brand-600">
          {{ eyebrow }}
        </p>
        <h2 class="mt-2 font-display text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
          {{ title }}
        </h2>
        <p class="mt-3 text-slate-600">{{ body }}</p>
        <div v-if="$slots.actions" class="mt-6 flex flex-wrap gap-3">
          <slot name="actions" />
        </div>
      </div>
    </div>
  </section>
</template>
