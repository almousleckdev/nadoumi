<script setup lang="ts">
import type { PersonalDetailsBody } from '~/composables/useApplicantFamily'
import type { PersonalDetailsDto } from '~/types/family'
import { personalBody, personalForm } from '~/utils/familyForms'

const props = defineProps<{ record: PersonalDetailsDto | null, busy: boolean }>()
const emit = defineEmits<{ submit: [body: PersonalDetailsBody] }>()
const { t } = useI18n()
const { options: statuses } = useEnumOptions('maritalStatus')

const form = reactive(personalForm(props.record))
const mailingOptions = computed(() => (['CURRENT', 'PERMANENT', 'OTHER'] as const)
  .map(value => ({ value, label: t(`family.personal.mailing.${value}`) })))
</script>

<template>
  <form class="grid gap-4 sm:grid-cols-2" novalidate @submit.prevent="emit('submit', personalBody(form))">
    <FormSelectField id="pd-marital" v-model="form.maritalStatus" :label="t('family.personal.marital')" :options="statuses" />
    <FormTextField id="pd-religion" v-model="form.religion" :label="t('family.personal.religion')" :maxlength="60" />
    <FormTextField id="pd-birthplace" v-model="form.placeOfBirth" :label="t('family.personal.placeOfBirth')" :maxlength="120" />
    <FormTextField id="pd-language" v-model="form.officialLanguage" :label="t('family.personal.officialLanguage')" :maxlength="60" />
    <div class="sm:col-span-2">
      <FormTextField id="pd-address" v-model="form.permanentAddress" :label="t('family.personal.permanentAddress')" autocomplete="street-address" :maxlength="300" />
    </div>
    <FormTextField id="pd-postcode" v-model="form.permanentPostcode" :label="t('family.personal.postcode')" autocomplete="postal-code" :maxlength="20" />
    <FormTextField id="pd-current-postcode" v-model="form.currentPostcode" :label="t('family.personal.currentPostcode')" autocomplete="postal-code" :maxlength="20" />
    <div class="sm:col-span-2">
      <FormSelectField id="pd-mailing" v-model="form.mailingChoice" :label="t('family.personal.mailingTitle')" :options="mailingOptions" />
      <p class="mt-1 text-sm text-slate-500">{{ t('family.personal.mailingHint') }}</p>
    </div>
    <template v-if="form.mailingChoice === 'OTHER'">
      <div class="sm:col-span-2">
        <FormTextField id="pd-mailing-address" v-model="form.mailingAddress" :label="t('family.personal.mailingAddress')" autocomplete="street-address" :maxlength="300" />
      </div>
      <FormTextField id="pd-mailing-postcode" v-model="form.mailingPostcode" :label="t('family.personal.postcode')" :maxlength="20" />
    </template>
    <FormActions :busy="busy" />
  </form>
</template>
