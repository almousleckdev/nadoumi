<script setup lang="ts">
import type { InterestBody } from '~/composables/useApplicant'
import {
  INTAKE_YEAR_SPAN, MAX_INTEREST_CITIES, MAX_INTEREST_FIELDS, MAX_INTEREST_FIELD_LENGTH, MIN_INTEREST_CITIES,
} from '~/constants/applicantOptions'
import type { InterestDto } from '~/types/catalog'
import { interestsBody, interestsForm } from '~/utils/sectionMappers'
import { validateInterests } from '~/utils/sectionRules'

const props = defineProps<{ record: InterestDto | null, busy: boolean }>()
const emit = defineEmits<{ submit: [body: InterestBody] }>()
const { t } = useI18n()
const { options: levels } = useEnumOptions('studyLevel')
const { options: teaching } = useEnumOptions('teachingLanguage')
const { options: scholarship } = useEnumOptions('scholarshipInterest')
const { options: terms } = useEnumOptions('intakeTerm')

const form = reactive(interestsForm(props.record))
const { errors, submit } = useValidatedForm(() => validateInterests(form))

const firstYear = new Date().getFullYear()
const years = Array.from({ length: INTAKE_YEAR_SPAN + 1 }, (_, i) => {
  const year = String(firstYear + i)
  return { value: year, label: year }
})
</script>

<template>
  <form class="grid gap-8" novalidate @submit.prevent="submit(() => emit('submit', interestsBody(form)))">
    <FormSection :title="t('interests.sectionStudy')">
      <FormSelectField id="int-level" v-model="form.desiredLevel" :label="t('interests.level')" :options="levels" :error="errors.desiredLevel" required />
      <FormSelectField id="int-scholarship" v-model="form.scholarshipInterest" :label="t('interests.scholarship')" :options="scholarship" />
      <TagInput id="int-fields" v-model="form.fields" :label="t('interests.fields')" :hint="t('interests.fieldsHint')" :placeholder="t('interests.fieldsPlaceholder')" :max="MAX_INTEREST_FIELDS" :max-length="MAX_INTEREST_FIELD_LENGTH" :min="1" :error="errors.fields" required />
    </FormSection>

    <FormSection :title="t('interests.sectionWhere')">
      <TagInput id="int-cities" v-model="form.cities" :label="t('interests.cities')" :hint="t('interests.citiesHint')" :placeholder="t('interests.citiesPlaceholder')" :max="MAX_INTEREST_CITIES" :min="MIN_INTEREST_CITIES" :error="errors.cities" required />
      <FormSelectField id="int-teaching" v-model="form.teachingLanguage" :label="t('interests.teachingLanguage')" :options="teaching" />
    </FormSection>

    <FormSection :title="t('interests.sectionWhen')">
      <FormSelectField id="int-year" v-model="form.intakeYear" :label="t('interests.intakeYear')" :options="years" />
      <FormSelectField id="int-term" v-model="form.intakeTerm" :label="t('interests.intakeTerm')" :options="terms" />
      <div class="sm:col-span-2">
        <FormTextareaField id="int-notes" v-model="form.notes" :label="t('interests.notes')" :hint="t('interests.notesHint')" :maxlength="500" />
      </div>
    </FormSection>

    <FormActions :busy="busy" :submit-label="t('common.save')" />
  </form>
</template>
