<script setup lang="ts">
import type { ContactBody } from '~/composables/useApplicant'
import type { ContactDto } from '~/types/catalog'

const props = defineProps<{ applicantId: number }>()
const emit = defineEmits<{ changed: [] }>()
const { t } = useI18n()
const { label: relationLabel } = useEnumOptions('contactRelation')
const api = useApplicant()

const records = useApplicantRecords(() => props.applicantId, {
  list: api.listContacts, add: api.addContact, update: api.updateContact, remove: api.deleteContact,
}, () => emit('changed'))
await records.load()
</script>

<template>
  <div class="grid gap-4">
    <ApplicantSectionAlerts :error="records.error.value" />
    <RecordList :items="records.items.value" :empty="t('contact.empty')" :add-label="t('contact.add')" @remove="records.remove">
      <template #summary="{ item }">
        <p class="font-medium text-slate-900">{{ item.name }}</p>
        <p class="text-sm text-slate-600">{{ [relationLabel(item.relation), item.phone, item.email].filter(Boolean).join(' · ') }}</p>
      </template>
      <template #form="{ item, close }">
        <ContactForm :record="(item as ContactDto | null)" :busy="records.busy.value" @cancel="close" @submit="async (body: ContactBody) => { if (await records.save((item as ContactDto | null)?.id ?? null, body)) close() }" />
      </template>
    </RecordList>
  </div>
</template>
