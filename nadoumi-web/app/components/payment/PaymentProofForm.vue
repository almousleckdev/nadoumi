<script setup lang="ts">
import { PAYMENT_PROOF_ACCEPT, PAYMENT_PROOF_MAX_MB, PAYMENT_PROOF_MIME, TRANSACTION_NO_PATTERN } from '~/constants/payment'
import { readAsDataUrl } from '~/utils/files'

/** Reports the payment: the transaction number from the receipt and a screenshot. */
defineProps<{ busy: boolean }>()
const emit = defineEmits<{ submit: [transactionNo: string, file: File], started: [] }>()

const { t } = useI18n()

const transactionNo = ref('')
const file = ref<File | null>(null)
const preview = ref('')
const errors = reactive({ transactionNo: '', file: '' })

// the student has paid and is reporting it: tell the parent once so the payment clock can stop
let announced = false
function announceStarted() {
  if (announced) return
  announced = true
  emit('started')
}
watch(transactionNo, (value: string) => value.trim() && announceStarted())

async function onSelect(picked: File) {
  file.value = picked
  announceStarted()
  errors.file = ''
  preview.value = picked.type === 'application/pdf' ? '' : await readAsDataUrl(picked)
}

function submit() {
  errors.transactionNo = TRANSACTION_NO_PATTERN.test(transactionNo.value.trim()) ? '' : t('payment.errors.txnInvalid')
  errors.file = file.value ? '' : t('payment.errors.screenshotRequired')
  if (errors.transactionNo || errors.file || !file.value) return
  emit('submit', transactionNo.value.trim(), file.value)
}
</script>

<template>
  <form class="grid gap-4 rounded-xl border border-slate-200 p-4" novalidate @submit.prevent="submit">
    <FormTextField
      id="payment-txn"
      v-model="transactionNo"
      :label="t('payment.transactionNo')"
      :hint="t('payment.transactionNoHint')"
      :error="errors.transactionNo"
      :maxlength="40"
      uppercase
      required
    />
    <div class="grid gap-1.5">
      <p class="text-sm font-medium text-slate-700">{{ t('payment.screenshot') }} <span class="text-red-600" aria-hidden="true">*</span></p>
      <FilePicker
        shape="document"
        :accept="PAYMENT_PROOF_ACCEPT"
        :mime="PAYMENT_PROOF_MIME"
        :max-mb="PAYMENT_PROOF_MAX_MB"
        :bad-type-message="t('payment.errors.fileType')"
        :src="preview"
        :pdf="file?.type === 'application/pdf'"
        :label="file ? t('onboarding.upload.replace') : t('onboarding.upload.choose')"
        :hint="file ? file.name : t('payment.fileHint', { mb: PAYMENT_PROOF_MAX_MB })"
        @select="onSelect"
      />
      <p v-if="errors.file" class="text-sm text-red-600" role="alert">{{ errors.file }}</p>
    </div>
    <div>
      <NButton type="submit" :loading="busy" data-test="i-have-paid">{{ t('payment.iHavePaid') }}</NButton>
    </div>
  </form>
</template>
