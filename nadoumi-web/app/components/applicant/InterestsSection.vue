<script setup lang="ts">
const props = defineProps<{ applicantId: number }>()
const emit = defineEmits<{ changed: [] }>()
const api = useApplicant()

const section = useApplicantRecord(
  () => props.applicantId, { get: api.getInterests, save: api.saveInterests }, () => emit('changed'),
)
await section.load()
</script>

<template>
  <div class="grid gap-4">
    <ApplicantSectionAlerts :error="section.error.value" :notice="section.notice.value" />
    <InterestsForm :record="section.record.value" :busy="section.busy.value" @submit="section.save" />
  </div>
</template>
