<script setup lang="ts">
import type { CuratedImage } from '~/data/imagery'

withDefaults(defineProps<{
  image: CuratedImage
  eyebrow?: string
  title: string
  body?: string
}>(), { eyebrow: undefined, body: undefined })
</script>

<template>
  <section class="relative isolate overflow-hidden">
    <NuxtImg
      :src="image.src"
      :alt="image.alt"
      sizes="100vw"
      loading="lazy"
      decoding="async"
      class="absolute inset-0 -z-10 h-full w-full object-cover"
      :modifiers="{ fit: 'crop', auto: 'format' }"
    />
    <div class="absolute inset-0 -z-10 bg-slate-950/70" aria-hidden="true" />
    <NContainer>
      <div class="max-w-2xl py-20 sm:py-28">
        <p v-if="eyebrow" class="text-xs font-semibold uppercase tracking-[0.14em] text-brand-300">
          {{ eyebrow }}
        </p>
        <h2 class="mt-2 font-display text-3xl font-bold tracking-tight text-white sm:text-4xl">
          {{ title }}
        </h2>
        <p v-if="body" class="mt-3 text-lg text-slate-200">{{ body }}</p>
        <div class="mt-8 flex flex-wrap gap-3">
          <slot name="actions" />
        </div>
      </div>
    </NContainer>
  </section>
</template>
