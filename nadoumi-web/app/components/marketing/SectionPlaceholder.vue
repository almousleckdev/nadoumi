<script setup lang="ts">
/**
 * Honest stand-in for a discovery section whose backend does not exist yet.
 * Renders the section heading + a clearly-labelled "arriving with …" notice and
 * an inert, content-free preview strip. Never shows invented cards or numbers.
 */
withDefaults(defineProps<{
  eyebrow?: string
  title: string
  description?: string
  /** e.g. "the Scholarship catalogue" */
  arrivingWith: string
}>(), { eyebrow: undefined, description: undefined })
</script>

<template>
  <section class="py-12 sm:py-16">
    <SectionHeading :eyebrow="eyebrow" :title="title" :description="description" />
    <div class="mt-6 rounded-2xl border border-dashed border-slate-200 bg-slate-50/60 p-6">
      <p class="flex items-center gap-2 text-sm font-medium text-slate-500">
        <span class="inline-block h-1.5 w-1.5 rounded-full bg-slate-300" aria-hidden="true" />
        Arriving with {{ arrivingWith }}
      </p>
      <div class="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-4" aria-hidden="true">
        <div v-for="i in 4" :key="i" class="rounded-xl border border-slate-200 bg-white p-4 opacity-60">
          <div class="h-24 rounded-lg bg-slate-100" />
          <div class="mt-3 h-3 w-3/4 rounded bg-slate-100" />
          <div class="mt-2 h-3 w-1/2 rounded bg-slate-100" />
        </div>
      </div>
    </div>
  </section>
</template>
