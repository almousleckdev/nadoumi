<script setup lang="ts">
import type { ScholarshipDetail } from '~/types/catalog'

const route = useRoute()
const { t } = useI18n()
const localePath = useLocalePath()
const { status } = useSession()
const slug = computed(() => String(route.params.slug))

const { publicGet } = useApi()
const { data: s } = await useAsyncData(
  () => `scholarship-${slug.value}`,
  () => publicGet<ScholarshipDetail>(`scholarships/${slug.value}`).catch(() => null),
  { watch: [slug] },
)

useSeo(
  s.value?.title ?? t('catalog.scholarshipsTitle'),
  s.value?.summary?.slice(0, 155) ?? t('catalog.scholarshipsSubtitle'),
)

const applyTo = computed(() => localePath(status.value === 'authed' ? '/dashboard' : '/register'))

function fundingLabel(m?: string) {
  return m ? t(`scholarships.funding.${m}`) : ''
}
function langLabel(c?: string | null) {
  return c ? t(`scholarships.lang.${c}`) : ''
}
function feeLabel(kind: string) {
  return t(`scholarships.fee.${kind}`, kind)
}
function dual(rmb?: number | null, usd?: number | null): string | null {
  if (rmb == null && usd == null) return null
  return `¥${(rmb ?? 0).toLocaleString('en')} · $${(usd ?? 0).toLocaleString('en')}`
}
function money(m?: { amountRmb: number, amountUsd: number } | null) {
  return m ? dual(m.amountRmb, m.amountUsd) : null
}

const deadlineInfo = computed(() => {
  const raw = s.value?.deadline
  if (!raw) return { rolling: true, tone: 'ok' as const, days: null as number | null, date: '' }
  const end = new Date(`${raw}T23:59:59`).getTime()
  const days = Math.ceil((end - Date.now()) / 86_400_000)
  // red once 10 days or fewer remain
  const tone = days < 0 ? 'passed' : days <= 10 ? 'urgent' : days <= 45 ? 'soon' : 'ok'
  return { rolling: false, tone, days, date: raw }
})
const deadlineText = computed(() => {
  const d = deadlineInfo.value
  if (d.rolling) return t('scholarships.deadlineRolling')
  if (d.days! < 0) return t('scholarships.deadlinePassed')
  if (d.days === 0) return t('scholarships.deadlineToday')
  return t('scholarships.deadlineDays', { n: d.days })
})

const prose = computed(() => {
  const x = s.value
  if (!x) return [] as { heading: string, body: string }[]
  return [
    { heading: t('scholarships.benefits'), body: x.benefits ?? '' },
    { heading: t('scholarships.requirements'), body: x.requirements ?? '' },
    { heading: t('scholarships.policy'), body: x.policy ?? '' },
  ].filter(p => p.body)
})

const eligibilityRows = computed(() => {
  const e = s.value?.eligibility
  if (!e) return [] as { label: string, value: string }[]
  const rows: { label: string, value: string }[] = []
  if (e.ageMin != null || e.ageMax != null) rows.push({ label: t('scholarships.elig.age'), value: `${e.ageMin ?? ''} to ${e.ageMax ?? ''}`.trim() })
  if (e.gpaMin != null) rows.push({ label: t('scholarships.elig.gpa'), value: `≥ ${e.gpaMin}` })
  if (e.ieltsMin != null) rows.push({ label: 'IELTS', value: `≥ ${e.ieltsMin}` })
  if (e.toeflMin != null) rows.push({ label: 'TOEFL', value: `≥ ${e.toeflMin}` })
  if (e.hskMin != null) rows.push({ label: 'HSK', value: `≥ ${e.hskMin}` })
  if (e.inChina != null) rows.push({ label: t('scholarships.elig.inChina'), value: e.inChina ? t('common.yes') : t('common.no') })
  if (e.acceptedCountries) rows.push({ label: t('scholarships.elig.nationality'), value: e.acceptedCountries })
  return rows
})
</script>

