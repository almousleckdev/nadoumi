<script setup lang="ts">
import type { ProgramDetail, ProgramMajor } from '~/types/catalog'

const route = useRoute()
const { t } = useI18n()
const localePath = useLocalePath()
const { status } = useSession()
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

const applyTo = computed(() => localePath(status.value === 'authed' ? '/dashboard' : '/register'))
const universityTo = computed(() =>
  p.value ? localePath(`/universities/${p.value.universitySlug ?? p.value.universityId}`) : '#')

const levelLabels = computed(() => {
  const x = p.value
  if (!x) return [] as string[]
  return x.programType === 'DEGREE' && x.levels?.length
    ? x.levels.map(l => t(`program.level.${l}`))
    : [t(`program.level.${x.programType}`)]
})

const tuitionCny = computed(() => {
  const x = p.value
  return x?.tuitionAmount != null
    ? `${x.tuitionCurrency ?? '¥'} ${x.tuitionAmount.toLocaleString('en')}`.trim()
    : ''
})
const tuitionUsd = computed(() =>
  p.value?.tuitionAmountUsd != null ? `$${p.value.tuitionAmountUsd.toLocaleString('en')}` : '')

const facts = computed(() => {
  const x = p.value
  if (!x) return [] as { label: string, value: string }[]
  return [
    { label: t('program.language'), value: x.teachingLanguage ? t(`program.lang.${x.teachingLanguage}`) : '' },
    { label: t('program.field'), value: x.field ?? '' },
    { label: t('program.durationLabel'), value: x.durationMonths ? t('program.duration', { months: x.durationMonths }) : '' },
    { label: t('program.tuition'), value: tuitionCny.value },
  ].filter(f => f.value)
})

// group majors by department, then fall back to a flat list
const majorGroups = computed(() => {
  const list = p.value?.majors ?? []
  const byDept = new Map<string, ProgramMajor[]>()
  for (const m of list) {
    const key = m.departmentName || ''
    if (!byDept.has(key)) byDept.set(key, [])
    byDept.get(key)!.push(m)
  }
  return [...byDept.entries()].map(([dept, majors]) => ({ dept, majors }))
})
</script>

