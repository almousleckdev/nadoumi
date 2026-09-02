<script setup lang="ts">
import type { ApplicantDto } from '~/types/catalog'
import type { SelfApplicantBody } from '~/composables/useApplicant'

const props = defineProps<{ modelValue: ApplicantDto | null; busy: boolean }>()
const emit = defineEmits<{ submit: [body: SelfApplicantBody] }>()
const { t } = useI18n()

const form = reactive<SelfApplicantBody>({
  givenName: '', familyName: '', dob: '', nationality: '', passportNo: '', email: '', phone: '',
})
watchEffect(() => {
  const a = props.modelValue
  if (!a) return
  form.givenName = a.givenName ?? ''
  form.familyName = a.familyName ?? ''
  form.dob = a.dob ?? ''
  form.nationality = a.nationality ?? ''
  form.passportNo = a.passportNo ?? ''
  form.email = a.email ?? ''
  form.phone = a.phone ?? ''
})

const error = ref('')
function submit() {
  error.value = ''
  if (!form.givenName || !form.familyName) { error.value = t('validation.required'); return }
  if (form.nationality && form.nationality.length !== 2) { error.value = t('validation.len2'); return }
  emit('submit', { ...form })
}
</script>

<template>
  <form class="grid gap-4 sm:grid-cols-2" @submit.prevent="submit">
    <NAlert v-if="error" tone="danger" class="sm:col-span-2">{{ error }}</NAlert>
    <NField :label="t('dashboard.givenName')" for="givenName" required>
      <NInput id="givenName" v-model="form.givenName" :maxlength="100" />
    </NField>
    <NField :label="t('dashboard.familyName')" for="familyName" required>
      <NInput id="familyName" v-model="form.familyName" :maxlength="100" />
    </NField>
    <NField :label="t('dashboard.dob')" for="dob">
      <NInput id="dob" v-model="form.dob" type="date" />
    </NField>
    <NField :label="t('dashboard.nationality')" for="nationality" :hint="t('validation.len2')">
      <NInput id="nationality" v-model="form.nationality" :maxlength="2" />
    </NField>
    <NField :label="t('dashboard.passportNo')" for="passportNo">
      <NInput id="passportNo" v-model="form.passportNo" :maxlength="64" />
    </NField>
    <NField :label="t('auth.email')" for="pemail">
      <NInput id="pemail" v-model="form.email" type="email" :maxlength="120" />
    </NField>
    <NField :label="t('dashboard.phone')" for="phone">
      <NInput id="phone" v-model="form.phone" :maxlength="32" />
    </NField>
    <div class="sm:col-span-2">
      <NButton type="submit" :loading="busy">{{ t('common.save') }}</NButton>
    </div>
  </form>
</template>
