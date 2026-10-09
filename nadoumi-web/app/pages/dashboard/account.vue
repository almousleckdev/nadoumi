<script setup lang="ts">
import { computed, ref } from 'vue'
import { CONTACT } from '~/data/contact'
import { PHOTO_ACCEPT, PHOTO_MAX_MB, PHOTO_MIME, PHOTO_MIN_PX } from '~/constants/passport'
import { imageSize, readAsDataUrl, validateFile } from '~/utils/files'

definePageMeta({ layout: 'dashboard', middleware: ['auth', 'onboarding'] })
const { t } = useI18n()
const localePath = useLocalePath()
const toast = useToast()
const { user, refresh, signOut } = useSession()
const { primary } = useMyApplicant()
const { url: photoUrl, refresh: refreshPhoto } = useMyPhoto()
const { uploadPhoto } = useApplicant()
const { busy, error, run } = useAsyncAction()

const supportEmail = CONTACT.emails[0]
const supportMailto = computed(() => `mailto:${supportEmail}?subject=${encodeURIComponent(t('dashboard.account.danger.mailSubject'))}`)

const fullName = computed(() => {
  const a = primary.value
  const legal = [a?.givenName, a?.familyName].filter(Boolean).join(' ')
  return legal || user.value?.nickName || user.value?.username || ''
})
const initial = computed(() => (fullName.value[0] || user.value?.email?.[0] || '?').toUpperCase())
const studentRef = computed(() => (user.value ? `STU-${user.value.userId}` : ''))

const photoBusy = ref(false)
async function onPhoto(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = '' // picking the same file again must still fire
  if (!file || !primary.value) return
  const problem = validateFile(file, { mime: PHOTO_MIME, maxMb: PHOTO_MAX_MB })
  if (problem) {
    toast.error(problem === 'badType' ? t('dashboard.account.photoBadType') : t('dashboard.account.photoTooBig', { mb: PHOTO_MAX_MB }))
    return
  }
  const { width, height } = await imageSize(await readAsDataUrl(file))
  if (width < PHOTO_MIN_PX || height < PHOTO_MIN_PX) {
    toast.error(t('onboarding.photo.tooSmall', { px: PHOTO_MIN_PX }))
    return
  }
  photoBusy.value = true
  try {
    await uploadPhoto(primary.value.id, file)
    await refreshPhoto()
    toast.success(t('dashboard.account.photoSaved'))
  }
  catch {
    toast.error(t('dashboard.account.photoFailed'))
  }
  finally {
    photoBusy.value = false
  }
}

async function changePassword(payload: { current: string; next: string }) {
  await run(async () => {
    await $fetch('/api/student-password', {
      method: 'POST',
      body: { currentPassword: payload.current, newPassword: payload.next },
    })
  }, `${t('dashboard.pwUpdated')} ${t('dashboard.pwOtherSessionsEnded')}`)
}

async function onEmailChanged() {
  toast.success(t('dashboard.account.security.emailUpdated'))
  await refresh()
}

useSeo(t('dashboard.accountTitle'), t('dashboard.account.subtitle'))
</script>

