<script setup lang="ts">
import type { WorkBody } from '~/composables/useApplicant'
import type { WorkDto } from '~/types/catalog'
import { formatPeriod } from '~/utils/dates'

const props = defineProps<{ applicantId: number }>()
const emit = defineEmits<{ changed: [] }>()
const { t } = useI18n()
const { countryLabel } = useLocaleOptions()
const { label: typeLabel } = useEnumOptions('employmentType')
const api = useApplicant()

const records = useApplicantRecords(() => props.applicantId, {
  list: api.listWork, add: api.addWork, update: api.updateWork, remove: api.deleteWork,
}, () => emit('changed'))
await records.load()

const details = (w: WorkDto) =>
  [w.employmentType && typeLabel(w.employmentType), w.city, countryLabel(w.country)].filter(Boolean).join(' · ')
</script>

<template>
  <div class="grid gap-4">
    <ApplicantSectionAlerts :error="records.error.value" />
    <RecordList :items="records.items.value" :empty="t('work.empty')" :add-label="t('work.add')" @remove="records.remove">
      <template #summary="{ item }">
        <p class="font-medium text-slate-900">{{ item.jobTitle }} · {{ item.employer }}</p>
        <p class="text-sm text-slate-600">{{ details(item) }}</p>
        <p class="text-xs text-slate-400">{{ formatPeriod(item.startDate, item.endDate, item.current, t('common.present')) }}</p>
      </template>
      <template #form="{ item, close }">
        <WorkForm :record="(item as WorkDto | null)" :busy="records.busy.value" @cancel="close" @submit="async (body: WorkBody) => { if (await records.save((item as WorkDto | null)?.id ?? null, body)) close() }" />
      </template>
    </RecordList>
  </div>
</template>
