<script setup lang="ts">
import type { EducationDto } from '~/types/catalog'
import type { EducationBody } from '~/composables/useApplicant'
definePageMeta({ layout: 'dashboard', middleware: ['auth', 'onboarding'] })

const { t } = useI18n()
const localePath = useLocalePath()
const { activeApplicantId } = useSession()
const { listEducation, addEducation, updateEducation, deleteEducation } = useApplicant()

const items = ref<EducationDto[]>([])
const busy = ref(false)
const notice = ref('')
const error = ref('')

async function reload() {
  if (activeApplicantId.value == null) return
  items.value = await listEducation(activeApplicantId.value).catch(() => [])
}
await reload()

async function run(fn: () => Promise<unknown>, ok: string) {
  if (activeApplicantId.value == null) return
  busy.value = true; error.value = ''; notice.value = ''
  try { await fn(); await reload(); notice.value = ok }
  catch (err) { error.value = problemMessage(err, t('auth.genericError')) }
  finally { busy.value = false }
}

const onAdd = (b: EducationBody) => run(() => addEducation(activeApplicantId.value!, b), t('dashboard.addedOk'))
const onUpdate = (id: number, b: EducationBody) => run(() => updateEducation(activeApplicantId.value!, id, b), t('dashboard.savedOk'))
const onRemove = (id: number) => run(() => deleteEducation(activeApplicantId.value!, id), t('dashboard.removedOk'))

useSeo(t('dashboard.eduTitle'), t('dashboard.eduTitle'))
</script>

<template>
  <SectionCard :title="t('dashboard.eduTitle')">
    <NAlert v-if="activeApplicantId == null" tone="warning">
      {{ t('dashboard.createProfileBlurb') }}
      <NuxtLink :to="localePath('/dashboard/profile')" class="ms-1 underline">{{ t('dashboard.quickProfile') }}</NuxtLink>
    </NAlert>
    <template v-else>
      <NAlert v-if="notice" tone="success" class="mb-4">{{ notice }}</NAlert>
      <NAlert v-if="error" tone="danger" class="mb-4">{{ error }}</NAlert>
      <EducationList :items="items" :busy="busy" @add="onAdd" @update="onUpdate" @remove="onRemove" />
    </template>
  </SectionCard>
</template>
