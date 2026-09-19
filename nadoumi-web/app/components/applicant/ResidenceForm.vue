<script setup lang="ts">
import type { ResidenceBody } from '~/composables/useApplicant'
import { CHINA_COUNTRY } from '~/constants/applicantOptions'
import type { ResidenceDto } from '~/types/catalog'
import type { Option } from '~/utils/countries'
import { isoDate } from '~/utils/dates'
import { residenceBody, residenceForm } from '~/utils/sectionMappers'
import { validateResidence } from '~/utils/sectionRules'

const props = defineProps<{ record: ResidenceDto | null, busy: boolean }>()
const emit = defineEmits<{ submit: [body: ResidenceBody] }>()
const { t } = useI18n()
const { countries } = useLocaleOptions()
const { options: levels } = useEnumOptions('educationLevel')
const { options: visaTypes } = useEnumOptions('chinaVisaType')

const form = reactive(residenceForm(props.record))
const { errors, submit } = useValidatedForm(() => validateResidence(form))

/** The server refuses "in China" with another country, so the two answers are kept in step. */
const otherCountries = computed(() => countries.value.filter((c: Option) => c.value !== CHINA_COUNTRY))
watch(() => form.inChina, (inChina: boolean | null) => {
  if (inChina === null) return
  form.country = inChina ? CHINA_COUNTRY : ''
})

const tomorrow = isoDate(new Date(Date.now() + 24 * 60 * 60 * 1000))
</script>

<template>
  <form class="grid gap-8" novalidate @submit.prevent="submit(() => emit('submit', residenceBody(form)))">
    <FormSection :title="t('location.sectionWhere')">
      <div class="sm:col-span-2">
        <YesNoField id="loc-in-china" v-model="form.inChina" :label="t('location.inChina')" :error="errors.inChina" />
      </div>
      <template v-if="form.inChina !== null">
        <FormSelectField v-if="!form.inChina" id="loc-country" v-model="form.country" :label="t('location.country')" :options="otherCountries" :error="errors.country" required />
        <FormTextField id="loc-city" v-model="form.city" :label="t('location.city')" :error="errors.city" autocomplete="address-level2" :maxlength="80" required />
        <div class="sm:col-span-2">
          <FormTextField id="loc-address" v-model="form.address" :label="t('location.otherInfo')" :hint="t('location.otherInfoHint')" :maxlength="255" />
        </div>
      </template>
    </FormSection>

    <FormSection v-if="form.inChina" :title="t('location.sectionChina')">
      <NAlert tone="info" class="sm:col-span-2">{{ t('location.chinaNotice') }}</NAlert>
      <FormSelectField id="loc-level" v-model="form.chinaEducationLevel" :label="t('location.currentLevel')" :options="levels" :error="errors.chinaEducationLevel" required />
      <FormTextField id="loc-school" v-model="form.chinaSchool" :label="t('location.school')" :maxlength="200" />
      <FormSelectField id="loc-visa" v-model="form.visaType" :label="t('location.visaType')" :options="visaTypes" :error="errors.visaType" required />
      <FormTextField id="loc-visa-expiry" v-model="form.visaExpiryDate" type="date" :label="t('location.visaExpiry')" :min="tomorrow" :error="errors.visaExpiryDate" required />
    </FormSection>

    <FormActions :busy="busy" />
  </form>
</template>
