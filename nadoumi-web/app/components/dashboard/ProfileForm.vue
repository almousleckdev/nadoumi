<script setup lang="ts">
import type { ApplicantDto } from '~/types/catalog'
import type { SelfApplicantBody } from '~/composables/useApplicant'
import { GENDERS } from '~/constants/profile'
import { latestEligibleDob, validateProfile } from '~/utils/profileRules'

const props = withDefaults(
  defineProps<{ modelValue: ApplicantDto | null, busy: boolean, submitLabel?: string }>(),
  { submitLabel: undefined },
)
const emit = defineEmits<{ submit: [body: SelfApplicantBody], 'email-verified': [applicant: ApplicantDto] }>()
const { t } = useI18n()
const { countries, languages } = useLocaleOptions()

const form = reactive<Required<SelfApplicantBody>>({
  givenName: '', familyName: '', dob: '', nationality: '', passportNo: '', email: '', phone: '',
  gender: '', countryOfOrigin: '', countryOfResidence: '', nativeLanguage: '', wechatId: '', whatsapp: '',
})
watchEffect(() => {
  const a = props.modelValue
  if (!a) return
  for (const key of Object.keys(form) as (keyof typeof form)[]) form[key] = a[key as keyof ApplicantDto] as string ?? ''
})

const genders = computed(() => GENDERS.map(value => ({ value, label: t(`profileForm.genderOption.${value}`) })))
const dobMax = latestEligibleDob()

// A different email must be proven before it can be saved; an existing one that was
// never verified (a legacy account) needs the same step.
const savedEmail = computed(() => props.modelValue?.email ?? '')
const emailChanged = computed(() =>
  Boolean(props.modelValue) && form.email.trim().toLowerCase() !== savedEmail.value.toLowerCase())
const emailNeedsVerification = computed(() =>
  Boolean(props.modelValue) && (emailChanged.value || !props.modelValue?.emailVerified))
const emailShownVerified = computed(() => Boolean(props.modelValue?.emailVerified) && !emailChanged.value)

const { errors, submit: validateAndSubmit } = useValidatedForm(() =>
  validateProfile(form, { emailNeedsVerification: emailNeedsVerification.value }))

function submit() {
  // an unchanged email is sent as-is; a new one only ever arrives through verification
  validateAndSubmit(() => emit('submit', { ...form, email: props.modelValue ? savedEmail.value : form.email }))
}

function onVerified(applicant: ApplicantDto) {
  form.email = applicant.email ?? form.email
  emit('email-verified', applicant)
}
</script>

<template>
  <form class="grid gap-8" novalidate @submit.prevent="submit">
    <FormSection :title="t('profileForm.sectionPerson')">
      <NAlert tone="info" class="sm:col-span-2">{{ t('profileForm.nameNotice') }}</NAlert>
      <FormTextField id="givenName" v-model="form.givenName" :label="t('dashboard.givenName')" :error="errors.givenName" autocomplete="given-name" :maxlength="100" uppercase required />
      <FormTextField id="familyName" v-model="form.familyName" :label="t('dashboard.familyName')" :error="errors.familyName" autocomplete="family-name" :maxlength="100" uppercase required />
      <FormTextField id="dob" v-model="form.dob" type="date" :label="t('dashboard.dob')" :hint="t('profileForm.dobHint')" :error="errors.dob" :max="dobMax" autocomplete="bday" required />
      <FormSelectField id="gender" v-model="form.gender" :label="t('profileForm.gender')" :options="genders" :error="errors.gender" required />
    </FormSection>

    <FormSection :title="t('profileForm.sectionCountries')">
      <FormComboboxField id="nationality" v-model="form.nationality" :label="t('dashboard.nationality')" :options="countries" :error="errors.nationality" required />
      <FormComboboxField id="countryOfOrigin" v-model="form.countryOfOrigin" :label="t('profileForm.countryOfOrigin')" :options="countries" :error="errors.countryOfOrigin" required />
      <FormComboboxField id="countryOfResidence" v-model="form.countryOfResidence" :label="t('profileForm.countryOfResidence')" :options="countries" :error="errors.countryOfResidence" required />
      <FormComboboxField id="nativeLanguage" v-model="form.nativeLanguage" :label="t('profileForm.nativeLanguage')" :options="languages" :error="errors.nativeLanguage" required />
      <FormTextField id="passportNo" v-model="form.passportNo" :label="t('dashboard.passportNo')" :maxlength="64" uppercase />
    </FormSection>

    <FormSection :title="t('profileForm.sectionContact')">
      <div class="grid gap-3 sm:col-span-2">
        <FormTextField id="pemail" v-model="form.email" type="email" :label="t('auth.email')" :error="emailNeedsVerification ? undefined : errors.email" autocomplete="email" :maxlength="120" required>
          <template v-if="emailShownVerified" #suffix>
            <span class="text-xs font-medium text-emerald-700">✓ {{ t('profileForm.emailVerified') }}</span>
          </template>
        </FormTextField>
        <ApplicantEmailVerify
          v-if="modelValue && emailNeedsVerification && form.email.trim()"
          :applicant-id="modelValue.id"
          :email="form.email.trim()"
          @verified="onVerified"
        />
        <button
          v-if="emailChanged"
          type="button"
          class="justify-self-start text-sm font-medium text-brand-700 hover:underline"
          @click="form.email = savedEmail"
        >
          {{ t('profileForm.keepEmail', { email: savedEmail }) }}
        </button>
      </div>
      <FormPhoneField id="phone" v-model="form.phone" :label="t('profileForm.contactNumber')" :error="errors.phone" required />
      <div class="hidden sm:block" aria-hidden="true" />
      <FormTextField id="wechatId" v-model="form.wechatId" :label="t('profileForm.wechatId')" :error="errors.contactHandle" :maxlength="64" />
      <FormTextField id="whatsapp" v-model="form.whatsapp" type="tel" :label="t('profileForm.whatsapp')" :hint="t('profileForm.contactHandleHint')" :maxlength="32" />
    </FormSection>

    <div>
      <NButton type="submit" :loading="busy">{{ submitLabel ?? t('common.save') }}</NButton>
    </div>
  </form>
</template>
