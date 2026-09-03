<script setup lang="ts">
import { imagery } from '~/data/imagery'

const { t } = useI18n()
const localePath = useLocalePath()
const router = useRouter()

const q = ref('')
function search() {
  const query = q.value.trim()
  router.push(localePath(query ? `/universities?q=${encodeURIComponent(query)}` : '/universities'))
}
</script>

<template>
  <section class="relative overflow-hidden bg-slate-50">
    <div
      class="pointer-events-none absolute -right-24 -top-24 h-96 w-96 rounded-full bg-brand-200/40 blur-3xl"
      aria-hidden="true"
    />
    <NContainer>
      <div class="grid items-center gap-10 py-14 lg:grid-cols-[1.05fr_1fr] lg:gap-14 lg:py-20">
        <div class="max-w-xl">
          <p class="text-xs font-semibold uppercase tracking-[0.14em] text-brand-600">
            {{ t('home.hero.eyebrow') }}
          </p>
          <h1 class="mt-3 font-display text-4xl font-extrabold leading-[1.1] tracking-tight text-slate-900 sm:text-5xl">
            {{ t('home.hero.title') }}
          </h1>
          <p class="mt-4 text-lg text-slate-600">{{ t('home.hero.subtitle') }}</p>

          <div class="mt-7 flex flex-wrap gap-3">
            <NButton :to="localePath('/scholarships')" size="lg">{{ t('home.hero.ctaScholarships') }}</NButton>
            <NButton :to="localePath('/universities')" variant="secondary" size="lg">
              {{ t('home.hero.ctaUniversities') }}
            </NButton>
          </div>

          <form class="mt-6 flex max-w-md items-center gap-2" role="search" @submit.prevent="search">
            <label for="hero-search" class="sr-only">{{ t('home.hero.searchLabel') }}</label>
            <input
              id="hero-search"
              v-model="q"
              type="search"
              :placeholder="t('home.hero.searchPlaceholder')"
              class="w-full rounded-md border border-slate-300 bg-white px-3.5 py-2.5 text-base outline-none focus-visible:border-brand-500"
            >
            <NButton type="submit" size="md">{{ t('common.search') }}</NButton>
          </form>
          <p class="mt-2 text-xs text-slate-500">{{ t('home.hero.searchNote') }}</p>
        </div>

        <div class="relative hidden lg:block">
          <MediaFigure
            :src="imagery.heroPrimary.src"
            :alt="imagery.heroPrimary.alt"
            ratio="4/5"
            rounded="2xl"
            sizes="lg:38vw"
            eager
          />
          <div class="absolute -bottom-8 -left-10 w-48 overflow-hidden rounded-xl border-4 border-slate-50 shadow-md">
            <MediaFigure
              :src="imagery.graduation.src"
              :alt="imagery.graduation.alt"
              ratio="1/1"
              rounded="none"
              sizes="200px"
            />
          </div>
        </div>
      </div>

      <div class="pb-10 lg:hidden">
        <MediaFigure
          :src="imagery.heroPrimary.src"
          :alt="imagery.heroPrimary.alt"
          ratio="16/10"
          rounded="xl"
          sizes="100vw"
          eager
        />
      </div>
    </NContainer>
  </section>
</template>
