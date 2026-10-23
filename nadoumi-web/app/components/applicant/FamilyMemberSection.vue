<script setup lang="ts">
import type { FamilyMemberBody } from '~/composables/useApplicantFamily'
import type { FamilyMemberDto } from '~/types/family'

const props = defineProps<{ applicantId: number }>()
const emit = defineEmits<{ changed: [] }>()
const { t } = useI18n()
const { label: relationLabel } = useEnumOptions('familyRelation')
const api = useApplicantFamily()

const records = useApplicantRecords(() => props.applicantId, {
  list: api.listFamily, add: api.addFamily, update: api.updateFamily, remove: api.deleteFamily,
}, () => emit('changed'))
await records.load()
</script>

<template>
  <div class="grid gap-4">
    <p class="text-sm text-slate-500">{{ t('family.member.rule') }}</p>
    <ApplicantSectionAlerts :error="records.error.value" />
    <RecordList :items="records.items.value" :empty="t('family.member.empty')" :add-label="t('family.member.add')" @remove="records.remove">
      <template #summary="{ item }">
        <p class="font-medium text-slate-900">{{ (item as FamilyMemberDto).fullName }}</p>
        <p class="text-sm text-slate-600">
          {{ [relationLabel((item as FamilyMemberDto).relation), (item as FamilyMemberDto).deceased ? t('family.member.deceasedTag') : (item as FamilyMemberDto).phone].filter(Boolean).join(' · ') }}
        </p>
      </template>
      <template #form="{ item, close }">
        <FamilyMemberForm :record="(item as FamilyMemberDto | null)" :busy="records.busy.value" @cancel="close" @submit="async (body: FamilyMemberBody) => { if (await records.save((item as FamilyMemberDto | null)?.id ?? null, body)) close() }" />
      </template>
    </RecordList>
  </div>
</template>
