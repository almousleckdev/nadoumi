<script setup lang="ts">
import type { ProgramDetail } from '~/types/catalog'

const route = useRoute()
const { t } = useI18n()
const localePath = useLocalePath()
const slug = computed(() => String(route.params.slug))

const { publicGet } = useApi()
const { data: p } = await useAsyncData(
  () => `program-${slug.value}`,
  () => publicGet<ProgramDetail>(`programs/${slug.value}`).catch(() => null),
  { watch: [slug] },
)

useSeo(
  p.value?.name ?? t('program.fallbackTitle'),
  p.value?.summary?.slice(0, 155) ?? t('home.programDiscovery.description'),
)

const tuition = computed(() =>
  p.value?.tuitionAmount != null
    ? `${p.value.tuitionCurrency ?? ''} ${p.value.tuitionAmount.toLocaleString('en')}`.trim()
    : '',
)

const facts = computed(() => {
  const x = p.value
  if (!x) return [] as { label: string, value: string }[]
  return [
    { label: t('program.language'), value: x.teachingLanguage ? t(`program.lang.${x.teachingLanguage}`) : '' },
    { label: t('program.field'), value: x.field ?? '' },
    { label: t('program.tuition'), value: tuition.value },
    { label: t('program.durationLabel'), value: x.durationMonths ? t('program.duration', { months: x.durationMonths }) : '' },
  ].filter(f => f.value)
})

function intakeWindow(open?: string | null, close?: string | null): string {
  if (open && close) return t('program.applyWindow', { open, close })
  if (open) return t('program.applyFrom', { open })
  if (close) return t('program.applyUntil', { close })
  return ''
}
</script>

<template>
  <div v-if="p">
    <section class="relative isolate overflow-hidden bg-slate-900 text-white">
      <div class="absolute inset-0 -z-10 bg-gradient-to-br from-slate-900 via-slate-900 to-brand-900/60" aria-hidden="true" />
      <NContainer>
        <div class="py-12 sm:py-16">
          <div class="flex flex-wrap items-center gap-2">
            <span class="rounded-full bg-white/10 px-2.5 py-0.5 text-xs font-medium ring-1 ring-white/20">
              {{ t(`program.level.${p.programType}`) }}
            </span>
            <span
              v-if="p.teachingLanguage"
              class="rounded-full bg-white/10 px-2.5 py-0.5 text-xs font-medium ring-1 ring-white/20"
            >
              {{ t(`program.lang.${p.teachingLanguage}`) }}
            </span>
          </div>
          <h1 class="mt-3 font-display text-3xl font-bold tracking-tight sm:text-4xl">{{ p.name }}</h1>
          <p v-if="p.nameCn" class="mt-1 text-lg text-white/70">{{ p.nameCn }}</p>
          <NuxtLink
            v-if="p.universityName"
            :to="localePath(`/universities/${p.universitySlug ?? p.universityId}`)"
            class="mt-3 inline-flex items-center gap-1.5 text-sm font-medium text-white/90 hover:text-white"
          >
            {{ t('program.offeredBy', { university: p.universityName }) }}
            <span aria-hidden="true">→</span>
          </NuxtLink>
        </div>
      </NContainer>
    </section>

    <NContainer>
      <div class="grid gap-10 py-10 lg:grid-cols-[minmax(0,1fr)_18rem]">
        <div class="min-w-0 space-y-12">
          <dl v-if="facts.length" class="grid grid-cols-2 gap-3 sm:grid-cols-3">
            <div v-for="f in facts" :key="f.label" class="rounded-xl border border-slate-200 bg-white p-4">
              <dt class="text-xs uppercase tracking-wide text-slate-400">{{ f.label }}</dt>
              <dd class="mt-1 font-display text-lg font-semibold text-slate-900">{{ f.value }}</dd>
            </div>
          </dl>

          <section v-if="p.summary">
            <p class="whitespace-pre-line leading-7 text-slate-700">{{ p.summary }}</p>
          </section>

          <section v-if="p.majors.length">
            <h2 class="font-display text-xl font-semibold text-slate-900">{{ t('program.majors') }}</h2>
            <ul class="mt-3 grid gap-2 sm:grid-cols-2">
              <li v-for="m in p.majors" :key="m.id" class="flex gap-2 text-slate-700">
                <span class="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-brand-500" aria-hidden="true" />
                <span>{{ m.name }}<span v-if="m.nameCn" class="text-slate-400"> · {{ m.nameCn }}</span></span>
              </li>
            </ul>
          </section>

          <section v-if="p.intakes.length">
            <h2 class="font-display text-xl font-semibold text-slate-900">{{ t('program.intakes') }}</h2>
            <ul class="mt-3 space-y-2">
              <li v-for="i in p.intakes" :key="i.id" class="flex items-baseline justify-between gap-3 border-b border-slate-100 pb-2 text-slate-700">
                <span class="font-medium">{{ t(`program.intake.${i.term}`) }}</span>
                <span class="text-sm text-slate-500">{{ intakeWindow(i.applicationOpen, i.applicationClose) }}</span>
              </li>
            </ul>
          </section>
        </div>

        <aside class="space-y-6 lg:sticky lg:top-24 lg:self-start">
          <NuxtLink
            v-if="p.universityName"
            :to="localePath(`/universities/${p.universitySlug ?? p.universityId}`)"
            class="inline-flex items-center gap-1.5 text-sm font-medium text-brand-700 hover:text-brand-800"
          >
            <span aria-hidden="true">←</span> {{ t('program.backToUniversity', { university: p.universityName }) }}
          </NuxtLink>
        </aside>
      </div>
    </NContainer>
  </div>

  <div v-else>
    <PageHero :title="t('program.fallbackTitle')" />
    <NContainer>
      <p class="py-10 text-slate-600">{{ t('program.notAvailable') }}</p>
      <NuxtLink :to="localePath('/universities')" class="text-sm font-medium text-brand-700 hover:text-brand-800">
        ← {{ t('catalog.allUniversities') }}
      </NuxtLink>
    </NContainer>
  </div>
</template>
