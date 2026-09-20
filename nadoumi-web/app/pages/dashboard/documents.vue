<script setup lang="ts">
import type { ApplicantDto } from '~/types/catalog'
definePageMeta({ layout: 'dashboard', middleware: ['auth', 'onboarding'] })

const { t } = useI18n()
const localePath = useLocalePath()
const { activeApplicantId } = useSession()
const { listMine, get } = useApplicant()

const applicant = ref<ApplicantDto | null>(null)
const pending = ref(true)

async function load() {
  pending.value = true
  const mine = await listMine().catch(() => [])
  const chosen = mine.find(a => a.id === activeApplicantId.value) ?? mine[0] ?? null
  applicant.value = chosen ? await get(chosen.id) : null
  pending.value = false
}
await load()

useSeo(t('dashboard.documentsTitle'), t('dashboard.documentsBlurb'))
</script>

<template>
  <div class="grid gap-6">
    <header>
      <h1 class="font-display text-xl font-bold text-slate-900">{{ t('dashboard.documentsTitle') }}</h1>
      <p class="mt-1 text-sm text-slate-500">{{ t('dashboard.documentsBlurb') }}</p>
    </header>

    <AsyncState :pending="pending">
      <NAlert v-if="!applicant" tone="warning">
        {{ t('dashboard.createProfileBlurb') }}
        <NuxtLink :to="localePath('/dashboard/profile')" class="ms-1 underline">{{ t('dashboard.quickProfile') }}</NuxtLink>
      </NAlert>
      <div v-else class="grid gap-4">
        <PhotoUploadCard :applicant-id="applicant.id" @changed="load" />
        <PassportUploadCard
          :applicant-id="applicant.id"
          :profile="{ givenName: applicant.givenName, familyName: applicant.familyName, dob: applicant.dob }"
          @changed="load"
          @edit-profile="navigateTo(localePath('/dashboard/profile'))"
        />
      </div>
    </AsyncState>
  </div>
</template>
