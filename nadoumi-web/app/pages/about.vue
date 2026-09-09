<script setup lang="ts">
import { imagery } from '~/data/imagery'
import { CONTACT, telHref } from '~/data/contact'
import whoImage from '~/assets/images/ceo.jpeg'
import helpImage from '~/assets/images/apply.png'
import ceoPhoto from '~/assets/team/ceo.jpeg'
import team6Photo from '~/assets/team/team6.jpg'
import femmePhoto from '~/assets/team/femme.jpg'
import engineerPhoto from '~/assets/team/atalibag.jpeg'

const { t } = useI18n()
const localePath = useLocalePath()
useSeo(t('about.title'), t('about.lead'))

const team = computed(() => [
  { photo: ceoPhoto, name: t('about.team1name'), role: t('about.team1role') },
  { photo: team6Photo, name: t('about.team2name'), role: t('about.team2role') },
  { photo: femmePhoto, name: t('about.team3name'), role: t('about.team3role') },
  { photo: engineerPhoto, name: t('about.team4name'), role: t('about.team4role') },
])

const values = computed(() => [
  { title: t('about.v1t'), body: t('about.v1b') },
  { title: t('about.v2t'), body: t('about.v2b') },
  { title: t('about.v3t'), body: t('about.v3b') },
  { title: t('about.v4t'), body: t('about.v4b') },
])
const help = computed(() => [
  t('about.h1'), t('about.h2'), t('about.h3'), t('about.h4'), t('about.h5'),
])
</script>

