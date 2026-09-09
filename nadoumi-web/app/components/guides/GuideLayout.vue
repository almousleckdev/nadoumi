<script setup lang="ts">
import { GUIDES, guideBySlug } from '~/data/guides'

const props = defineProps<{ slug: string }>()

const { t } = useI18n()
const localePath = useLocalePath()

const guide = computed(() => guideBySlug(props.slug))
const others = computed(() => GUIDES.filter(g => g.slug !== props.slug))

useSeo(
  guide.value ? `${guide.value.title}, Nadoumi Guides` : 'Nadoumi Guides',
  guide.value?.summary ?? '',
)
</script>

<template>
  <div>
    <PageHero :title="guide?.title ?? t('footer.guides')" :subtitle="guide?.summary">
      <nav class="mt-4 text-sm">
        <NuxtLink :to="localePath('/guides')" class="text-brand-700 no-underline hover:underline">{{ t('footer.guides') }}</NuxtLink>
        <span class="mx-2 text-slate-400" aria-hidden="true">/</span>
        <span class="text-slate-500">{{ guide?.title }}</span>
      </nav>
    </PageHero>

    <NContainer>
      <article class="guide-prose mx-auto max-w-3xl py-12 sm:py-16">
        <slot />
      </article>

      <section class="border-t border-slate-200 py-12">
        <h2 class="font-display text-lg font-bold text-slate-900">{{ t('guides.more') }}</h2>
        <div class="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <NuxtLink
            v-for="g in others"
            :key="g.slug"
            :to="localePath(`/guides/${g.slug}`)"
            class="group rounded-xl border border-slate-200 bg-white p-5 no-underline transition hover:border-brand-300 hover:shadow-sm"
          >
            <p class="font-display font-semibold text-slate-900 group-hover:text-brand-700">{{ g.title }}</p>
            <p class="mt-1 text-sm leading-6 text-slate-600">{{ g.summary }}</p>
          </NuxtLink>
        </div>
      </section>
    </NContainer>
  </div>
</template>

<!-- Not scoped: styles slotted prose passed by each guide page. All rules are
     namespaced under .guide-prose so nothing leaks. -->
<style>
.guide-prose > h2 {
  font-family: '"Plus Jakarta Sans"', 'Inter', ui-sans-serif, system-ui, sans-serif;
  font-size: 1.5rem;
  line-height: 2rem;
  font-weight: 700;
  color: rgb(15 23 42);
  margin-top: 2.5rem;
  margin-bottom: 0.75rem;
}
.guide-prose > h2:first-child { margin-top: 0; }
.guide-prose > h3 {
  font-size: 1.125rem;
  font-weight: 600;
  color: rgb(15 23 42);
  margin-top: 1.75rem;
  margin-bottom: 0.5rem;
}
.guide-prose > p,
.guide-prose li {
  color: rgb(51 65 85);
  line-height: 1.8;
}
.guide-prose > p { margin-top: 1rem; }
.guide-prose ul,
.guide-prose ol { margin-top: 1rem; padding-left: 1.5rem; }
.guide-prose ul { list-style: disc; }
.guide-prose ol { list-style: decimal; }
.guide-prose li { margin-top: 0.4rem; padding-left: 0.25rem; }
.guide-prose li::marker { color: rgb(148 163 184); }
.guide-prose a { color: rgb(4 120 87); text-decoration: underline; }
.guide-prose strong { color: rgb(15 23 42); font-weight: 600; }
.guide-prose hr { margin: 2.5rem 0; border: 0; border-top: 1px solid rgb(226 232 240); }
</style>
