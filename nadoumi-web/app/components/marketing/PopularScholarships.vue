<script setup lang="ts">
import type { Page, ScholarshipCard as ScholarshipCardT } from '~/types/catalog'

const props = withDefaults(defineProps<{ title?: string, size?: number }>(), {
  title: undefined,
  size: 3,
})

const localePath = useLocalePath()
const { publicGet } = useApi()

// "Popular right now": fully funded first, then newest. Silently hidden if the
// catalog has nothing to show yet.
const { data } = useAsyncData(
  `popular-scholarships-${props.size}`,
  async () => {
    const [funded, newest] = await Promise.all([
      publicGet<Page<ScholarshipCardT>>('scholarships', { funding: 'FULLY', size: props.size }).catch(() => null),
      publicGet<Page<ScholarshipCardT>>('scholarships', { sort: 'newest', size: props.size }).catch(() => null),
    ])
    const seen = new Set<string>()
    const out: ScholarshipCardT[] = []
    for (const s of [...(funded?.content ?? []), ...(newest?.content ?? [])]) {
      if (out.length >= props.size || seen.has(s.slug)) continue
      seen.add(s.slug)
      out.push(s)
    }
    return out
  },
)

const items = computed(() => data.value ?? [])
</script>

<template>
  <section v-if="items.length" class="border-t border-slate-200 pt-10">
    <div class="flex items-end justify-between gap-4">
      <div>
        <p class="text-xs font-semibold uppercase tracking-[0.14em] text-brand-600">Popular right now</p>
        <h2 class="mt-1 font-display text-2xl font-bold tracking-tight text-slate-900">
          {{ props.title ?? 'Most popular scholarships' }}
        </h2>
      </div>
      <NuxtLink :to="localePath('/scholarships')" class="shrink-0 text-sm font-medium text-brand-700 no-underline hover:underline">
        Browse all &rarr;
      </NuxtLink>
    </div>
    <ol class="mt-5 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
      <li v-for="(s, i) in items" :key="s.slug" class="relative">
        <span class="absolute -left-2 -top-2 z-10 flex h-7 w-7 items-center justify-center rounded-full bg-brand-600 text-xs font-bold text-white shadow-sm">
          {{ i + 1 }}
        </span>
        <ScholarshipCard :scholarship="s" />
      </li>
    </ol>
  </section>
</template>
