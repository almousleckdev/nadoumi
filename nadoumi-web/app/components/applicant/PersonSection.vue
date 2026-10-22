<script setup lang="ts">
import type { PersonBody } from '~/composables/useApplicantFamily'
import type { PersonDto, PersonRole } from '~/types/family'

/** The financial supporter or the emergency contact in China: the same details, one of each per student. */
const props = defineProps<{ applicantId: number, role: PersonRole }>()
const emit = defineEmits<{ changed: [] }>()
const api = useApplicantFamily()

const section = useApplicantRecord<PersonDto, PersonBody>(
  () => props.applicantId,
  { get: id => api.getPerson(id, props.role), save: (id, body) => api.savePerson(id, props.role, body) },
  () => emit('changed'),
)
await section.load()
</script>

<template>
  <div class="grid gap-4">
    <ApplicantSectionAlerts :error="section.error.value" :notice="section.notice.value" />
    <PersonForm :record="section.record.value" :busy="section.busy.value" :id-prefix="role" @submit="section.save" />
  </div>
</template>
