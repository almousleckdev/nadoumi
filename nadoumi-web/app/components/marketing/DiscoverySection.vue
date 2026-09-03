<script setup lang="ts">
withDefaults(defineProps<{
  eyebrow?: string
  title: string
  description?: string
  carouselLabel: string
  viewAllTo?: string
  viewAllLabel?: string
  pending?: boolean
  error?: string
  empty?: boolean
  emptyText?: string
}>(), {
  eyebrow: undefined,
  description: undefined,
  viewAllTo: undefined,
  viewAllLabel: undefined,
  emptyText: undefined,
  pending: false,
  error: '',
  empty: false,
})

const carousel = ref<{ scrollPrev: () => void, scrollNext: () => void, atStart: boolean, atEnd: boolean } | null>(null)
</script>

<template>
  <section class="py-12 sm:py-16">
    <SectionHeading :eyebrow="eyebrow" :title="title" :description="description">
      <template v-if="!pending && !error && !empty" #actions>
        <CarouselArrows
          :at-start="carousel?.atStart"
          :at-end="carousel?.atEnd"
          @prev="carousel?.scrollPrev()"
          @next="carousel?.scrollNext()"
        />
      </template>
    </SectionHeading>

    <div class="mt-6">
      <div v-if="pending" class="flex gap-4 overflow-hidden" aria-hidden="true">
        <div
          v-for="i in 4"
          :key="i"
          class="h-64 w-72 shrink-0 animate-pulse rounded-xl bg-slate-100"
        />
      </div>
      <NAlert v-else-if="error" tone="danger">{{ error }}</NAlert>
      <p v-else-if="empty" class="rounded-xl border border-dashed border-slate-200 px-5 py-8 text-sm text-slate-500">
        {{ emptyText || 'Nothing to show here yet.' }}
      </p>
      <Carousel v-else ref="carousel" :label="carouselLabel">
        <slot />
      </Carousel>
    </div>

    <div v-if="viewAllTo && !pending && !error && !empty" class="mt-6 flex justify-end">
      <NuxtLink
        :to="viewAllTo"
        class="inline-flex items-center gap-1.5 text-sm font-medium text-brand-700 no-underline hover:gap-2.5 hover:text-brand-800"
      >
        {{ viewAllLabel || 'View all' }}
        <span aria-hidden="true">→</span>
      </NuxtLink>
    </div>
  </section>
</template>
