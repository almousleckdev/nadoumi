<script setup lang="ts">
definePageMeta({ layout: 'dashboard', middleware: ['auth', 'onboarding'] })
const { t } = useI18n()
const { user, signOut } = useSession()
const { busy, notice, error, run } = useAsyncAction()

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
    <SectionCard :title="t('dashboard.accountTitle')">
      <p class="text-sm text-slate-600">{{ t('dashboard.accountUser') }}</p>
      <p class="font-medium">{{ user?.email ?? user?.nickName ?? user?.username }}</p>
    </SectionCard>

    <SectionCard :title="t('dashboard.changePassword')">
      <NAlert v-if="notice" tone="success" class="mb-4">{{ notice }}</NAlert>
      <NAlert v-if="error" tone="danger" class="mb-4">{{ error }}</NAlert>
      <PasswordChangeForm :busy="busy" @submit="changePassword" />
    </SectionCard>

    <div>
      <NButton data-test="sign-out" variant="secondary" @click="signOut">{{ t('common.signOut') }}</NButton>
    </div>
  </div>
</template>
