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
  <div class="grid gap-6">
    <header>
      <h1 class="font-display text-xl font-bold text-slate-900">{{ t('dashboard.accountTitle') }}</h1>
      <p class="mt-1 text-sm text-slate-500">{{ t('dashboard.account.blurb') }}</p>
    </header>

    <div class="grid gap-10">
      <section class="grid gap-4 md:grid-cols-[240px_1fr] md:gap-10">
        <div>
          <h2 class="font-display text-base font-semibold text-slate-900">{{ t('dashboard.account.personal.title') }}</h2>
          <p class="mt-1 text-sm text-slate-500">{{ t('dashboard.account.personal.blurb') }}</p>
        </div>
        <div class="rounded-xl border border-slate-200 bg-white p-5">
          <p class="text-xs font-medium uppercase tracking-wide text-slate-400">{{ t('dashboard.accountUser') }}</p>
          <p class="mt-1 font-medium text-slate-900">{{ user?.email ?? user?.nickName ?? user?.username }}</p>
          <NButton class="mt-4" size="sm" variant="secondary" :to="localePath('/dashboard/profile')">
            {{ t('dashboard.quickProfile') }}
          </NButton>
        </div>
      </section>

      <section class="grid gap-4 border-t border-slate-200 pt-10 md:grid-cols-[240px_1fr] md:gap-10">
        <div>
          <h2 class="font-display text-base font-semibold text-slate-900">{{ t('dashboard.account.security.title') }}</h2>
          <p class="mt-1 text-sm text-slate-500">{{ t('dashboard.account.security.passwordBlurb') }}</p>
        </div>
        <div class="rounded-xl border border-slate-200 bg-white p-5">
          <NAlert v-if="notice" tone="success" class="mb-4">{{ notice }}</NAlert>
          <NAlert v-if="error" tone="danger" class="mb-4">{{ error }}</NAlert>
          <h3 class="mb-3 text-sm font-semibold text-slate-700">{{ t('dashboard.changePassword') }}</h3>
          <PasswordChangeForm :busy="busy" @submit="changePassword" />
        </div>
      </section>

      <section class="grid gap-4 border-t border-slate-200 pt-10 md:grid-cols-[240px_1fr] md:gap-10">
        <div>
          <h2 class="font-display text-base font-semibold text-slate-900">{{ t('dashboard.account.security.emailTitle') }}</h2>
          <p class="mt-1 text-sm text-slate-500">{{ t('dashboard.account.security.emailBlurb') }}</p>
        </div>
        <div class="rounded-xl border border-slate-200 bg-white p-5">
          <NAlert v-if="emailNotice" tone="success" class="mb-4">{{ emailNotice }}</NAlert>
          <p class="mb-3 text-sm text-slate-600">
            {{ t('dashboard.account.security.currentEmail') }}
            <span class="font-medium text-slate-900">{{ user?.email }}</span>
          </p>
          <EmailChangeForm :current-email="user?.email ?? ''" @changed="onEmailChanged" />
        </div>
      </section>

      <section class="grid gap-4 border-t border-slate-200 pt-10 md:grid-cols-[240px_1fr] md:gap-10">
        <div>
          <h2 class="font-display text-base font-semibold text-red-700">{{ t('dashboard.account.danger.title') }}</h2>
          <p class="mt-1 text-sm text-slate-500">{{ t('dashboard.account.danger.subtitle') }}</p>
        </div>
        <div class="rounded-xl border border-red-200 bg-red-50/60 p-5">
          <p class="text-sm text-red-900">{{ t('dashboard.account.danger.blurb') }}</p>
          <NButton class="mt-4" size="sm" variant="secondary" :to="supportMailto">
            {{ t('dashboard.account.danger.cta') }}
          </NButton>
        </div>
      </section>

      <div class="border-t border-slate-200 pt-8">
        <NButton data-test="sign-out" variant="secondary" @click="signOut">{{ t('common.signOut') }}</NButton>
      </div>
    </div>
  </div>
</template>
