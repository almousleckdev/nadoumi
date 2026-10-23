<script setup lang="ts">
const props = defineProps<{ applicantId: number }>()
const emit = defineEmits<{ changed: [] }>()
const api = useApplicantFamily()

const section = useApplicantRecord(
  () => props.applicantId, { get: api.getPersonal, save: api.savePersonal }, () => emit('changed'),
)
await section.load()
</script>

<template>
  <div class="grid gap-4">
    <ApplicantSectionAlerts :error="section.error.value" :notice="section.notice.value" />
    <PersonalDetailsForm :record="section.record.value" :busy="section.busy.value" @submit="section.save" />
  </div>
</template>
