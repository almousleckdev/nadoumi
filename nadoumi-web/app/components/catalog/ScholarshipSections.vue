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

const coverageItems = computed(() => props.s.coverage.map(c => ({
  text: t(`scholarships.coverageKind.${c.kind}`, c.kind),
  detail: c.detail,
})))

const conditions = computed(() => [
  props.s.requiresFinancialProof ? { text: t('scholarships.requiresFinancialProof') } : null,
  props.s.requiresFoundationYear ? { text: t('scholarships.requiresFoundationYear') } : null,
].filter((c): c is { text: string } => c !== null))
</script>

<template>
  <DetailCard v-for="p in prose" :key="p.heading" :title="p.heading">
    <p class="whitespace-pre-line leading-7 text-slate-700">{{ p.body }}</p>
  </DetailCard>

  <DetailCard v-if="eligibilityRows.length || s.eligibility?.notes || conditions.length" :title="t('scholarships.eligibility')">
    <dl v-if="eligibilityRows.length" class="grid grid-cols-1 gap-x-8 gap-y-4 min-[480px]:grid-cols-2">
      <div v-for="r in eligibilityRows" :key="r.label" class="min-w-0">
        <dt class="text-xs font-medium uppercase tracking-wide text-slate-500">{{ r.label }}</dt>
        <dd class="mt-0.5 break-words font-medium text-slate-900">{{ r.value }}</dd>
      </div>
    </dl>
    <BulletList v-if="conditions.length" :items="conditions" :class="eligibilityRows.length ? 'mt-5' : ''" />
    <p v-if="s.eligibility?.notes" class="mt-4 text-sm text-slate-600">{{ s.eligibility.notes }}</p>
  </DetailCard>

  <DetailCard v-if="feeRows.length" :title="t('scholarships.fees')">
    <PriceRows :rows="feeRows" />
  </DetailCard>

  <DetailCard v-if="coverageItems.length" :title="t('scholarships.coverage')">
    <BulletList :items="coverageItems" :columns="2" />
  </DetailCard>

  <DetailCard v-if="roomRows.length" :title="t('scholarships.accommodation')">
    <PriceRows :rows="roomRows" />
  </DetailCard>

  <DetailCard v-if="s.renewalConditions" :title="t('scholarships.renewal')">
    <p class="whitespace-pre-line leading-7 text-slate-700">{{ s.renewalConditions }}</p>
  </DetailCard>

  <DetailCard v-if="s.documentRequirements.length" :title="t('scholarships.documents')" :description="t('scholarships.documentsNote')">
    <ul class="divide-y divide-slate-100">
      <li
        v-for="d in s.documentRequirements"
        :key="d.docType"
        class="flex flex-col gap-1 py-3 first:pt-0 last:pb-0 sm:flex-row sm:items-start sm:justify-between sm:gap-6"
      >
        <span class="min-w-0">
          <span class="font-medium text-slate-900">{{ t(`scholarships.doc.${d.docType}`, d.docType) }}</span>
          <span v-if="d.note" class="block text-sm text-slate-500">{{ d.note }}</span>
        </span>
        <span class="shrink-0 text-sm" :class="d.mandatory ? 'font-semibold text-slate-900' : 'text-slate-500'">
          {{ d.mandatory ? t('scholarships.required') : t('scholarships.optional') }}
        </span>
      </li>
    </ul>
  </DetailCard>
</template>
