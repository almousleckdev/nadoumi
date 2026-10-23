<script setup lang="ts">
import type { StudentPaymentDto } from '~/types/payment'
import { URGENT_SECONDS, formatCountdown } from '~/utils/paymentView'

/** The open payment session: the QR, the time left, the exact amount and the reference to write in the note. */
const props = defineProps<{ payment: StudentPaymentDto, qrSrc: string, qrFailed: boolean }>()
const emit = defineEmits<{ elapsed: [], cancel: [] }>()

const { t } = useI18n()
const toast = useToast()

const secondsLeft = computed(() => props.payment.secondsLeft)
const { remaining } = usePaymentCountdown(secondsLeft, () => emit('elapsed'))
const urgent = computed(() => remaining.value <= URGENT_SECONDS)
const amount = computed(() => offeringMoney(props.payment.amount, null, props.payment.currency))

async function copyReference() {
  if (!props.payment.reference) return
  try {
    await navigator.clipboard.writeText(props.payment.reference)
    toast.success(t('payment.copied'))
  }
  catch {
    /* the reference is also shown as text; copying is a convenience */
  }
}
</script>

<template>
  <div class="grid gap-5 sm:grid-cols-[15rem_minmax(0,1fr)] sm:items-start">
    <figure class="mx-auto w-full max-w-[15rem]">
      <img v-if="qrSrc" :src="qrSrc" :alt="t('payment.qrAlt')" class="w-full rounded-xl border border-slate-200 bg-white">
      <p v-else-if="qrFailed" class="rounded-xl border border-dashed border-slate-300 p-6 text-center text-sm text-slate-500" role="alert">
        {{ t('payment.errors.qrFailed') }}
      </p>
      <NSkeleton v-else class="aspect-[908/1280] w-full rounded-xl" />
    </figure>

    <div class="grid gap-4">
      <div>
        <p class="text-xs font-medium uppercase tracking-wide text-slate-500">{{ t('payment.timeLeft') }}</p>
        <p
          class="font-display text-3xl font-bold tabular-nums text-slate-900"
          :class="urgent ? 'animate-pulse' : ''"
          role="timer"
          :aria-label="t('payment.timeLeft')"
          data-test="countdown"
        >
          {{ formatCountdown(remaining) }}
        </p>
      </div>

      <dl class="grid gap-3 text-sm">
        <div>
          <dt class="text-xs font-medium uppercase tracking-wide text-slate-500">{{ t('payment.amount') }}</dt>
          <dd class="font-display text-xl font-bold tabular-nums text-red-600" data-test="amount">{{ amount }}</dd>
        </div>
        <div>
          <dt class="text-xs font-medium uppercase tracking-wide text-slate-500">{{ t('payment.payee') }}</dt>
          <dd class="break-words font-medium text-slate-900">{{ payment.payee }}</dd>
        </div>
        <div>
          <dt class="text-xs font-medium uppercase tracking-wide text-slate-500">{{ t('payment.reference') }}</dt>
          <dd class="flex flex-wrap items-center gap-2">
            <code class="rounded bg-slate-100 px-2 py-1 font-mono text-sm text-slate-900" data-test="reference">{{ payment.reference }}</code>
            <button type="button" class="text-sm font-medium text-slate-900 underline underline-offset-2 hover:text-slate-600" @click="copyReference">
              {{ t('payment.copy') }}
            </button>
          </dd>
          <dd class="mt-1 text-xs text-slate-500">{{ t('payment.referenceHint') }}</dd>
        </div>
      </dl>

      <ol class="grid list-decimal gap-1.5 ps-5 text-sm text-slate-700">
        <li>{{ t('payment.step1') }}</li>
        <li>{{ t('payment.step2', { amount }) }}</li>
        <li>{{ t('payment.step3') }}</li>
        <li>{{ t('payment.step4') }}</li>
      </ol>

      <div>
        <NButton variant="ghost" size="sm" @click="emit('cancel')">{{ t('payment.cancel') }}</NButton>
      </div>
    </div>
  </div>
</template>
