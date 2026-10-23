<script setup lang="ts">
import type { FamilyMemberBody } from '~/composables/useApplicantFamily'
import type { FamilyMemberDto } from '~/types/family'
import { isoDate } from '~/utils/dates'
import { familyMemberBody, familyMemberForm, validateFamilyMember } from '~/utils/familyForms'

const props = defineProps<{ record: FamilyMemberDto | null, busy: boolean }>()
const emit = defineEmits<{ submit: [body: FamilyMemberBody], cancel: [] }>()
const { t } = useI18n()
const { options: relations } = useEnumOptions('familyRelation')

const form = reactive(familyMemberForm(props.record))
const { errors, submit } = useValidatedForm(() => validateFamilyMember(form))
const today = isoDate(new Date())
</script>

<template>
  <form class="grid gap-4 sm:grid-cols-2" novalidate @submit.prevent="submit(() => emit('submit', familyMemberBody(form)))">
    <FormSelectField id="fm-relation" v-model="form.relation" :label="t('family.member.relation')" :options="relations" :error="errors.relation" required />
    <FormTextField id="fm-name" v-model="form.fullName" :label="t('family.member.fullName')" :error="errors.fullName" autocomplete="off" :maxlength="150" required />
    <FormTextField id="fm-dob" v-model="form.dateOfBirth" type="date" :label="t('family.member.dateOfBirth')" :max="today" :error="errors.dateOfBirth" />
    <FormTextField id="fm-nationality" v-model="form.nationality" :label="t('family.member.nationality')" :maxlength="60" />
    <div class="sm:col-span-2">
      <NCheckbox id="fm-deceased" v-model="form.deceased">{{ t('family.member.deceased') }}</NCheckbox>
    </div>
    <div v-if="form.deceased" class="sm:col-span-2">
      <FormTextField id="fm-note" v-model="form.note" :label="t('family.member.note')" :hint="t('family.member.noteHint')" :error="errors.note" :maxlength="300" required />
    </div>
    <template v-else>
      <FormTextField id="fm-company" v-model="form.company" :label="t('family.member.company')" :maxlength="200" />
      <FormTextField id="fm-position" v-model="form.position" :label="t('family.member.position')" :maxlength="150" />
      <div class="sm:col-span-2">
        <FormTextField id="fm-company-address" v-model="form.companyAddress" :label="t('family.member.companyAddress')" :maxlength="300" />
      </div>
      <FormPhoneField id="fm-phone" v-model="form.phone" :label="t('family.member.phone')" />
      <FormTextField id="fm-email" v-model="form.email" type="email" :label="t('family.member.email')" :error="errors.email" autocomplete="off" :maxlength="120" />
    </template>
    <FormActions :busy="busy" cancellable @cancel="emit('cancel')" />
  </form>
</template>