<template>
  <div v-if="p">
    <!-- hero -->
    <section class="relative isolate overflow-hidden bg-slate-900 text-white">
      <img
        v-if="p.imageUrl"
        :src="mediaUrl(p.imageUrl)"
        alt=""
        class="absolute inset-0 -z-20 h-full w-full object-cover opacity-35"
      >
      <div class="absolute inset-0 -z-10 bg-gradient-to-br from-slate-950 via-slate-900 to-brand-900/70" aria-hidden="true" />
      <NContainer>
        <div class="max-w-3xl py-14 sm:py-20">
          <div class="flex flex-wrap items-center gap-2">
            <span
              v-for="lv in levelLabels"
              :key="lv"
              class="rounded-full bg-white/10 px-2.5 py-0.5 text-xs font-semibold ring-1 ring-white/20"
            >{{ lv }}</span>
            <span
              v-if="p.teachingLanguage"
              class="rounded-full bg-white/10 px-2.5 py-0.5 text-xs font-medium ring-1 ring-white/20"
            >{{ t(`program.lang.${p.teachingLanguage}`) }}</span>
            <span v-if="p.hot" class="rounded-full bg-amber-400 px-2.5 py-0.5 text-xs font-semibold text-amber-950">{{ t('program.hot') }}</span>
          </div>
          <h1 class="mt-4 font-display text-3xl font-bold tracking-tight sm:text-4xl">{{ p.name }}</h1>
          <p v-if="p.nameCn" class="mt-1 text-lg text-white/70">{{ p.nameCn }}</p>
          <NuxtLink
            v-if="p.universityName"
            :to="universityTo"
            class="mt-4 inline-flex items-center gap-1.5 text-sm font-medium text-white/90 hover:text-white"
          >
            {{ t('program.offeredBy', { university: p.universityName }) }}
            <span aria-hidden="true">→</span>
          </NuxtLink>
          <div class="mt-7 flex flex-wrap gap-3">
            <NButton :to="applyTo" size="lg">{{ t('program.applyNow') }}</NButton>
            <NButton :to="universityTo" variant="secondary" size="lg">{{ t('program.view') }}</NButton>
          </div>
        </div>
      </NContainer>
    </section>

    <NContainer>
      <div class="grid gap-10 py-12 lg:grid-cols-[minmax(0,1fr)_20rem]">
        <div class="min-w-0 space-y-12">
          <!-- facts -->
          <dl v-if="facts.length" class="grid grid-cols-2 gap-3 sm:grid-cols-4">
            <div v-for="f in facts" :key="f.label" class="rounded-xl border border-slate-200 bg-white p-4">
              <dt class="text-xs uppercase tracking-wide text-slate-400">{{ f.label }}</dt>
              <dd class="mt-1 font-display text-base font-semibold text-slate-900">{{ f.value }}</dd>
            </div>
          </dl>

          <!-- overview -->
          <section v-if="p.summary">
            <h2 class="font-display text-xl font-semibold text-slate-900">{{ t('program.fallbackTitle') }}</h2>
            <p class="mt-2 whitespace-pre-line leading-7 text-slate-700">{{ p.summary }}</p>
          </section>

          <!-- majors -->
          <section>
            <h2 class="font-display text-xl font-semibold text-slate-900">{{ t('program.majors') }}</h2>
            <p class="mt-1 text-sm text-slate-500">{{ t('program.majorsIntro') }}</p>
            <div v-if="majorGroups.length" class="mt-4 space-y-5">
              <div v-for="g in majorGroups" :key="g.dept">
                <p v-if="g.dept" class="text-xs font-semibold uppercase tracking-wide text-brand-600">{{ g.dept }}</p>
                <ul class="mt-2 grid gap-2 sm:grid-cols-2">
                  <li
                    v-for="m in g.majors"
                    :key="m.id"
                    class="flex items-start gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-700"
                  >
                    <span class="mt-1 h-1.5 w-1.5 shrink-0 rounded-full bg-brand-500" aria-hidden="true" />
                    <span>{{ m.name }}<span v-if="m.nameCn" class="text-slate-400"> · {{ m.nameCn }}</span></span>
                  </li>
                </ul>
              </div>
            </div>
            <p v-else class="mt-3 text-sm text-slate-500">{{ t('program.noMajors') }}</p>
          </section>

          <!-- intakes -->
          <section v-if="p.intakes.length">
            <h2 class="font-display text-xl font-semibold text-slate-900">{{ t('program.intakes') }}</h2>
            <div class="mt-3 overflow-x-auto">
              <table class="w-full min-w-[30rem] overflow-hidden rounded-xl border border-slate-200 text-left text-sm">
                <thead class="bg-slate-50 text-xs font-medium uppercase tracking-wide text-slate-500">
                  <tr>
                    <th class="px-4 py-2.5">{{ t('program.term') }}</th>
                    <th class="px-4 py-2.5">{{ t('program.opens') }}</th>
                    <th class="px-4 py-2.5">{{ t('program.closes') }}</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-slate-100">
                  <tr v-for="i in p.intakes" :key="i.id" class="odd:bg-white even:bg-slate-50/40">
                    <td class="px-4 py-3 font-medium text-slate-900">{{ t(`program.intake.${i.term}`, i.term) }}</td>
                    <td class="px-4 py-3 text-slate-600">{{ i.applicationOpen || t('program.rolling') }}</td>
                    <td class="px-4 py-3 text-slate-600">{{ i.applicationClose || t('program.rolling') }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>
        </div>

        <!-- aside -->
        <aside class="space-y-5 lg:sticky lg:top-24 lg:self-start">
          <div v-if="tuitionCny" class="rounded-2xl border border-slate-200 bg-white p-5">
            <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">{{ t('program.tuition') }}</p>
            <p class="mt-1 font-display text-2xl font-bold text-slate-900">{{ tuitionCny }}</p>
            <p v-if="tuitionUsd" class="text-sm text-slate-500">≈ {{ tuitionUsd }}</p>
            <p class="mt-2 text-xs text-slate-400">{{ t('program.tuitionNote') }}</p>
          </div>

          <div v-if="p.universityName" class="rounded-2xl border border-slate-200 bg-white p-5">
            <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">{{ t('program.universityLabel') }}</p>
            <NuxtLink :to="universityTo" class="mt-1 block font-display text-base font-semibold text-slate-900 hover:text-brand-800">
              {{ p.universityName }}
            </NuxtLink>
            <NuxtLink :to="universityTo" class="mt-2 inline-flex items-center gap-1 text-sm font-medium text-brand-700 hover:text-brand-800">
              {{ t('program.view') }} <span aria-hidden="true">→</span>
            </NuxtLink>
          </div>

          <div class="rounded-2xl border border-brand-200 bg-brand-50 p-5">
            <p class="text-sm text-brand-900">{{ t('program.applyVia', { university: p.universityName ?? '' }) }}</p>
            <NButton :to="applyTo" size="sm" class="mt-3">{{ t('program.applyNow') }}</NButton>
          </div>
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