<template>
  <div>
    <!-- hero -->
    <section class="relative isolate overflow-hidden bg-slate-900 text-white">
      <NuxtImg
        :src="imagery.campusLife.src"
        :alt="imagery.campusLife.alt"
        sizes="100vw"
        class="absolute inset-0 -z-10 h-full w-full object-cover opacity-40"
        :modifiers="{ fit: 'crop', auto: 'format' }"
      />
      <div class="absolute inset-0 -z-10 bg-slate-950/60" aria-hidden="true" />
      <NContainer>
        <div class="max-w-2xl py-20 sm:py-28">
          <p class="text-xs font-semibold uppercase tracking-[0.14em] text-brand-300">{{ t('about.eyebrow') }}</p>
          <h1 class="mt-2 font-display text-4xl font-bold tracking-tight sm:text-5xl">{{ t('about.title') }}</h1>
          <p class="mt-4 text-lg text-slate-200">{{ t('about.lead') }}</p>
        </div>
      </NContainer>
    </section>

    <NContainer>
      <!-- who we are -->
      <section class="grid gap-10 py-14 lg:grid-cols-2 lg:gap-16 lg:py-20">
        <div class="max-w-lg">
          <h2 class="font-display text-2xl font-bold tracking-tight text-slate-900">{{ t('about.whoTitle') }}</h2>
          <p class="mt-3 leading-7 text-slate-700">{{ t('about.whoBody') }}</p>
          <p class="mt-3 leading-7 text-slate-700">{{ t('about.whoBody2') }}</p>
        </div>
        <MediaFigure :src="whoImage" :alt="t('about.whoTitle')" local ratio="4/5" position="top" rounded="2xl" class="mx-auto w-full max-w-sm lg:max-w-none" />
      </section>

      <!-- mission + vision -->
      <section class="grid gap-6 border-t border-slate-200 py-14 sm:grid-cols-2">
        <div class="rounded-2xl border border-slate-200 bg-white p-7">
          <h2 class="font-display text-lg font-semibold text-slate-900">{{ t('about.missionTitle') }}</h2>
          <p class="mt-2 leading-7 text-slate-700">{{ t('about.missionBody') }}</p>
        </div>
        <div class="rounded-2xl border border-slate-200 bg-white p-7">
          <h2 class="font-display text-lg font-semibold text-slate-900">{{ t('about.visionTitle') }}</h2>
          <p class="mt-2 leading-7 text-slate-700">{{ t('about.visionBody') }}</p>
        </div>
      </section>

      <!-- values -->
      <section class="border-t border-slate-200 py-14">
        <h2 class="font-display text-2xl font-bold tracking-tight text-slate-900">{{ t('about.valuesTitle') }}</h2>
        <div class="mt-6 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
          <div v-for="v in values" :key="v.title" class="rounded-xl border border-slate-200 p-5">
            <h3 class="font-display text-base font-semibold text-slate-900">{{ v.title }}</h3>
            <p class="mt-2 text-sm leading-6 text-slate-600">{{ v.body }}</p>
          </div>
        </div>
      </section>

      <!-- how we help -->
      <section class="grid gap-10 border-t border-slate-200 py-14 lg:grid-cols-2 lg:gap-16">
        <MediaFigure :src="helpImage" :alt="t('about.helpTitle')" local ratio="4/3" rounded="2xl" />
        <div class="max-w-lg">
          <h2 class="font-display text-2xl font-bold tracking-tight text-slate-900">{{ t('about.helpTitle') }}</h2>
          <ol class="mt-4 space-y-3">
            <li v-for="(item, i) in help" :key="item" class="flex gap-3 text-slate-700">
              <span class="font-display text-sm font-bold text-brand-600">{{ i + 1 }}</span>
              <span>{{ item }}</span>
            </li>
          </ol>
        </div>
      </section>

      <!-- team -->
      <section class="border-t border-slate-200 py-14">
        <h2 class="font-display text-2xl font-bold tracking-tight text-slate-900">{{ t('about.teamTitle') }}</h2>
        <p class="mt-2 max-w-xl text-slate-600">{{ t('about.teamIntro') }}</p>
        <ul class="mt-8 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
          <li
            v-for="m in team"
            :key="m.role"
            class="group overflow-hidden rounded-2xl border border-slate-200 bg-white transition-shadow hover:shadow-md"
          >
            <div class="aspect-square overflow-hidden bg-slate-100">
              <img
                :src="m.photo"
                :alt="m.name || m.role"
                loading="lazy"
                decoding="async"
                class="h-full w-full object-cover object-center transition-transform duration-300 group-hover:scale-[1.03]"
              >
            </div>
            <div class="p-4">
              <p v-if="m.name" class="font-display text-base font-semibold text-slate-900">{{ m.name }}</p>
              <p class="text-sm text-brand-700">{{ m.role }}</p>
            </div>
          </li>
        </ul>
      </section>

      <!-- contact / location -->
      <section class="grid gap-6 border-t border-slate-200 py-14 sm:grid-cols-2 lg:grid-cols-3">
        <div class="rounded-xl border border-slate-200 bg-white p-6">
          <h3 class="font-display text-sm font-semibold uppercase tracking-wide text-slate-500">{{ t('about.contactTitle') }}</h3>
          <ul class="mt-3 space-y-1 text-sm">
            <li v-for="e in CONTACT.emails" :key="e">
              <a :href="`mailto:${e}`" class="text-brand-700 hover:text-brand-800">{{ e }}</a>
            </li>
          </ul>
          <ul class="mt-3 space-y-1 text-sm text-slate-700">
            <li v-for="p in CONTACT.phones" :key="p">
              <a :href="telHref(p)" class="hover:text-slate-900">{{ p }}</a>
            </li>
          </ul>
          <NuxtLink :to="localePath('/contact')" class="mt-3 inline-block text-sm font-medium text-brand-700 hover:text-brand-800">
            {{ t('about.contactCta') }} →
          </NuxtLink>
        </div>
        <div class="rounded-xl border border-slate-200 bg-white p-6">
          <h3 class="font-display text-sm font-semibold uppercase tracking-wide text-slate-500">{{ t('about.officeTitle') }}</h3>
          <p class="mt-3 text-sm text-slate-700">{{ CONTACT.officeEn }}</p>
          <p class="mt-1 text-sm text-slate-500">{{ CONTACT.officeCn }}</p>
        </div>
        <div class="rounded-xl border border-slate-200 bg-white p-6">
          <h3 class="font-display text-sm font-semibold uppercase tracking-wide text-slate-500">{{ t('about.hoursTitle') }}</h3>
          <p class="mt-3 text-sm text-slate-700">{{ CONTACT.hours }}</p>
        </div>
      </section>
    </NContainer>

    <CtaBand
      :image="imagery.graduation"
      :eyebrow="t('home.cta.eyebrow')"
      :title="t('about.ctaTitle')"
      :body="t('about.ctaBody')"
    >
      <template #actions>
        <NButton :to="localePath('/register')" size="lg">{{ t('home.cta.apply') }}</NButton>
        <NButton :to="localePath('/contact')" variant="secondary" size="lg">{{ t('home.cta.contact') }}</NButton>
      </template>
    </CtaBand>
  </div>
</template>
