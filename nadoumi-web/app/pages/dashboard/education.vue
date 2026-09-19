<script setup lang="ts">
import type { EducationDto } from '~/types/catalog'
import type { EducationBody } from '~/composables/useApplicant'
definePageMeta({ layout: 'dashboard', middleware: ['auth', 'onboarding'] })

const { t } = useI18n()
const localePath = useLocalePath()
const { activeApplicantId } = useSession()
const { listEducation, addEducation, updateEducation, deleteEducation } = useApplicant()

const items = ref<EducationDto[]>([])
const { busy, notice, error, run } = useAsyncAction()

async function reload() {
  if (activeApplicantId.value == null) return
  items.value = await listEducation(activeApplicantId.value).catch(() => [])
}
await reload()

function runEdu(fn: () => Promise<unknown>, ok: string) {
  if (activeApplicantId.value == null) return
  return run(async () => { await fn(); await reload() }, ok)
}

const onAdd = (b: EducationBody) => runEdu(() => addEducation(activeApplicantId.value!, b), t('dashboard.addedOk'))
const onUpdate = (id: number, b: EducationBody) => runEdu(() => updateEducation(activeApplicantId.value!, id, b), t('dashboard.savedOk'))
const onRemove = (id: number) => runEdu(() => deleteEducation(activeApplicantId.value!, id), t('dashboard.removedOk'))

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
