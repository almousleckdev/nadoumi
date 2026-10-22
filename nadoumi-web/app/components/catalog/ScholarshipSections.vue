<script setup lang="ts">
import type { ScholarshipDetail } from '~/types/catalog'
import { dualMoney, eligibilityRows as buildEligibilityRows } from '~/utils/scholarshipDisplay'

const props = defineProps<{ s: ScholarshipDetail }>()

const { t } = useI18n()

const prose = computed(() => [
  { heading: t('scholarships.benefits'), body: props.s.benefits ?? '' },
  { heading: t('scholarships.requirements'), body: props.s.requirements ?? '' },
  { heading: t('scholarships.policy'), body: props.s.policy ?? '' },
].filter(p => p.body))

const eligibilityRows = computed(() => buildEligibilityRows(props.s.eligibility, t))

const feeRows = computed(() => props.s.fees.map(f => ({
  label: t(`scholarships.fee.${f.kind}`, f.kind),
  note: f.note,
  price: dualMoney(f.amountRmb, f.amountUsd) ?? '',
})))

const roomRows = computed(() => props.s.accommodation.map(a => ({
  label: t(`scholarships.room.${a.roomType}`, a.roomType),
  note: a.note,
  price: dualMoney(a.amountRmb, a.amountUsd) ?? t('scholarships.free'),
})))
</script>

<template>
  <div class="min-w-0 space-y-6">
    <DetailCard v-for="p in prose" :key="p.heading" :title="p.heading">
      <p class="whitespace-pre-line leading-7 text-slate-700">{{ p.body }}</p>
    </DetailCard>

    <DetailCard v-if="eligibilityRows.length || s.eligibility?.notes" :title="t('scholarships.eligibility')">
      <dl v-if="eligibilityRows.length" class="grid grid-cols-1 gap-3 min-[420px]:grid-cols-2 lg:grid-cols-3">
        <div v-for="r in eligibilityRows" :key="r.label" class="rounded-xl bg-slate-50 p-3">
          <dt class="text-xs font-medium uppercase tracking-wide text-slate-400">{{ r.label }}</dt>
          <dd class="mt-0.5 break-words font-medium text-slate-900">{{ r.value }}</dd>
        </div>
      </dl>
      <p v-if="s.eligibility?.notes" class="text-sm text-slate-600" :class="eligibilityRows.length ? 'mt-4' : ''">{{ s.eligibility.notes }}</p>
    </DetailCard>

    <DetailCard v-if="feeRows.length" :title="t('scholarships.fees')">
      <PriceRows :rows="feeRows" />
    </DetailCard>

    <DetailCard v-if="s.coverage.length" :title="t('scholarships.coverage')">
      <ul class="grid gap-3 sm:grid-cols-2">
        <li v-for="(c, i) in s.coverage" :key="i" class="flex items-start gap-2.5 text-slate-700">
          <span class="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-brand-500" aria-hidden="true" />
          <span>
            {{ t(`scholarships.coverageKind.${c.kind}`, c.kind) }}
            <span v-if="c.detail" class="block text-sm text-slate-500">{{ c.detail }}</span>
          </span>
        </li>
      </ul>
    </DetailCard>

    <DetailCard v-if="roomRows.length" :title="t('scholarships.accommodation')">
      <PriceRows :rows="roomRows" />
    </DetailCard>

    <DetailCard v-if="s.renewalConditions" :title="t('scholarships.renewal')">
      <p class="whitespace-pre-line leading-7 text-slate-700">{{ s.renewalConditions }}</p>
    </DetailCard>

    <ul v-if="s.requiresFinancialProof || s.requiresFoundationYear" class="flex flex-wrap gap-2 text-sm">
      <li v-if="s.requiresFinancialProof" class="rounded-full bg-amber-50 px-3 py-1 font-medium text-amber-800 ring-1 ring-amber-200">{{ t('scholarships.requiresFinancialProof') }}</li>
      <li v-if="s.requiresFoundationYear" class="rounded-full bg-amber-50 px-3 py-1 font-medium text-amber-800 ring-1 ring-amber-200">{{ t('scholarships.requiresFoundationYear') }}</li>
    </ul>

    <DetailCard v-if="s.documentRequirements.length" :title="t('scholarships.documents')" :description="t('scholarships.documentsNote')">
      <ul class="divide-y divide-slate-100">
        <li
          v-for="d in s.documentRequirements"
          :key="d.docType"
          class="flex flex-col gap-1.5 py-3 first:pt-0 last:pb-0 sm:flex-row sm:items-start sm:justify-between sm:gap-6"
        >
          <span class="min-w-0">
            <span class="font-medium text-slate-900">{{ t(`scholarships.doc.${d.docType}`, d.docType) }}</span>
            <span v-if="d.note" class="block text-sm text-slate-500">{{ d.note }}</span>
          </span>
          <span
            class="inline-flex w-fit shrink-0 rounded-full px-2.5 py-0.5 text-xs font-semibold"
            :class="d.mandatory ? 'bg-red-100 text-red-700' : 'bg-slate-100 text-slate-600'"
          >
            {{ d.mandatory ? t('scholarships.required') : t('scholarships.optional') }}
          </span>
        </li>
      </ul>
    </DetailCard>
  </div>
</template>
