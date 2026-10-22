<script setup lang="ts">
defineProps<{
  /** Cover image: always the full-width hero background. */
  image?: string | null
  title: string
  subtitle?: string | null
  eyebrow?: string | null
  /** Small square mark shown above the title (e.g. university logo). */
  logo?: string | null
  backTo: string
  backLabel: string
}>()
</script>

<template>
  <section class="relative isolate overflow-hidden bg-slate-900 text-white">
    <img
      v-if="image"
      :src="mediaUrl(image)"
      alt=""
      fetchpriority="high"
      class="absolute inset-0 -z-20 h-full w-full object-cover"
    >
    <div class="absolute inset-0 -z-10 bg-gradient-to-t from-black/80 via-black/55 to-black/35" aria-hidden="true" />
    <NContainer>
      <div class="flex min-h-[18rem] flex-col justify-end pb-14 pt-8 sm:min-h-[24rem] sm:pb-16 lg:min-h-[28rem]">
        <NuxtLink :to="backTo" class="mb-auto inline-flex w-fit items-center gap-1.5 text-sm text-white/80 no-underline hover:text-white">
          <span aria-hidden="true">←</span> {{ backLabel }}
        </NuxtLink>
        <img
          v-if="logo"
          :src="mediaUrl(logo)"
          alt=""
          class="mb-4 h-14 w-14 rounded-lg bg-white object-contain p-1.5 shadow-md sm:h-16 sm:w-16"
        >
        <div v-if="$slots.badges" class="mb-3 flex flex-wrap items-center gap-2">
          <slot name="badges" />
        </div>
        <p v-if="eyebrow" class="mb-1 font-mono text-xs uppercase tracking-widest text-white/70">{{ eyebrow }}</p>
        <h1 class="max-w-4xl text-balance font-display text-3xl font-bold tracking-tight sm:text-4xl lg:text-5xl">{{ title }}</h1>
        <p v-if="subtitle" class="mt-2 max-w-3xl text-base text-white/85 sm:text-lg">{{ subtitle }}</p>
        <div v-if="$slots.actions" class="mt-6 flex flex-col gap-3 sm:flex-row sm:flex-wrap">
          <slot name="actions" />
        </div>
      </div>
    </NContainer>
  </section>
</template>
