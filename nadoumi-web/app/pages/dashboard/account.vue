<script setup lang="ts">
import { CONTACT } from '~/data/contact'

definePageMeta({ layout: 'dashboard', middleware: ['auth', 'onboarding'] })
const { t } = useI18n()
const localePath = useLocalePath()
const { user, signOut } = useSession()
const { busy, notice, error, run } = useAsyncAction()

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

useSeo(t('dashboard.accountTitle'), t('dashboard.accountTitle'))
</script>

<template>
  <div class="grid gap-6">
    <header>
      <h1 class="font-display text-xl font-bold text-slate-900">{{ t('dashboard.accountTitle') }}</h1>
      <p class="mt-1 text-sm text-slate-500">{{ t('dashboard.account.blurb') }}</p>
    </header>

    <SectionCard :title="t('dashboard.account.personal.title')">
      <p class="text-sm text-slate-600">{{ t('dashboard.accountUser') }}</p>
      <p class="font-medium">{{ user?.email ?? user?.nickName ?? user?.username }}</p>
      <p class="mt-3 text-sm text-slate-500">{{ t('dashboard.account.personal.blurb') }}</p>
      <NButton class="mt-3" size="sm" variant="secondary" :to="localePath('/dashboard/profile')">
        {{ t('dashboard.quickProfile') }}
      </NButton>
    </SectionCard>

    <SectionCard :title="t('dashboard.account.security.title')">
      <NAlert v-if="notice" tone="success" class="mb-4">{{ notice }}</NAlert>
      <NAlert v-if="error" tone="danger" class="mb-4">{{ error }}</NAlert>
      <h2 class="mb-2 text-sm font-semibold text-slate-700">{{ t('dashboard.changePassword') }}</h2>
      <PasswordChangeForm :busy="busy" @submit="changePassword" />
      <p class="mt-6 border-t border-slate-100 pt-4 text-sm text-slate-500">
        {{ t('dashboard.account.security.emailChangeBlurb') }}
        <a :href="supportMailto" class="font-medium text-brand-700 hover:underline">{{ supportEmail }}</a>
      </p>
    </SectionCard>

    <SectionCard :title="t('dashboard.account.notifications.title')">
      <p class="text-sm text-slate-500">{{ t('dashboard.account.notifications.blurb') }}</p>
      <NButton class="mt-3" size="sm" variant="secondary" :to="localePath('/dashboard/notifications')">
        {{ t('dashboard.nav.notifications') }}
      </NButton>
    </SectionCard>

    <SectionCard :title="t('dashboard.account.documents.title')">
      <p class="text-sm text-slate-500">{{ t('dashboard.account.documents.blurb') }}</p>
      <NButton class="mt-3" size="sm" variant="secondary" :to="localePath('/dashboard/documents')">
        {{ t('dashboard.documentsTitle') }}
      </NButton>
    </SectionCard>

    <SectionCard :title="t('dashboard.account.danger.title')">
      <NAlert tone="danger">
        {{ t('dashboard.account.danger.blurb') }}
        <NButton class="mt-3" size="sm" variant="secondary" :to="supportMailto">
          {{ t('dashboard.account.danger.cta') }}
        </NButton>
      </NAlert>
    </SectionCard>

    <div>
      <NButton data-test="sign-out" variant="secondary" @click="signOut">{{ t('common.signOut') }}</NButton>
    </div>
  </div>
</template>
