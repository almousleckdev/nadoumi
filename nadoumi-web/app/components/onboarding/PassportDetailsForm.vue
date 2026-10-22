<script setup lang="ts">
import { validatePassport, type PassportForm } from '~/utils/passportRules'

/**
 * The passport's number and validity dates. The holder's name and date of birth are not asked again:
 * the profile entered on the first step is the identity.
 */
const props = defineProps<{ initial: PassportForm | null, busy: boolean, canSave: boolean }>()
const emit = defineEmits<{ submit: [form: PassportForm] }>()
const { t } = useI18n()

const EMPTY: PassportForm = { passportNo: '', issueDate: '', expiryDate: '' }
const form = reactive<PassportForm>({ ...EMPTY })
watch(() => props.initial, (value: PassportForm | null) => Object.assign(form, value ?? EMPTY), { immediate: true })

const { errors, submit: validateAndSubmit } = useValidatedForm(() => validatePassport(form))

function submit() {
  validateAndSubmit(() => emit('submit', { ...form }))
}
</script>

<template>
  <form class="grid gap-4" novalidate @submit.prevent="submit">
    <FormSection :title="t('passport.detailsTitle')">
      <FormTextField id="passportNo" v-model="form.passportNo" :label="t('passport.fields.passportNo')" :error="errors.passportNo" :maxlength="20" uppercase required />
      <div class="hidden sm:block" aria-hidden="true" />
      <FormTextField id="passportIssueDate" v-model="form.issueDate" type="date" :label="t('passport.fields.issueDate')" :error="errors.issueDate" required />
      <FormTextField id="passportExpiryDate" v-model="form.expiryDate" type="date" :label="t('passport.fields.expiryDate')" :hint="t('passport.expiryHint')" :error="errors.expiryDate" required />
    </FormSection>
    <div>
      <NButton type="submit" :loading="busy" :disabled="!canSave">{{ t('passport.save') }}</NButton>
    </div>
  </form>
</template>
