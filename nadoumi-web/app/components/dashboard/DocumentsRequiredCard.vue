<script setup lang="ts">
/**
 * A case sent back for documents. The note is what Nadoumi wrote for the student; the checklist below lets them fix the
 * documents, and this button returns the SAME application to review. Nothing is created or paid again.
 */
const props = defineProps<{ applicationId: number, note: string | null }>()
const emit = defineEmits<{ resubmitted: [] }>()

const { t } = useI18n()
const { resubmit } = useApplications()
const { busy, error, run } = useAsyncAction()

async function sendBack() {
  const done = await run(() => resubmit(props.applicationId), t('dashboard.applications.documentsRequired.sent'))
  if (done) emit('resubmitted')
}
</script>

<template>
  <SectionCard :title="t('dashboard.applications.documentsRequired.title')">
    <div class="grid gap-3" data-test="documents-required">
      <p class="text-sm text-slate-700">{{ t('dashboard.applications.documentsRequired.body') }}</p>
      <p v-if="note" class="whitespace-pre-wrap rounded-md border border-slate-200 bg-slate-50 p-3 text-sm text-slate-900" data-test="action-note">{{ note }}</p>
      <p class="text-sm text-slate-600">{{ t('dashboard.applications.documentsRequired.howTo') }}</p>
      <NAlert v-if="error" tone="danger">{{ error }}</NAlert>
      <div>
        <NButton :loading="busy" data-test="resubmit" @click="sendBack">
          {{ t('dashboard.applications.documentsRequired.sendBack') }}
        </NButton>
      </div>
    </div>
  </SectionCard>
</template>
