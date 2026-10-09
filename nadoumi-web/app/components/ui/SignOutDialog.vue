<script setup lang="ts">
const { t } = useI18n()
const { open, dismiss } = useSignOutConfirm()
const { signOut } = useSession()
const busy = ref(false)

async function confirm() {
  busy.value = true
  try {
    await signOut()
  }
  finally {
    busy.value = false
    dismiss()
  }
}
</script>

<template>
  <NModal v-model="open" :title="t('common.signOutTitle')">
    <p class="text-sm text-slate-600">{{ t('common.signOutMessage') }}</p>
    <template #footer>
      <NButton variant="ghost" size="sm" data-test="sign-out-stay" @click="dismiss">{{ t('common.signOutStay') }}</NButton>
      <NButton variant="primary" size="sm" :loading="busy" data-test="sign-out-confirm" @click="confirm">{{ t('common.signOut') }}</NButton>
    </template>
  </NModal>
</template>