<template>
  <div v-if="s">
    <section class="relative isolate overflow-hidden bg-slate-900 text-white">
      <img
        v-if="s.heroUrl ?? s.heroImageUrl ?? s.coverUrl ?? s.coverImageUrl"
        :src="mediaUrl(s.heroUrl ?? s.heroImageUrl ?? s.coverUrl ?? s.coverImageUrl)"
        alt=""
        class="absolute inset-0 -z-20 h-full w-full object-cover opacity-40"
      >
      <div class="absolute inset-0 -z-10 bg-gradient-to-br from-slate-900 via-slate-900/90 to-brand-900/60" aria-hidden="true" />
      <NContainer>
        <div class="max-w-3xl py-12 sm:py-16">
          <div class="flex flex-wrap items-center gap-2 text-xs font-semibold">
            <span class="rounded-full bg-white/10 px-2.5 py-0.5 ring-1 ring-white/20">{{ fundingLabel(s.fundingModel) }}</span>
            <span v-if="s.hasStipend" class="rounded-full bg-white/10 px-2.5 py-0.5 ring-1 ring-white/20">{{ t('scholarships.withStipend') }}</span>
            <span v-if="s.hot" class="rounded-full bg-amber-400 px-2.5 py-0.5 text-amber-950">{{ t('scholarships.hot') }}</span>
            <span v-else-if="s.featured" class="rounded-full bg-brand-500 px-2.5 py-0.5">{{ t('catalog.featured') }}</span>
          </div>
          <p v-if="s.referenceCode" class="mt-3 font-mono text-xs uppercase tracking-widest text-white/60">{{ s.referenceCode }}</p>
          <h1 class="mt-1 font-display text-3xl font-bold tracking-tight sm:text-4xl">{{ s.title }}</h1>
          <p v-if="s.summary" class="mt-3 text-lg text-slate-200">{{ s.summary }}</p>
          <dl class="mt-5 flex flex-wrap gap-x-8 gap-y-2 text-sm text-slate-300">
            <div><dt class="inline text-slate-400">{{ t('scholarships.colDeadline') }}: </dt><dd class="inline">{{ s.deadline ?? t('catalog.rollingDeadline') }}</dd></div>
            <div><dt class="inline text-slate-400">{{ t('scholarships.colLanguage') }}: </dt><dd class="inline">{{ langLabel(s.teachingLanguage) }}</dd></div>
            <div v-if="s.levels.length"><dt class="inline text-slate-400">{{ t('scholarships.colLevel') }}: </dt><dd class="inline">{{ s.levels.map(l => t(`scholarships.level.${l}`)).join(', ') }}</dd></div>
            <div v-if="s.nonDegreeDuration"><dt class="inline text-slate-400">{{ t('scholarships.nonDegreeDuration') }}: </dt><dd class="inline">{{ t(`scholarships.nonDegree.${s.nonDegreeDuration}`, s.nonDegreeDuration) }}</dd></div>
            <div v-if="s.studyDurationMonths"><dt class="inline text-slate-400">{{ t('scholarships.studyDuration') }}: </dt><dd class="inline">{{ t('scholarships.stipendDuration', { n: s.studyDurationMonths }) }}</dd></div>
            <div v-if="s.applicationChannel"><dt class="inline text-slate-400">{{ t('scholarships.applicationChannel') }}: </dt><dd class="inline">{{ t(`scholarships.channel.${s.applicationChannel}`, s.applicationChannel) }}<span v-if="s.agencyNumber"> ({{ s.agencyNumber }})</span></dd></div>
            <div v-if="[s.city, s.province, s.country].filter(Boolean).length"><dt class="inline text-slate-400">{{ t('scholarships.colLocation') }}: </dt><dd class="inline">{{ [s.city, s.province, s.country].filter(Boolean).join(', ') }}</dd></div>
          </dl>
          <div class="mt-7 flex flex-wrap gap-3">
            <NButton :to="applyTo" size="lg">{{ t('scholarships.applyNow') }}</NButton>
            <NButton :to="localePath('/scholarships')" variant="secondary" size="lg">{{ t('catalog.backToList') }}</NButton>
          </div>
          <p class="mt-2 text-xs text-slate-400">{{ t('scholarships.applyNote') }}</p>
        </div>
      </NContainer>
    </section>

    <NContainer>
      <div class="grid gap-10 py-10 lg:grid-cols-[minmax(0,1fr)_20rem]">
        <div class="min-w-0 space-y-10">
          <section v-for="p in prose" :key="p.heading">
            <h2 class="font-display text-xl font-semibold text-slate-900">{{ p.heading }}</h2>
            <p class="mt-2 whitespace-pre-line leading-7 text-slate-700">{{ p.body }}</p>
          </section>

          <section v-if="eligibilityRows.length || s.eligibility?.notes">
            <h2 class="font-display text-xl font-semibold text-slate-900">{{ t('scholarships.eligibility') }}</h2>
            <dl class="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-3">
              <div v-for="r in eligibilityRows" :key="r.label" class="rounded-xl border border-slate-200 p-3">
                <dt class="text-xs uppercase tracking-wide text-slate-400">{{ r.label }}</dt>
                <dd class="mt-0.5 font-medium text-slate-900">{{ r.value }}</dd>
              </div>
            </dl>
            <p v-if="s.eligibility?.notes" class="mt-3 text-sm text-slate-600">{{ s.eligibility.notes }}</p>
          </section>

          <section v-if="s.fees.length">
            <h2 class="font-display text-xl font-semibold text-slate-900">{{ t('scholarships.fees') }}</h2>
            <div class="mt-3 overflow-x-auto">
              <table class="w-full min-w-[28rem] overflow-hidden rounded-xl border border-slate-200 text-left text-sm">
                <thead class="bg-slate-50 text-xs font-medium uppercase tracking-wide text-slate-500">
                  <tr>
                    <th class="px-4 py-2.5">{{ t('scholarships.colDetails') }}</th>
                    <th class="px-4 py-2.5 text-right">{{ t('scholarships.colPrice') }}</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-slate-100">
                  <tr v-for="(f, i) in s.fees" :key="i" class="odd:bg-white even:bg-slate-50/40">
                    <td class="px-4 py-3 text-slate-700">
                      {{ feeLabel(f.kind) }}
                      <span v-if="f.note" class="block text-xs text-slate-400">{{ f.note }}</span>
                    </td>
                    <td class="px-4 py-3 text-right font-semibold tabular-nums text-red-600">{{ dual(f.amountRmb, f.amountUsd) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>

          <section v-if="s.coverage.length">
            <h2 class="font-display text-xl font-semibold text-slate-900">{{ t('scholarships.coverage') }}</h2>
            <ul class="mt-3 grid gap-2 sm:grid-cols-2">
              <li v-for="(c, i) in s.coverage" :key="i" class="flex items-start gap-2 text-slate-700">
                <span class="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-brand-500" aria-hidden="true" />
                <span>
                  {{ t(`scholarships.coverageKind.${c.kind}`, c.kind) }}
                  <span v-if="c.detail" class="block text-sm text-slate-500">{{ c.detail }}</span>
                </span>
              </li>
            </ul>
          </section>

          <section v-if="s.renewalConditions">
            <h2 class="font-display text-xl font-semibold text-slate-900">{{ t('scholarships.renewal') }}</h2>
            <p class="mt-2 whitespace-pre-line leading-7 text-slate-700">{{ s.renewalConditions }}</p>
          </section>

          <p v-if="s.requiresFinancialProof || s.requiresFoundationYear" class="text-sm text-slate-600">
            <span v-if="s.requiresFinancialProof" class="mr-3">• {{ t('scholarships.requiresFinancialProof') }}</span>
            <span v-if="s.requiresFoundationYear">• {{ t('scholarships.requiresFoundationYear') }}</span>
          </p>

          <section v-if="s.accommodation.length">
            <h2 class="font-display text-xl font-semibold text-slate-900">{{ t('scholarships.accommodation') }}</h2>
            <div class="mt-3 overflow-x-auto">
              <table class="w-full min-w-[34rem] overflow-hidden rounded-xl border border-slate-200 text-left text-sm">
                <thead class="bg-slate-50 text-xs font-medium uppercase tracking-wide text-slate-500">
                  <tr>
                    <th class="px-4 py-2.5">{{ t('scholarships.colRoom') }}</th>
                    <th class="px-4 py-2.5">{{ t('scholarships.colDetails') }}</th>
                    <th class="px-4 py-2.5 text-right">{{ t('scholarships.colPrice') }}</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-slate-100">
                  <tr v-for="(a, i) in s.accommodation" :key="i" class="odd:bg-white even:bg-slate-50/40">
                    <td class="px-4 py-3 font-medium text-slate-900">{{ t(`scholarships.room.${a.roomType}`, a.roomType) }}</td>
                    <td class="px-4 py-3 text-slate-600">{{ a.note || '' }}</td>
                    <td class="px-4 py-3 text-right font-semibold tabular-nums text-red-600">
                      {{ dual(a.amountRmb, a.amountUsd) ?? t('scholarships.free') }}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>

          <section v-if="s.documentRequirements.length">
            <h2 class="font-display text-xl font-semibold text-slate-900">{{ t('scholarships.documents') }}</h2>
            <p class="mt-1 text-sm text-slate-500">{{ t('scholarships.documentsNote') }}</p>
            <div class="mt-3 overflow-x-auto">
              <table class="w-full min-w-[36rem] overflow-hidden rounded-xl border border-slate-200 text-left text-sm">
                <thead class="bg-slate-50 text-xs font-medium uppercase tracking-wide text-slate-500">
                  <tr>
                    <th class="px-4 py-2.5">{{ t('scholarships.colDoc') }}</th>
                    <th class="px-4 py-2.5">{{ t('scholarships.colRequirement') }}</th>
                    <th class="px-4 py-2.5">{{ t('scholarships.colNotes') }}</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-slate-100">
                  <tr v-for="d in s.documentRequirements" :key="d.docType" class="odd:bg-white even:bg-slate-50/40">
                    <td class="px-4 py-3 font-medium text-slate-900">{{ t(`scholarships.doc.${d.docType}`, d.docType) }}</td>
                    <td class="px-4 py-3">
                      <span
                        class="inline-flex rounded-full px-2 py-0.5 text-xs font-semibold"
                        :class="d.mandatory ? 'bg-red-100 text-red-700' : 'bg-slate-100 text-slate-600'"
                      >
                        {{ d.mandatory ? t('scholarships.required') : t('scholarships.optional') }}
                      </span>
                    </td>
                    <td class="px-4 py-3 text-slate-600">{{ d.note || '' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>
        </div>

        <aside class="space-y-6 lg:sticky lg:top-24 lg:self-start">
          <div v-if="s.stipends.length" class="rounded-xl border border-slate-200 bg-white p-5">
            <h2 class="font-display text-sm font-semibold uppercase tracking-wide text-slate-500">{{ t('scholarships.stipend') }}</h2>
            <ul class="mt-2 space-y-3">
              <li v-for="st in s.stipends" :key="st.level">
                <p class="text-xs uppercase tracking-wide text-slate-400">{{ t(`scholarships.level.${st.level}`, st.level) }}</p>
                <p class="font-display text-lg font-semibold text-slate-900">
                  {{ dual(st.amountRmb, st.amountUsd) }}
                  <span class="text-sm font-normal text-slate-500">/ {{ t(`scholarships.freq.${st.frequency}`, st.frequency) }}</span>
                </p>
                <p v-if="st.durationMonths" class="text-sm text-slate-600">{{ t('scholarships.stipendDuration', { n: st.durationMonths }) }}</p>
                <p v-if="st.conditions" class="text-sm text-slate-500">{{ st.conditions }}</p>
              </li>
            </ul>
          </div>

          <div v-if="money(s.applicationFee) || money(s.serviceFee)" class="rounded-xl border border-slate-200 bg-white p-5 text-sm">
            <h2 class="font-display text-sm font-semibold uppercase tracking-wide text-slate-500">{{ t('scholarships.upfront') }}</h2>
            <ul class="mt-3 space-y-1 text-slate-700">
              <li v-if="money(s.applicationFee)">{{ t('scholarships.fee.APPLICATION') }}: {{ money(s.applicationFee) }}</li>
              <li v-if="money(s.serviceFee)">{{ t('scholarships.fee.NADOUMI_SERVICE') }}: {{ money(s.serviceFee) }}</li>
            </ul>
          </div>

          <div v-if="s.intakes.length" class="rounded-xl border border-slate-200 bg-white p-5 text-sm">
            <h2 class="font-display text-sm font-semibold uppercase tracking-wide text-slate-500">{{ t('scholarships.intakes') }}</h2>
            <ul class="mt-3 space-y-1 text-slate-700">
              <li v-for="(it, i) in s.intakes" :key="i">
                {{ t(`scholarships.intake.${it.term}`, it.term) }}
                <span v-if="it.applicationClose" class="text-slate-400"> · {{ t('scholarships.closes') }} {{ it.applicationClose }}</span>
              </li>
            </ul>
          </div>

          <!-- animated deadline card — sits below Upfront fee + Intakes -->
          <div
            class="deadline-banner relative overflow-hidden rounded-xl border p-5"
            :class="{
              'border-red-200 bg-red-50 text-red-900': deadlineInfo.tone === 'urgent' || deadlineInfo.tone === 'passed',
              'border-amber-200 bg-amber-50 text-amber-900': deadlineInfo.tone === 'soon',
              'border-emerald-200 bg-emerald-50 text-emerald-900': deadlineInfo.tone === 'ok',
            }"
          >
            <span class="deadline-banner__glow" aria-hidden="true" />
            <div class="relative">
              <div class="flex items-center gap-2">
                <span
                  class="deadline-banner__pulse inline-block h-2.5 w-2.5 rounded-full"
                  :class="{
                    'bg-red-500': deadlineInfo.tone === 'urgent' || deadlineInfo.tone === 'passed',
                    'bg-amber-500': deadlineInfo.tone === 'soon',
                    'bg-emerald-500': deadlineInfo.tone === 'ok',
                  }"
                  aria-hidden="true"
                />
                <p class="text-xs font-semibold uppercase tracking-[0.14em] opacity-70">{{ t('scholarships.deadlineLabel') }}</p>
              </div>
              <p class="mt-1.5 font-display text-2xl font-bold tracking-tight">{{ deadlineText }}</p>
              <p v-if="!deadlineInfo.rolling" class="mt-0.5 text-sm tabular-nums opacity-70">{{ deadlineInfo.date }}</p>
            </div>
          </div>
        </aside>
      </div>
    </NContainer>
  </div>

  <div v-else>
    <PageHero :title="t('catalog.scholarshipsTitle')" />
    <NContainer>
      <p class="py-10 text-slate-600">{{ t('scholarships.notFound') }}</p>
      <NuxtLink :to="localePath('/scholarships')" class="text-sm font-medium text-brand-700">← {{ t('catalog.backToList') }}</NuxtLink>
    </NContainer>
  </div>
</template>

<style scoped>
.deadline-banner__glow {
  position: absolute;
  inset: -40% -10%;
  background: radial-gradient(60% 60% at 20% 30%, rgba(255, 255, 255, 0.55), transparent 70%);
  transform: translateX(-30%);
  animation: deadline-sweep 6s ease-in-out infinite;
}
.deadline-banner__pulse {
  animation: deadline-pulse 1.8s ease-in-out infinite;
}
@keyframes deadline-sweep {
  0%, 100% { transform: translateX(-30%); opacity: 0.6; }
  50% { transform: translateX(30%); opacity: 1; }
}
@keyframes deadline-pulse {
  0%, 100% { transform: scale(1); opacity: 1; box-shadow: 0 0 0 0 currentColor; }
  50% { transform: scale(1.35); opacity: 0.75; box-shadow: 0 0 0 6px transparent; }
}
@media (prefers-reduced-motion: reduce) {
  .deadline-banner__glow,
  .deadline-banner__pulse { animation: none; }
}
</style>
