<script setup lang="ts">
import type { EducationBody } from '~/composables/useApplicant'
import type { EducationDto } from '~/types/catalog'
import { formatPeriod } from '~/utils/dates'

const props = defineProps<{ applicantId: number }>()
const emit = defineEmits<{ changed: [] }>()
const { t } = useI18n()
const { countryLabel } = useLocaleOptions()
const { label: levelLabel } = useEnumOptions('educationLevel')
const api = useApplicant()

const records = useApplicantRecords(() => props.applicantId, {
  list: api.listEducation, add: api.addEducation, update: api.updateEducation, remove: api.deleteEducation,
}, () => emit('changed'))
await records.load()

const details = (e: EducationDto) => [levelLabel(e.level), e.field, countryLabel(e.country)].filter(Boolean).join(' · ')
</script>

<template>
  <div class="grid gap-4">
    <ApplicantSectionAlerts :error="records.error.value" />
    <RecordList :items="records.items.value" :empty="t('education.empty')" :add-label="t('education.add')" @remove="records.remove">
      <template #summary="{ item }">
        <p class="font-medium text-slate-900">{{ item.institution }}</p>
        <p class="text-sm text-slate-600">{{ details(item) }}</p>
        <p class="text-xs text-slate-400">{{ formatPeriod(item.startDate, item.endDate, item.current, t('common.present')) }}</p>
      </template>
      <template #form="{ item, close }">
        <EducationForm :record="(item as EducationDto | null)" :busy="records.busy.value" @cancel="close" @submit="async (body: EducationBody) => { if (await records.save((item as EducationDto | null)?.id ?? null, body)) close() }" />
      </template>
    </RecordList>
  </div>
</template>
