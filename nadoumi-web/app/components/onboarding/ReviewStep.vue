<script setup lang="ts">
import type { ReviewRow } from '~/components/onboarding/ReviewSection.vue'
import { ONBOARDING_STEPS, STEP_SECTIONS, type OnboardingStep } from '~/constants/onboarding'
import type {
  ApplicantDto, ContactDto, EducationDto, InterestDto, OnboardingStatusDto, PassportStatusDto, ResidenceDto, WorkDto,
} from '~/types/catalog'
import { formatPeriod } from '~/utils/dates'

/** Every section the student entered, one card each, with a way back to any of them before Finish. */
const props = defineProps<{ applicant: ApplicantDto, status: OnboardingStatusDto | null }>()
defineEmits<{ edit: [step: OnboardingStep] }>()
const { t } = useI18n()
const { countryLabel, languageLabel } = useLocaleOptions()
const api = useApplicant()
const educationLevel = useEnumOptions('educationLevel').label
const studyLevel = useEnumOptions('studyLevel').label
const field = useEnumOptions('fieldOfStudy').label
const city = useEnumOptions('chinaCity').label
const visa = useEnumOptions('chinaVisaType').label
const employment = useEnumOptions('employmentType').label
const relation = useEnumOptions('contactRelation').label

const id = props.applicant.id
const [passport, education, interests, residence, contacts, work] = await Promise.all([
  api.passportStatus(id).catch((): PassportStatusDto | null => null),
  api.listEducation(id).catch((): EducationDto[] => []),
  api.getInterests(id).catch((): InterestDto | null => null),
  api.getResidence(id).catch((): ResidenceDto | null => null),
  api.listContacts(id).catch((): ContactDto[] => []),
  api.listWork(id).catch((): WorkDto[] => []),
])

const row = (label: string, value: string | null | undefined): ReviewRow[] => (value ? [{ label, value }] : [])
const present = t('common.present')
const yesNo = (value: boolean) => t(value ? 'common.yes' : 'common.no')

const rowsByStep = computed<Record<OnboardingStep, ReviewRow[]>>(() => {
  const a = props.applicant
  return {
    personal: [
      ...row(t('review.name'), `${a.givenName} ${a.familyName}`),
      ...row(t('dashboard.dob'), a.dob),
      ...row(t('profileForm.gender'), a.gender && t(`profileForm.genderOption.${a.gender}`)),
      ...row(t('dashboard.nationality'), countryLabel(a.nationality)),
      ...row(t('profileForm.countryOfOrigin'), countryLabel(a.countryOfOrigin)),
      ...row(t('profileForm.countryOfResidence'), countryLabel(a.countryOfResidence)),
      ...row(t('profileForm.nativeLanguage'), languageLabel(a.nativeLanguage)),
      ...row(t('auth.email'), a.email),
      ...row(t('profileForm.contactNumber'), a.phone),
      ...row(t('profileForm.wechatId'), a.wechatId),
      ...row(t('profileForm.whatsapp'), a.whatsapp),
    ],
    identity: passport?.passportNo
      ? [
          ...row(t('dashboard.passportNo'), passport.passportNo),
          ...row(t('passport.fields.issueDate'), passport.issueDate),
          ...row(t('passport.fields.expiryDate'), passport.expiryDate),
          ...row(t('review.photo'), yesNo(props.status?.sections.find(s => s.key === 'PHOTO')?.complete === true)),
        ]
      : [],
    education: education.map(e => ({
      label: e.institution,
      value: [educationLevel(e.level), e.field, formatPeriod(e.startDate, e.endDate, e.current, present)].filter(Boolean).join(' · '),
    })),
    interests: interests
      ? [
          ...row(t('interests.level'), studyLevel(interests.desiredLevel)),
          ...row(t('interests.fields'), interests.fields.map(field).join(', ')),
          ...row(t('interests.cities'), interests.cities.map(city).join(', ')),
        ]
      : [],
    location: residence
      ? [
          ...row(t('location.country'), countryLabel(residence.country)),
          ...row(t('location.city'), residence.city),
          ...(residence.inChina
            ? [
                ...row(t('location.currentLevel'), residence.chinaEducationLevel && educationLevel(residence.chinaEducationLevel)),
                ...row(t('location.visaType'), residence.visaType && visa(residence.visaType)),
                ...row(t('location.visaExpiry'), residence.visaExpiryDate),
              ]
            : []),
        ]
      : [],
    contact: contacts.map(c => ({ label: c.name, value: [relation(c.relation), c.phone, c.email].filter(Boolean).join(' · ') })),
    work: work.map(w => ({
      label: `${w.jobTitle} · ${w.employer}`,
      value: [w.employmentType && employment(w.employmentType), countryLabel(w.country), formatPeriod(w.startDate, w.endDate, w.current, present)].filter(Boolean).join(' · '),
    })),
    review: [],
  }
})

const reviewed = ONBOARDING_STEPS.filter(step => step !== 'review')
const isComplete = (step: OnboardingStep) =>
  STEP_SECTIONS[step].every(key => props.status?.sections.find(s => s.key === key)?.complete === true)
const isOptional = (step: OnboardingStep) => STEP_SECTIONS[step].length === 0
</script>

<template>
  <div class="grid gap-3">
    <ReviewSection
      v-for="step in reviewed"
      :key="step"
      :title="t(`onboarding.steps.${step}`)"
      :rows="rowsByStep[step]"
      :complete="isOptional(step) ? rowsByStep[step].length > 0 : isComplete(step)"
      :optional="isOptional(step)"
      :empty="t('onboarding.review.nothingEntered')"
      @edit="$emit('edit', step)"
    />
  </div>
</template>
