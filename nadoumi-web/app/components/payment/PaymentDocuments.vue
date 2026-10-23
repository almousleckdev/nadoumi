<script setup lang="ts">
import type { PaymentDocumentDto, PaymentDocumentKind } from '~/types/payment'

/** The receipt and the application form, rendered after the fee is confirmed. They take a moment to appear. */
const props = defineProps<{ applicationId: number }>()

const { t } = useI18n()
const api = useApplicationPayment(props.applicationId)
const toast = useToast()

const POLL_MS = 4000
const MAX_POLLS = 15

const documents = ref<PaymentDocumentDto[]>([])
const polls = ref(0)
const downloading = ref<PaymentDocumentKind | null>(null)
const resending = ref(false)
let timer: ReturnType<typeof setTimeout> | null = null

const READY_KINDS: readonly PaymentDocumentKind[] = ['RECEIPT', 'APPLICATION_FORM']
const ready = computed(() => READY_KINDS.every(k => documents.value.some((d: PaymentDocumentDto) => d.kind === k)))

async function load() {
  try {
    documents.value = await api.documents()
  }
  catch {
    documents.value = []
  }
  polls.value += 1
  if (!ready.value && polls.value < MAX_POLLS) timer = setTimeout(load, POLL_MS)
}

async function download(doc: PaymentDocumentDto) {
  downloading.value = doc.kind
  try {
    const url = URL.createObjectURL(await api.document(doc.kind))
    const link = document.createElement('a')
    link.href = url
    link.download = `${doc.number}.pdf`
    link.click()
    URL.revokeObjectURL(url)
  }
  catch {
    toast.error(t('payment.documents.failed'))
  }
  finally {
    downloading.value = null
  }
}

async function resend() {
  resending.value = true
  try {
    await api.resendReceipt()
    toast.success(t('payment.documents.resent'))
  }
  catch (e) {
    const status = (e as { statusCode?: number, status?: number }).statusCode ?? (e as { status?: number }).status
    toast.error(status === 429 ? t('payment.documents.resendWait') : t('payment.documents.failed'))
  }
  finally {
    resending.value = false
  }
}

onMounted(load)
onBeforeUnmount(() => {
  if (timer !== null) clearTimeout(timer)
})
</script>

<template>
  <div class="grid gap-2 border-t border-slate-200 pt-4" data-test="documents">
    <p class="text-sm font-medium text-slate-900">{{ t('payment.documents.title') }}</p>
    <p v-if="!documents.length" class="text-sm text-slate-500">{{ t('payment.documents.preparing') }}</p>
    <ul v-else class="grid gap-2">
      <li v-for="doc in documents" :key="doc.kind" class="flex flex-wrap items-center justify-between gap-2 text-sm">
        <span class="text-slate-700">
          {{ doc.kind === 'RECEIPT' ? t('payment.documents.receipt') : t('payment.documents.form') }}
          <span class="ms-1 font-mono text-xs text-slate-500">{{ doc.number }}</span>
        </span>
        <NButton variant="secondary" size="sm" :loading="downloading === doc.kind" :data-test="`download-${doc.kind}`" @click="download(doc)">
          {{ t('payment.documents.download') }}
        </NButton>
      </li>
    </ul>
    <div v-if="documents.some((d: PaymentDocumentDto) => d.kind === 'RECEIPT')">
      <NButton variant="ghost" size="sm" :loading="resending" data-test="resend-receipt" @click="resend">
        {{ t('payment.documents.resend') }}
      </NButton>
    </div>
  </div>
</template>
