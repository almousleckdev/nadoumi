<script setup lang="ts">
import type { ProgramDetail } from '~/types/catalog'
import { programTuition } from '~/utils/programDisplay'

const { t } = useI18n()
const localePath = useLocalePath()
const { status } = useSession()
const { data: p } = await usePublicDetail<ProgramDetail>('programs')

useSeo(
  p.value?.name ?? t('program.fallbackTitle'),
  p.value?.summary?.slice(0, 155) ?? t('home.programDiscovery.description'),
)

const applyTo = computed(() => localePath(status.value === 'authed' ? '/dashboard' : '/register'))
const universityTo = computed(() =>
  p.value ? localePath(`/universities/${p.value.universitySlug ?? p.value.universityId}`) : '#')

const tuition = computed(() => (p.value ? programTuition(p.value) : { cny: '', usd: '' }))

const facts = computed(() => {
  const x = p.value
  if (!x) return [] as { label: string, value: string }[]
  return [
    { label: t('program.language'), value: x.teachingLanguage ? t(`program.lang.${x.teachingLanguage}`) : '' },
    { label: t('program.field'), value: x.field ?? '' },
    { label: t('program.durationLabel'), value: x.durationMonths ? t('program.duration', { months: x.durationMonths }) : '' },
    { label: t('program.tuition'), value: tuition.value.cny },
  ].filter(f => f.value)
})
</script>

<template>
  <div v-if="p">
    <ProgramHero :program="p" :apply-to="applyTo" :university-to="universityTo" />

    <NContainer>
      <FactTiles v-if="facts.length" :items="facts" class="relative z-10 mt-6 lg:-mt-6" />
      <div class="grid gap-8 py-8 lg:grid-cols-[minmax(0,1fr)_21rem] lg:gap-10 lg:py-10">
        <div class="min-w-0 space-y-6">
          <DetailCard v-if="p.summary" :title="t('program.fallbackTitle')">
            <p class="whitespace-pre-line leading-7 text-slate-700">{{ p.summary }}</p>
          </DetailCard>

          <ProgramMajors :majors="p.majors ?? []" />

          <ProgramIntakes :intakes="p.intakes" />
        </div>

        <ProgramSidebar :program="p" :apply-to="applyTo" :university-to="universityTo" class="order-first lg:order-none" />
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
