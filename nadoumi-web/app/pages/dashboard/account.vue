<script setup lang="ts">
import { CONTACT } from '~/data/contact'

definePageMeta({ layout: 'dashboard', middleware: ['auth', 'onboarding'] })
const { t } = useI18n()
const localePath = useLocalePath()
const { user, refresh, signOut } = useSession()
const { busy, notice, error, run } = useAsyncAction()
const emailNotice = ref('')

const supportEmail = CONTACT.emails[0]
const supportMailto = computed(() => `mailto:${supportEmail}?subject=${encodeURIComponent(t('dashboard.account.danger.mailSubject'))}`)

async function changePassword(payload: { current: string; next: string }) {
  await run(async () => {
    await $fetch('/api/student-password', {
      method: 'POST',
      body: { currentPassword: payload.current, newPassword: payload.next },
    })
  }, `${t('dashboard.pwUpdated')} ${t('dashboard.pwOtherSessionsEnded')}`)
}

async function onEmailChanged() {
  emailNotice.value = t('dashboard.account.security.emailUpdated')
  await refresh()
}

useSeo(t('dashboard.accountTitle'), t('dashboard.accountTitle'))
</script>

<template>
  <div class="grid gap-6 md:grid-cols-[300px_1fr]">
    <!-- Left Column (Identity) -->
    <SectionCard class="h-fit">
      <div class="flex flex-col items-center pb-6 border-b border-slate-100">
        <div class="h-24 w-24 rounded-full bg-brand-100 text-brand-600 flex items-center justify-center text-3xl font-medium mb-4">
          {{ (user?.givenName?.[0] || user?.email?.[0] || '?').toUpperCase() }}
        </div>
        <h2 class="font-display text-lg font-bold text-slate-900">{{ user?.givenName }} {{ user?.familyName }}</h2>
        <p class="text-sm text-slate-500">{{ user?.email ?? user?.username }}</p>
        <div class="mt-4 flex gap-2">
          <NButton size="sm" variant="secondary" :to="localePath('/dashboard/profile')">{{ t('dashboard.quickProfile') }}</NButton>
          <NButton size="sm" variant="secondary" @click="signOut">{{ t('common.signOut') }}</NButton>
        </div>
      </div>
      <div class="pt-6">
        <h3 class="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-3">{{ t('dashboard.account.danger.title') }}</h3>
        <p class="text-sm text-slate-600 mb-3">{{ t('dashboard.account.danger.blurb') }}</p>
        <NButton size="sm" variant="danger" :to="supportMailto" class="w-full">{{ t('dashboard.account.danger.cta') }}</NButton>
      </div>
    </SectionCard>

    <!-- Right Column (Forms) -->
    <div class="grid gap-6">
      <SectionCard :title="t('dashboard.account.security.emailTitle')">
        <p class="text-sm text-slate-500 mb-4">{{ t('dashboard.account.security.emailBlurb') }}</p>
        <NAlert v-if="emailNotice" tone="success" class="mb-4">{{ emailNotice }}</NAlert>
        <div class="max-w-md">
          <EmailChangeForm :current-email="user?.email ?? ''" @changed="onEmailChanged" />
        </div>
      </SectionCard>

      <SectionCard :title="t('dashboard.account.security.title')">
        <p class="text-sm text-slate-500 mb-4">{{ t('dashboard.account.security.passwordBlurb') }}</p>
        <NAlert v-if="notice" tone="success" class="mb-4">{{ notice }}</NAlert>
        <NAlert v-if="error" tone="danger" class="mb-4">{{ error }}</NAlert>
        <div class="max-w-md">
          <PasswordChangeForm :busy="busy" @submit="changePassword" />
        </div>
      </SectionCard>

      <SectionCard :title="t('dashboard.account.documents.title')">
        <p class="text-sm text-slate-500 mb-4">{{ t('dashboard.account.documents.blurb') }}</p>
        <NButton size="sm" variant="secondary" :to="localePath('/dashboard/documents')">{{ t('dashboard.documentsTitle') }}</NButton>
      </SectionCard>
    </div>
  </div>
</template>
