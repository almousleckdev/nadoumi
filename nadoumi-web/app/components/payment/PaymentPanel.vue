<script setup lang="ts">
import type { FeeVariant } from '~/composables/useApplicationPayment'
import type { PaymentState, StudentPaymentDto } from '~/types/payment'
import { isStartable } from '~/utils/paymentView'

/**
 * The Nadoumi fee of an application: the application fee by default, or (variant `service`) the service fee asked for once
 * the admission is confirmed. A draft stays saved whether or not it is paid; this panel
 * is how the student pays (a submitted application can be paid again only after staff rejected the payment): open the QR (15 minutes), pay in their banking app, then report the payment.
 * It tells its parent whether the fee is settled so the Submit button can follow.
 */
const props = withDefaults(defineProps<{ applicationId: number, submitted?: boolean, variant?: FeeVariant }>(), {
  variant: 'application',
})
const emit = defineEmits<{ settled: [value: boolean] }>()

const { t } = useI18n()
const api = useApplicationPayment(props.applicationId, props.variant)
const copy = (key: string) => t(props.variant === 'service' ? `payment.service.${key}` : `payment.${key}`)
const { busy, error, run } = useAsyncAction()

const payment = ref<StudentPaymentDto | null>(null)
const loadFailed = ref(false)
const qrSrc = ref('')
const qrFailed = ref(false)
const holding = ref(false)

function setPayment(next: StudentPaymentDto) {
  payment.value = next
  emit('settled', next.settled)
}

async function load() {
  try {
    setPayment(await api.overview())
    loadFailed.value = false
  }
  catch {
    loadFailed.value = true
  }
}

function revokeQr() {
  if (qrSrc.value) URL.revokeObjectURL(qrSrc.value)
  qrSrc.value = ''
  qrFailed.value = false
}

async function loadQr() {
  revokeQr()
  try {
    qrSrc.value = URL.createObjectURL(await api.qr())
  }
  catch {
    qrFailed.value = true
  }
}

// the QR exists only while a session is open; fetch it when one opens and drop it when it closes
watch(() => payment.value?.state, (state: PaymentState | undefined) => {
  if (state === 'PENDING') loadQr()
  else revokeQr()
}, { immediate: true })
onBeforeUnmount(revokeQr)

async function start() {
  const next = await run(() => api.start())
  if (next) setPayment(next)
}

// the student started reporting the payment: stop the clock server side so a slow upload cannot expire it
async function hold() {
  holding.value = true
  try {
    setPayment(await api.hold())
  }
  catch {
    holding.value = false
  }
}

async function cancel() {
  const next = await run(() => api.cancel())
  if (next) setPayment(next)
}

async function submitProof(transactionNo: string, file: File) {
  const next = await run(() => api.submitProof(transactionNo, file))
  if (next) setPayment(next)
  else await load() // the window may have closed meanwhile: show the real state
}

onMounted(load)
const state = computed(() => payment.value?.state ?? 'NOT_STARTED')
const amount = computed(() => offeringMoney(payment.value?.amount, null, payment.value?.currency))
</script>

<template>
  <SectionCard v-if="!payment || payment.required || loadFailed" :title="copy('title')">
    <NSkeleton v-if="!payment && !loadFailed" class="h-24 w-full rounded-xl" />
    <NAlert v-else-if="loadFailed" tone="danger">
      {{ t('errors.loadSection') }}
      <button type="button" class="ms-2 font-medium underline" @click="load()">{{ t('common.retry') }}</button>
    </NAlert>

    <div v-else-if="payment" class="grid gap-4">
      <NAlert v-if="error" tone="danger">{{ error }}</NAlert>

      <!-- fee paid -->
      <div v-if="state === 'CONFIRMED'" class="flex items-start gap-3" data-test="state-confirmed">
        <span class="mt-0.5 shrink-0 text-brand-600"><DashboardIcon name="check" :size="22" /></span>
        <div>
          <p class="font-display text-lg font-semibold text-slate-900">{{ copy('confirmedTitle') }}</p>
          <p class="text-sm text-slate-600">{{ copy('confirmedBody') }}</p>
          <p class="mt-1 font-mono text-xs text-slate-500">{{ payment.reference }}</p>
        </div>
      </div>
      <PaymentDocuments v-if="state === 'CONFIRMED' && variant === 'application'" :application-id="applicationId" />

      <!-- reported, waiting for the Nadoumi team -->
      <div v-else-if="state === 'PROOF_SUBMITTED'" data-test="state-verifying">
        <p class="font-display text-lg font-semibold text-slate-900">{{ t('payment.verifyingTitle') }}</p>
        <p class="text-sm text-slate-600">{{ t('payment.verifyingBody') }}</p>
        <p class="mt-1 font-mono text-xs text-slate-500">{{ payment.reference }}</p>
      </div>

      <!-- QR session open -->
      <template v-else-if="state === 'PENDING'">
        <PaymentQrCard :payment="payment" :qr-src="qrSrc" :qr-failed="qrFailed" :holding="holding" @elapsed="load" @cancel="cancel" />
        <PaymentProofForm :busy="busy" @submit="submitProof" @started="hold" />
      </template>

      <!-- not paid yet, expired, cancelled or rejected: offer to pay -->
      <div v-else-if="isStartable(state)" class="grid gap-3" data-test="state-start">
        <NAlert v-if="state === 'REJECTED'" tone="danger" :title="t('payment.rejectedTitle')">
          {{ payment.rejectionReason }}
        </NAlert>
        <NAlert v-else-if="state === 'EXPIRED'" tone="warning" :title="t('payment.expiredTitle')">
          {{ t('payment.expiredBody') }}
        </NAlert>
        <p class="text-sm text-slate-600">{{ submitted && variant === 'application' ? t('payment.submittedIntro') : copy('intro') }}</p>
        <p class="text-sm">
          <span class="text-slate-500">{{ t('payment.amount') }}</span>
          <span class="ms-2 font-display text-xl font-bold tabular-nums text-red-600">{{ amount }}</span>
        </p>
        <div>
          <NButton :loading="busy" :disabled="!payment.canStart" data-test="pay" @click="start">
            {{ state === 'NOT_STARTED' ? t('payment.pay') : t('payment.payAgain') }}
          </NButton>
        </div>
      </div>
    </div>
  </SectionCard>
</template>
