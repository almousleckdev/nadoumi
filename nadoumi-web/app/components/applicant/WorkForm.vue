<script setup lang="ts">
import type { WorkBody } from '~/composables/useApplicant'
import { CHINA_COUNTRY } from '~/constants/applicantOptions'
import type { WorkDto } from '~/types/catalog'
import { isoDate } from '~/utils/dates'
import { workBody, workForm } from '~/utils/sectionMappers'
import { validateWork } from '~/utils/sectionRules'

const props = defineProps<{ record: WorkDto | null, busy: boolean }>()
const emit = defineEmits<{ submit: [body: WorkBody], cancel: [] }>()
const { t } = useI18n()
const { countries } = useLocaleOptions()
const { options: employmentTypes } = useEnumOptions('employmentType')
const { options: visaTypes } = useEnumOptions('chinaVisaType')

const form = reactive(workForm(props.record))
const today = isoDate(new Date())
const inChina = computed(() => form.country === CHINA_COUNTRY)
const { errors, submit } = useValidatedForm(() => validateWork(form))

watch(() => form.current, (current: boolean) => { if (current) form.endDate = '' })
</script>

<template>
  <form class="grid gap-4 sm:grid-cols-2" novalidate @submit.prevent="submit(() => emit('submit', workBody(form)))">
    <FormTextField id="work-employer" v-model="form.employer" :label="t('work.employer')" :error="errors.employer" :maxlength="200" required />
    <FormTextField id="work-title" v-model="form.jobTitle" :label="t('work.jobTitle')" :error="errors.jobTitle" :maxlength="150" required />
    <FormSelectField id="work-type" v-model="form.employmentType" :label="t('work.employmentType')" :options="employmentTypes" />
    <FormComboboxField id="work-country" v-model="form.country" :label="t('work.country')" :options="countries" :error="errors.country" required />
    <FormTextField id="work-city" v-model="form.city" :label="t('work.city')" :maxlength="80" />
    <div class="hidden sm:block" aria-hidden="true" />
    <FormTextField id="work-start" v-model="form.startDate" type="date" :label="t('work.startDate')" :max="today" :error="errors.startDate" required />
    <FormTextField id="work-end" v-model="form.endDate" type="date" :label="t('work.endDate')" :max="today" :disabled="form.current" :error="errors.endDate" :required="!form.current" />
    <NCheckbox id="work-current" v-model="form.current" class="sm:col-span-2">{{ t('work.current') }}</NCheckbox>
    <template v-if="inChina">
      <NAlert tone="info" class="sm:col-span-2">{{ t('work.chinaNotice') }}</NAlert>
      <FormSelectField id="work-visa" v-model="form.workVisaType" :label="t('work.visaType')" :options="visaTypes" :error="errors.workVisaType" required />
      <FormTextField id="work-visa-expiry" v-model="form.workVisaExpiry" type="date" :label="t('work.visaExpiry')" />
    </template>
    <div class="sm:col-span-2">
      <FormTextareaField id="work-description" v-model="form.description" :label="t('work.description')" :maxlength="1000" />
    </div>
    <FormActions :busy="busy" cancellable @cancel="emit('cancel')" />
  </form>
</template>