<template>
  <div class="mx-auto grid max-w-4xl gap-6">
    <header>
      <h1 class="font-display text-2xl font-bold tracking-tight text-slate-900">{{ t('dashboard.accountTitle') }}</h1>
      <p class="mt-1 text-sm text-slate-600">{{ t('dashboard.account.subtitle') }}</p>
    </header>

    <section class="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm" data-test="identity">
      <div class="h-20 bg-gradient-to-r from-[var(--ink-900)] via-[var(--ink-800)] to-brand-700" aria-hidden="true" />
      <div class="flex flex-col gap-5 px-5 pb-6 sm:flex-row sm:items-end sm:px-8">
        <div class="-mt-12 shrink-0">
          <div class="relative h-24 w-24">
            <img
              v-if="photoUrl"
              :src="photoUrl"
              :alt="fullName"
              class="h-24 w-24 rounded-full object-cover ring-4 ring-white"
              data-test="account-photo"
            >
            <span
              v-else
              class="grid h-24 w-24 place-items-center rounded-full bg-brand-100 text-3xl font-semibold text-brand-700 ring-4 ring-white"
              aria-hidden="true"
              data-test="account-initial"
            >{{ initial }}</span>
            <label
              class="absolute bottom-0 end-0 grid h-9 w-9 cursor-pointer place-items-center rounded-full bg-white text-slate-700 shadow-md ring-1 ring-slate-200 transition hover:bg-slate-50 focus-within:outline focus-within:outline-2 focus-within:outline-brand-500"
              :title="t('dashboard.account.changePhoto')"
            >
              <NSpinner v-if="photoBusy" class="h-4 w-4" />
              <svg v-else viewBox="0 0 24 24" class="h-4.5 w-4.5" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><path d="M4 8h3l1.5-2h7L17 8h3v11H4V8Z" stroke-linejoin="round" /><circle cx="12" cy="13" r="3.2" /></svg>
              <span class="sr-only">{{ t('dashboard.account.changePhoto') }}</span>
              <input type="file" class="sr-only" :accept="PHOTO_ACCEPT" :disabled="photoBusy || !primary" data-test="photo-input" @change="onPhoto">
            </label>
          </div>
        </div>
        <div class="min-w-0 flex-1">
          <h2 class="truncate font-display text-xl font-bold text-slate-900" data-test="account-name">{{ fullName }}</h2>
          <p class="truncate text-sm text-slate-600">{{ user?.email ?? user?.username }}</p>
          <p class="mt-2 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-500">
            <span><span class="font-medium text-slate-700">{{ t('dashboard.account.studentId') }}</span> {{ studentRef }}</span>
            <span v-if="user?.username"><span class="font-medium text-slate-700">{{ t('auth.username') }}</span> {{ user.username }}</span>
          </p>
        </div>
        <div class="flex shrink-0 gap-2">
          <NButton size="sm" variant="secondary" :to="localePath('/dashboard/profile')">{{ t('dashboard.quickProfile') }}</NButton>
          <NButton size="sm" variant="ghost" data-test="sign-out" @click="signOut">{{ t('common.signOut') }}</NButton>
        </div>
      </div>
    </section>

    <div class="grid gap-6 md:grid-cols-2">
      <SectionCard :title="t('dashboard.account.security.emailTitle')">
        <p class="mb-4 text-sm text-slate-600">{{ t('dashboard.account.security.emailBlurb') }}</p>
        <EmailChangeForm :current-email="user?.email ?? ''" @changed="onEmailChanged" />
      </SectionCard>

      <SectionCard :title="t('dashboard.account.security.title')">
        <p class="mb-4 text-sm text-slate-600">{{ t('dashboard.account.security.passwordBlurb') }}</p>
        <NAlert v-if="error" tone="danger" class="mb-4">{{ error }}</NAlert>
        <PasswordChangeForm :busy="busy" @submit="changePassword" />
      </SectionCard>
    </div>

    <SectionCard :title="t('dashboard.account.documents.title')">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <p class="max-w-xl text-sm text-slate-600">{{ t('dashboard.account.documents.blurb') }}</p>
        <NButton size="sm" variant="secondary" :to="localePath('/dashboard/documents')">{{ t('dashboard.documentsTitle') }}</NButton>
      </div>
    </SectionCard>

    <section class="rounded-2xl border border-red-200 bg-red-50/50 p-5 sm:p-6">
      <h3 class="font-display text-base font-semibold text-red-800">{{ t('dashboard.account.danger.title') }}</h3>
      <div class="mt-2 flex flex-wrap items-center justify-between gap-3">
        <p class="max-w-xl text-sm text-slate-700">{{ t('dashboard.account.danger.blurb') }}</p>
        <NButton size="sm" variant="danger" :to="supportMailto">{{ t('dashboard.account.danger.cta') }}</NButton>
      </div>
    </section>
  </div>
</template>
