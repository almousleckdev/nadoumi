<script setup lang="ts">
import type { PersonBody } from '~/composables/useApplicantFamily'
import type { PersonDto } from '~/types/family'
import { personBody, personForm, validatePerson } from '~/utils/familyForms'

const props = defineProps<{ record: PersonDto | null, busy: boolean, idPrefix: string }>()
const emit = defineEmits<{ submit: [body: PersonBody] }>()
const { t } = useI18n()
const { options: genders } = useEnumOptions('personGender')

const form = reactive(personForm(props.record))
const { errors, submit } = useValidatedForm(() => validatePerson(form))
</script>

<template>
  <form class="grid gap-4 sm:grid-cols-2" novalidate @submit.prevent="submit(() => emit('submit', personBody(form)))">
    <FormTextField :id="`${idPrefix}-name`" v-model="form.fullName" :label="t('family.person.fullName')" :error="errors.fullName" autocomplete="off" :maxlength="150" required />
    <FormTextField :id="`${idPrefix}-relationship`" v-model="form.relationship" :label="t('family.person.relationship')" :error="errors.relationship" autocomplete="off" :maxlength="80" required />
    <FormSelectField :id="`${idPrefix}-gender`" v-model="form.gender" :label="t('family.person.gender')" :options="genders" />
    <FormPhoneField :id="`${idPrefix}-phone`" v-model="form.phone" :label="t('family.person.phone')" :error="errors.phone" required />
    <FormTextField :id="`${idPrefix}-email`" v-model="form.email" type="email" :label="t('family.person.email')" :error="errors.email" autocomplete="off" :maxlength="120" />
    <FormTextField :id="`${idPrefix}-position`" v-model="form.position" :label="t('family.person.position')" :maxlength="150" />
    <FormTextField :id="`${idPrefix}-company`" v-model="form.company" :label="t('family.person.company')" :maxlength="200" />
    <FormTextField :id="`${idPrefix}-company-address`" v-model="form.companyAddress" :label="t('family.person.companyAddress')" :maxlength="300" />
    <div class="sm:col-span-2">
      <FormTextField :id="`${idPrefix}-home`" v-model="form.homeAddress" :label="t('family.person.homeAddress')" :maxlength="300" />
    </div>
    <FormActions :busy="busy" />
  </form>
</template>
