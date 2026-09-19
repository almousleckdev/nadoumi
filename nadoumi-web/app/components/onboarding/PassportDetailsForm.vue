<script setup lang="ts">
import { PASSPORT_ERROR_KEYS } from '~/constants/passport'
import { validatePassport, type PassportForm } from '~/utils/passportRules'

/** The passport's holder data, as read from the passport or typed off it, for the student to confirm. */
const props = defineProps<{ initial: PassportForm | null, busy: boolean, canSave: boolean }>()
const emit = defineEmits<{ submit: [form: PassportForm] }>()
const { t } = useI18n()

const EMPTY: PassportForm = { passportNo: '', givenName: '', familyName: '', dob: '', issueDate: '', expiryDate: '' }
const form = reactive<PassportForm>({ ...EMPTY })
watch(() => props.initial, (value: PassportForm | null) => Object.assign(form, value ?? EMPTY), { immediate: true })

const submitted = ref(false)
const errors = computed(() => {
  if (!submitted.value) return {}
  return Object.fromEntries(Object.entries(validatePassport(form))
    .map(([field, code]) => [field, t(PASSPORT_ERROR_KEYS[code])]))
})

function submit() {
  submitted.value = true
  if (Object.keys(errors.value).length === 0) emit('submit', { ...form })
}
</script>

<template>
  <form class="grid gap-4" novalidate @submit.prevent="submit">
    <FormSection :title="t('passport.detailsTitle')">
      <FormTextField id="passportNo" v-model="form.passportNo" :label="t('passport.fields.passportNo')" :error="errors.passportNo" :maxlength="20" uppercase required />
      <div class="hidden sm:block" aria-hidden="true" />
      <FormTextField id="passportGivenName" v-model="form.givenName" :label="t('passport.fields.givenName')" :error="errors.givenName" :maxlength="100" uppercase required />
      <FormTextField id="passportFamilyName" v-model="form.familyName" :label="t('passport.fields.familyName')" :error="errors.familyName" :maxlength="100" uppercase required />
      <FormTextField id="passportDob" v-model="form.dob" type="date" :label="t('passport.fields.dob')" :error="errors.dob" required />
      <div class="hidden sm:block" aria-hidden="true" />
      <FormTextField id="passportIssueDate" v-model="form.issueDate" type="date" :label="t('passport.fields.issueDate')" :error="errors.issueDate" required />
      <FormTextField id="passportExpiryDate" v-model="form.expiryDate" type="date" :label="t('passport.fields.expiryDate')" :hint="t('passport.expiryHint')" :error="errors.expiryDate" required />
    </FormSection>
    <div>
      <NButton type="submit" :loading="busy" :disabled="!canSave">{{ t('passport.save') }}</NButton>
    </div>
  </form>
</template>
