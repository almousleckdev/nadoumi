<script setup lang="ts">
import type { ApplicantDto } from '~/types/catalog'
import type { SelfApplicantBody } from '~/composables/useApplicant'
definePageMeta({ layout: 'dashboard', middleware: ['auth', 'onboarding'] })

const { t } = useI18n()
const { activeApplicantId, refresh } = useSession()
const { listMine, get, create, update } = useApplicant()

const current = ref<ApplicantDto | null>(null)
const busy = ref(false)
const notice = ref('')
const error = ref('')

async function load() {
  const mine = await listMine().catch(() => [])
  const chosen = mine.find(a => a.id === activeApplicantId.value) ?? mine[0] ?? null
  current.value = chosen ? await get(chosen.id) : null
}
await load()

async function onSubmit(body: SelfApplicantBody) {
  busy.value = true; error.value = ''; notice.value = ''
  try {
    if (current.value) {
      current.value = await update(current.value.id, body)
    }
    else {
      current.value = await create(body)
      await refresh()
    }
    notice.value = t('dashboard.savedOk')
  }
  catch (err) {
    error.value = problemMessage(err, t('auth.genericError'))
  }
  finally {
    busy.value = false
  }
}

useSeo(t('dashboard.profileTitle'), t('dashboard.createProfileBlurb'))
</script>

<template>
  <SectionCard :title="current ? t('dashboard.profileTitle') : t('dashboard.createProfileTitle')">
    <NAlert v-if="notice" tone="success" class="mb-4">{{ notice }}</NAlert>
    <NAlert v-if="error" tone="danger" class="mb-4">{{ error }}</NAlert>
    <p v-if="!current" class="mb-4 text-sm text-slate-600">{{ t('dashboard.createProfileBlurb') }}</p>
    <ProfileForm :model-value="current" :busy="busy" @submit="onSubmit" />
  </SectionCard>
</template>
