<script setup lang="ts">
const props = defineProps<{ applicantId: number }>()
const emit = defineEmits<{ changed: [] }>()
const api = useApplicant()

const section = useApplicantRecord(
  () => props.applicantId, { get: api.getResidence, save: api.saveResidence }, () => emit('changed'),
)
await section.load()
</script>

<template>
  <div class="grid gap-4">
    <ApplicantSectionAlerts :error="section.error.value" :notice="section.notice.value" />
    <ResidenceForm :record="section.record.value" :busy="section.busy.value" @submit="section.save" />
  </div>
</template>
