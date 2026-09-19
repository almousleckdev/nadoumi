<script setup lang="ts">
import type { EducationBody } from '~/composables/useApplicant'
import type { EducationDto } from '~/types/catalog'
import { educationBody, educationForm } from '~/utils/sectionMappers'
import { validateEducation } from '~/utils/sectionRules'
import { isoDate } from '~/utils/dates'

const props = defineProps<{ record: EducationDto | null, busy: boolean }>()
const emit = defineEmits<{ submit: [body: EducationBody], cancel: [] }>()
const { t } = useI18n()
const { countries } = useLocaleOptions()
const { options: levels } = useEnumOptions('educationLevel')

const form = reactive(educationForm(props.record))
const today = isoDate(new Date())
const { errors, submit } = useValidatedForm(() => validateEducation(form))

watch(() => form.current, (current: boolean) => { if (current) form.endDate = '' })
</script>

<template>
  <form class="grid gap-4 sm:grid-cols-2" novalidate @submit.prevent="submit(() => emit('submit', educationBody(form)))">
    <FormTextField id="edu-institution" v-model="form.institution" :label="t('education.institution')" :error="errors.institution" :maxlength="200" required />
    <FormSelectField id="edu-level" v-model="form.level" :label="t('education.level')" :options="levels" :error="errors.level" required />
    <FormSelectField id="edu-country" v-model="form.country" :label="t('education.country')" :options="countries" :error="errors.country" required />
    <FormTextField id="edu-city" v-model="form.city" :label="t('education.city')" :maxlength="80" />
    <FormTextField id="edu-qualification" v-model="form.qualification" :label="t('education.qualification')" :hint="t('education.qualificationHint')" :maxlength="200" />
    <FormTextField id="edu-field" v-model="form.field" :label="t('education.field')" :maxlength="120" />
    <FormTextField id="edu-start" v-model="form.startDate" type="date" :label="t('education.startDate')" :max="today" :error="errors.startDate" required />
    <FormTextField id="edu-end" v-model="form.endDate" type="date" :label="t('education.endDate')" :max="today" :disabled="form.current" :error="errors.endDate" :required="!form.current" />
    <NCheckbox id="edu-current" v-model="form.current" class="sm:col-span-2">{{ t('education.current') }}</NCheckbox>
    <FormTextField id="edu-gpa" v-model="form.gpa" type="number" :label="t('education.gpa')" />
    <FormTextField id="edu-gpa-scale" v-model="form.gpaScale" type="number" :label="t('education.gpaScale')" :hint="t('education.gpaScaleHint')" />
    <FormActions :busy="busy" cancellable @cancel="emit('cancel')" />
  </form>
</template>
