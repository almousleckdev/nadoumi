<script setup lang="ts">
import type { ScholarshipDetail } from '~/types/catalog'
import { dualMoney, eligibilityRows as buildEligibilityRows } from '~/utils/scholarshipDisplay'

const props = defineProps<{ s: ScholarshipDetail }>()

const { t } = useI18n()

function feeLabel(kind: string) {
  return t(`scholarships.fee.${kind}`, kind)
}

const prose = computed(() => [
  { heading: t('scholarships.benefits'), body: props.s.benefits ?? '' },
  { heading: t('scholarships.requirements'), body: props.s.requirements ?? '' },
  { heading: t('scholarships.policy'), body: props.s.policy ?? '' },
].filter(p => p.body))

const eligibilityRows = computed(() => buildEligibilityRows(props.s.eligibility, t))
</script>

<template>
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
            <td class="px-4 py-3 text-right font-semibold tabular-nums text-red-600">{{ dualMoney(f.amountRmb, f.amountUsd) }}</td>
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
    <span v-if="s.requiresFinancialProof" class="me-3">• {{ t('scholarships.requiresFinancialProof') }}</span>
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
              {{ dualMoney(a.amountRmb, a.amountUsd) ?? t('scholarships.free') }}
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
</template>
