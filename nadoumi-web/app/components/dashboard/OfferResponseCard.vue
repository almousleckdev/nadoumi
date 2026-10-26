<script setup lang="ts">
/** The student's answer to an offer. Declining closes the application, so it asks first. */
const props = defineProps<{ applicationId: number, conditional: boolean }>()
const emit = defineEmits<{ answered: [] }>()

const { t } = useI18n()
const { acceptOffer, declineOffer } = useApplications()
const { busy, error, run } = useAsyncAction()
const confirmDecline = ref(false)

async function accept() {
  if (await run(() => acceptOffer(props.applicationId), t('dashboard.applications.offer.accepted'))) emit('answered')
}

async function decline() {
  const done = await run(() => declineOffer(props.applicationId))
  confirmDecline.value = false
  if (done) emit('answered')
}
</script>

<template>
  <SectionCard :title="t('dashboard.applications.offer.title')">
    <div class="grid gap-3" data-test="offer-response">
      <p class="text-sm text-slate-700">{{ conditional ? t('dashboard.applications.offer.conditionalBody') : t('dashboard.applications.offer.body') }}</p>
      <NAlert v-if="error" tone="danger">{{ error }}</NAlert>
      <div class="flex flex-wrap gap-2">
        <NButton :loading="busy" data-test="offer-accept" @click="accept">{{ t('dashboard.applications.offer.accept') }}</NButton>
        <NButton variant="secondary" :disabled="busy" data-test="offer-decline" @click="confirmDecline = true">
          {{ t('dashboard.applications.offer.decline') }}
        </NButton>
      </div>
    </div>
    <NModal v-model="confirmDecline" :title="t('dashboard.applications.offer.declineTitle')">
      <p class="text-sm text-slate-700">{{ t('dashboard.applications.offer.declineBody') }}</p>
      <template #footer>
        <NButton variant="secondary" :disabled="busy" @click="confirmDecline = false">{{ t('common.cancel') }}</NButton>
        <NButton variant="danger" :loading="busy" data-test="offer-decline-confirm" @click="decline">{{ t('dashboard.applications.offer.declineConfirm') }}</NButton>
      </template>
    </NModal>
  </SectionCard>
</template>
