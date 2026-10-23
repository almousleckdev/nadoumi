<script setup lang="ts">
/** The documents an opportunity asks applicants for, shared by the scholarship and programme pages. */
export interface RequiredDocument {
  docType: string
  mandatory: boolean
  note?: string | null
  appliesWhen?: string | null
}

defineProps<{ documents: RequiredDocument[] }>()
const { t } = useI18n()
</script>

<template>
  <DetailCard v-if="documents.length" :title="t('scholarships.documents')" :description="t('scholarships.documentsNote')">
    <ul class="divide-y divide-slate-100">
      <li
        v-for="d in documents"
        :key="d.docType"
        class="flex flex-col gap-1 py-3 first:pt-0 last:pb-0 sm:flex-row sm:items-start sm:justify-between sm:gap-6"
      >
        <span class="min-w-0">
          <span class="font-medium text-slate-900">{{ t(`scholarships.doc.${d.docType}`, d.docType) }}</span>
          <span v-if="d.appliesWhen" class="block text-sm text-slate-500">{{ t(`scholarships.cond.${d.appliesWhen}`) }}</span>
          <span v-if="d.note" class="block text-sm text-slate-500">{{ d.note }}</span>
        </span>
        <span class="shrink-0 text-sm" :class="d.mandatory ? 'font-semibold text-slate-900' : 'text-slate-500'">
          {{ d.mandatory ? t('scholarships.required') : t('scholarships.optional') }}
        </span>
      </li>
    </ul>
  </DetailCard>
</template>
