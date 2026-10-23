<script setup lang="ts">
import type { ApplicantDto } from '~/types/catalog'

/** Adds a new student to apply for: just the name, because the rest is collected step by step in the application. */
const emit = defineEmits<{ created: [applicant: ApplicantDto], cancel: [] }>()
const { t } = useI18n()
const { createForOther } = useApplicant()
const { busy, error, run } = useAsyncAction()

const givenName = ref('')
const familyName = ref('')
const missing = ref(false)

async function submit() {
  missing.value = !givenName.value.trim() || !familyName.value.trim()
  if (missing.value) return
  const created = await run(() => createForOther({ givenName: givenName.value.trim(), familyName: familyName.value.trim() }))
  if (created) emit('created', created)
}
</script>

<template>
  <form class="grid gap-4 rounded-xl border border-slate-200 p-4" novalidate data-test="new-student" @submit.prevent="submit">
    <p class="text-sm text-slate-600">{{ t('applyFlow.start.newStudentHint') }}</p>
    <NAlert v-if="error" tone="danger">{{ error }}</NAlert>
    <div class="grid gap-4 sm:grid-cols-2">
      <FormTextField id="new-given" v-model="givenName" :label="t('applyFlow.start.givenName')" :error="missing && !givenName.trim() ? t('validation.required') : ''" required uppercase :maxlength="100" autocomplete="off" />
      <FormTextField id="new-family" v-model="familyName" :label="t('applyFlow.start.familyName')" :error="missing && !familyName.trim() ? t('validation.required') : ''" required uppercase :maxlength="100" autocomplete="off" />
    </div>
    <div class="flex gap-2">
      <NButton :loading="busy" data-test="new-student-save" @click="submit">{{ t('applyFlow.start.addStudent') }}</NButton>
      <NButton variant="ghost" :disabled="busy" @click="emit('cancel')">{{ t('common.cancel') }}</NButton>
    </div>
  </form>
</template>
