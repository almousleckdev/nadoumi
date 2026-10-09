<script setup lang="ts">
import type { ReviewRow } from '~/components/onboarding/ReviewSection.vue'
import { ONBOARDING_STEPS, STEP_SECTIONS, type OnboardingStep } from '~/constants/onboarding'
import type { SignedFileUrl } from '~/composables/useApplicant'
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
const scholarship = useEnumOptions('scholarshipInterest').label
const teaching = useEnumOptions('teachingLanguage').label
const intakeTerm = useEnumOptions('intakeTerm').label

const id = props.applicant.id
const [passport, education, interests, residence, contacts, work, photo] = await Promise.all([
  api.passportStatus(id).catch((): PassportStatusDto | null => null),
  api.listEducation(id).catch((): EducationDto[] => []),
  api.getInterests(id).catch((): InterestDto | null => null),
  api.getResidence(id).catch((): ResidenceDto | null => null),
  api.listContacts(id).catch((): ContactDto[] => []),
  api.listWork(id).catch((): WorkDto[] => []),
  api.photoUrl(id).catch((): SignedFileUrl | null => null),
])
// the scan is only requested when one is on file; a missing file is a normal state, not an error
const passportScan = passport?.scanUploaded ? await api.passportScanUrl(id).catch((): SignedFileUrl | null => null) : null

const row = (label: string, value: string | null | undefined): ReviewRow[] => (value ? [{ label, value }] : [])
const present = t('common.present')

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
          ...row(t('passport.fields.passportNo'), passport.passportNo),
          ...row(t('passport.fields.givenName'), passport.givenName),
          ...row(t('passport.fields.familyName'), passport.familyName),
          ...row(t('passport.fields.dob'), passport.dob),
          ...row(t('passport.fields.issueDate'), passport.issueDate),
          ...row(t('passport.fields.expiryDate'), passport.expiryDate),
        ]
      : [],
    education: education.map(e => ({
      label: e.institution,
      value: [
        educationLevel(e.level), e.qualification, e.field, [e.city, countryLabel(e.country)].filter(Boolean).join(', '),
        e.gpa != null ? `${e.gpa}${e.gpaScale ? ` / ${e.gpaScale}` : ''}` : '',
        formatPeriod(e.startDate, e.endDate, e.current, present),
      ].filter(Boolean).join(' · '),
    })),
    interests: interests
      ? [
          ...row(t('interests.level'), studyLevel(interests.desiredLevel)),
          ...row(t('interests.scholarship'), interests.scholarshipInterest && scholarship(interests.scholarshipInterest)),
          ...row(t('interests.fields'), interests.fields.map(field).join(', ')),
          ...row(t('interests.cities'), interests.cities.map(city).join(', ')),
          ...row(t('interests.teachingLanguage'), interests.teachingLanguage && teaching(interests.teachingLanguage)),
          ...row(t('interests.intakeYear'), interests.intakeYear ? String(interests.intakeYear) : null),
          ...row(t('interests.intakeTerm'), interests.intakeTerm && intakeTerm(interests.intakeTerm)),
          ...row(t('interests.notes'), interests.notes),
        ]
      : [],
    location: residence
      ? [
          ...row(t('location.country'), countryLabel(residence.country)),
          ...row(t('location.city'), residence.city),
          ...row(t('location.otherInfo'), residence.address),
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
      value: [w.employmentType && employment(w.employmentType), [w.city, countryLabel(w.country)].filter(Boolean).join(', '), formatPeriod(w.startDate, w.endDate, w.current, present), w.description].filter(Boolean).join(' · '),
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
    >
      <template v-if="step === 'identity'" #media>
        <div class="grid gap-3 sm:grid-cols-[10rem_minmax(0,1fr)]" data-test="review-pictures">
          <figure class="grid gap-1.5">
            <figcaption class="text-xs font-medium uppercase tracking-wide text-slate-500">{{ t('review.photo') }}</figcaption>
            <DocumentPreview v-if="photo?.url" :src="photo.url" type="image/jpeg" :name="t('review.photo')" compact data-test="review-photo" />
            <p v-else class="grid h-44 place-items-center rounded-lg border border-dashed border-slate-300 bg-slate-50 text-sm text-slate-500" data-test="review-photo-missing">{{ t('review.notUploaded') }}</p>
          </figure>
          <figure class="grid gap-1.5">
            <figcaption class="text-xs font-medium uppercase tracking-wide text-slate-500">{{ t('review.passportScan') }}</figcaption>
            <DocumentPreview v-if="passportScan?.url" :src="passportScan.url" :name="t('review.passportScan')" compact data-test="review-passport" />
            <p v-else class="grid h-44 place-items-center rounded-lg border border-dashed border-slate-300 bg-slate-50 text-sm text-slate-500" data-test="review-passport-missing">{{ t('review.notUploaded') }}</p>
          </figure>
        </div>
      </template>
    </ReviewSection>
  </div>
</template>
