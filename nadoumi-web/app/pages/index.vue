<script setup lang="ts">
import type { Page, UniversitySummary } from '~/types/catalog'

const { t } = useI18n()
const localePath = useLocalePath()
useSeo(t('home.title'), t('home.subtitle'))

const { publicGet } = useApi()
const { data: unis } = await useAsyncData('home-universities', () =>
  publicGet<Page<UniversitySummary>>('universities', { size: 6 }).catch(() => null),
)
const featured = computed(() => unis.value?.content ?? [])

const values = computed(() => [
  { t: t('home.f1Title'), b: t('home.f1Body') },
  { t: t('home.f2Title'), b: t('home.f2Body') },
  { t: t('home.f3Title'), b: t('home.f3Body') },
])
const steps = computed(() => [
  { t: t('home.how1t'), b: t('home.how1b') },
  { t: t('home.how2t'), b: t('home.how2b') },
  { t: t('home.how3t'), b: t('home.how3b') },
  { t: t('home.how4t'), b: t('home.how4b') },
])
function place(u: UniversitySummary): string {
  return [u.city, u.country].filter(Boolean).join(', ')
}
</script>

<template>
  <div>
    <section class="border-b border-slate-200 bg-slate-50">
      <NContainer>
        <div class="py-16 sm:py-24">
          <h1 class="max-w-3xl font-display text-4xl font-bold tracking-tight text-slate-900 sm:text-5xl">
            {{ t('home.title') }}
          </h1>
          <p class="mt-4 max-w-2xl text-lg text-slate-600">{{ t('home.subtitle') }}</p>
          <div class="mt-8 flex flex-wrap gap-3">
            <NButton :to="localePath('/register')" size="lg">{{ t('home.ctaPrimary') }}</NButton>
            <NButton :to="localePath('/scholarships')" variant="secondary" size="lg">
              {{ t('home.ctaSecondary') }}
            </NButton>
          </div>
        </div>
      </NContainer>
    </section>

    <NContainer>
      <section class="py-14">
        <h2 class="font-display text-2xl font-semibold text-slate-900">{{ t('home.valuesTitle') }}</h2>
        <div class="mt-6 grid gap-5 sm:grid-cols-3">
          <div v-for="v in values" :key="v.t" class="rounded-xl border border-slate-200 p-5">
            <h3 class="font-display text-base font-semibold text-slate-900">{{ v.t }}</h3>
            <p class="mt-2 text-sm text-slate-600">{{ v.b }}</p>
          </div>
        </div>
      </section>

      <section class="border-t border-slate-200 py-14">
        <h2 class="font-display text-2xl font-semibold text-slate-900">{{ t('home.howTitle') }}</h2>
        <ol class="mt-6 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
          <li v-for="(s, i) in steps" :key="s.t" class="rounded-xl border border-slate-200 p-5">
            <span class="font-display text-sm font-bold text-brand-600">{{ i + 1 }}</span>
            <h3 class="mt-1 font-display text-base font-semibold text-slate-900">{{ s.t }}</h3>
            <p class="mt-2 text-sm text-slate-600">{{ s.b }}</p>
          </li>
        </ol>
      </section>

      <section class="border-t border-slate-200 py-14">
        <div class="flex items-end justify-between gap-4">
          <h2 class="font-display text-2xl font-semibold text-slate-900">{{ t('home.featuredTitle') }}</h2>
          <NuxtLink :to="localePath('/universities')" class="text-sm text-brand-600 underline">
            {{ t('home.seeAll') }}
          </NuxtLink>
        </div>
        <div v-if="featured.length" class="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <NuxtLink
            v-for="u in featured"
            :key="u.id"
            :to="localePath(`/universities/${u.id}`)"
            class="block rounded-xl border border-slate-200 p-5 no-underline transition-colors hover:border-brand-500"
          >
            <h3 class="font-display text-base font-semibold text-slate-900">{{ u.name }}</h3>
            <p v-if="u.nameCn" class="mt-0.5 text-sm text-slate-500">{{ u.nameCn }}</p>
            <p class="mt-2 text-sm text-slate-600">{{ place(u) }}</p>
          </NuxtLink>
        </div>
        <p v-else class="mt-6 text-slate-600">{{ t('home.featuredEmpty') }}</p>
      </section>
    </NContainer>

    <section class="border-t border-slate-200 bg-slate-50">
      <NContainer>
        <div class="flex flex-col items-start gap-4 py-14 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 class="font-display text-2xl font-semibold text-slate-900">{{ t('home.ctaBandTitle') }}</h2>
            <p class="mt-2 text-slate-600">{{ t('home.ctaBandBody') }}</p>
          </div>
          <NButton :to="localePath('/register')" size="lg">{{ t('home.ctaPrimary') }}</NButton>
        </div>
      </NContainer>
    </section>
  </div>
</template>
