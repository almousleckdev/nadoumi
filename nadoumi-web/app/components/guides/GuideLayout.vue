<script setup lang="ts">
import { GUIDES, guideBySlug } from '~/data/guides'
import { GUIDE_HEROES } from '~/data/guides/heroes'

const props = defineProps<{ slug: string }>()

const { t } = useI18n()
const localePath = useLocalePath()

const guide = computed(() => guideBySlug(props.slug))
const hero = computed(() => GUIDE_HEROES[props.slug])
const others = computed(() => GUIDES.filter(g => g.slug !== props.slug))

const title = computed(() => t(`guides.meta.${props.slug}.title`))
const summary = computed(() => t(`guides.meta.${props.slug}.summary`))

useSeo(`${title.value} · Nadoumi Guides`, summary.value)
</script>

<template>
  <div>
    <!-- hero band -->
    <section class="relative isolate flex min-h-[360px] items-end overflow-hidden bg-slate-900 text-white sm:min-h-[460px]">
      <img
        v-if="hero"
        :src="hero"
        alt=""
        decoding="async"
        class="absolute inset-0 -z-10 h-full w-full object-cover"
      >
      <div class="absolute inset-0 -z-10 bg-gradient-to-t from-slate-950/90 via-slate-950/55 to-slate-950/15" aria-hidden="true" />
      <NContainer>
        <div class="max-w-2xl py-10 sm:py-12">
          <nav class="text-sm text-white/70">
            <NuxtLink :to="localePath('/guides')" class="no-underline hover:text-white">{{ t('footer.guides') }}</NuxtLink>
            <span class="mx-2" aria-hidden="true">/</span>
            <span>{{ title }}</span>
          </nav>
          <span class="mt-5 inline-flex h-11 w-11 items-center justify-center rounded-xl bg-white/15 text-white ring-1 ring-white/20">
            <GuideIcon v-if="guide" :name="guide.icon" :size="22" />
          </span>
          <h1 class="mt-4 font-display text-4xl font-bold tracking-tight sm:text-5xl">{{ title }}</h1>
          <p class="mt-4 text-lg leading-8 text-slate-200">{{ summary }}</p>
        </div>
      </NContainer>
    </section>

    <NContainer>
      <div class="mx-auto max-w-3xl py-12 sm:py-16">
        <!-- lead paragraph(s) + GuideSection blocks -->
        <div class="space-y-10 [&>p]:text-lg [&>p]:leading-8 [&>p]:text-slate-700 [&>p_a]:text-brand-700 [&>p_a]:underline">
          <slot />
        </div>
      </div>

      <!-- more guides -->
      <section class="border-t border-slate-200 py-12">
        <h2 class="font-display text-lg font-bold text-slate-900">{{ t('guides.more') }}</h2>
        <div class="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <NuxtLink
            v-for="g in others"
            :key="g.slug"
            :to="localePath(`/guides/${g.slug}`)"
            class="group flex items-start gap-3 rounded-xl border border-slate-200 bg-white p-4 no-underline transition hover:border-brand-300 hover:shadow-sm"
          >
            <span class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-brand-50 text-brand-700">
              <GuideIcon :name="g.icon" :size="18" />
            </span>
            <span>
              <span class="block font-display font-semibold text-slate-900 group-hover:text-brand-700">{{ t(`guides.meta.${g.slug}.title`) }}</span>
              <span class="mt-0.5 block text-sm leading-6 text-slate-600">{{ t(`guides.meta.${g.slug}.summary`) }}</span>
            </span>
          </NuxtLink>
        </div>
      </section>
    </NContainer>
  </div>
</template>
