<template>
  <Drawer
    :model-value="modelValue"
    :title="t('payment.drawerTitle')"
    :size="560"
    @update:model-value="(v: boolean) => emit('update:modelValue', v)"
  >
    <div
      v-if="modelValue"
      v-loading="loading"
    >
      <template v-if="payment">
        <DescriptionList :items="items">
          <template #status>
            <StatusBadge
              :status="payment.status"
              :map="PAYMENT_TONES"
              :label="t(`payment.statusMap.${payment.status}`)"
            />
          </template>
        </DescriptionList>
        <h4 class="pd__h">
          {{ t('payment.proof') }}
        </h4>
        <PaymentProof
          :payment-id="payment.id"
          :has-proof="payment.hasProof"
        />
      </template>
    </div>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">
        {{ t('common.cancel') }}
      </el-button>
      <template v-if="canDecide">
        <el-button
          type="danger"
          plain
          :disabled="deciding"
          data-test="reject-open"
          @click="rejectOpen = true"
        >
          {{ t('payment.reject') }}
        </el-button>
        <el-button
          type="primary"
          :loading="deciding"
          data-test="confirm-open"
          @click="doConfirm"
        >
          {{ t('payment.confirm') }}
        </el-button>
      </template>
    </template>
    <PaymentRejectDialog
      v-model="rejectOpen"
      :busy="deciding"
      @confirm="doReject"
    />
  </Drawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import Drawer from '@/components/ui/Drawer.vue'
import DescriptionList from '@/components/ui/DescriptionList.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import PaymentProof from './PaymentProof.vue'
import PaymentRejectDialog from './PaymentRejectDialog.vue'
import { useConfirm } from '@/composables/useConfirm'
import { useUserStore } from '@/stores/user'
import { cny } from '@/utils/money'
import { formatDateTime } from '@/utils/date'
import {
  getPayment, confirmPayment, rejectPayment, type StaffPayment,
} from '@/api/payment'
import { PAYMENT_TONES } from './paymentTones'

const props = defineProps<{ modelValue: boolean, paymentId: number | null }>()
const emit = defineEmits<{ 'update:modelValue': [v: boolean], 'decided': [] }>()
const { t } = useI18n()
const { confirm } = useConfirm()
const userStore = useUserStore()

const payment = ref<StaffPayment | null>(null)
const loading = ref(false)
const deciding = ref(false)
const rejectOpen = ref(false)

const canDecide = computed(() =>
  payment.value?.status === 'PROOF_SUBMITTED' && userStore.hasPerm('nad:payment:verify'))

const items = computed(() => {
  const p = payment.value
  if (!p) return []
  return [
    { label: t('payment.reference'), value: p.reference },
    { label: t('payment.status'), slot: 'status' },
    { label: t('payment.student'), value: p.applicantName },
    { label: t('payment.application'), value: p.applicationRef },
    { label: t('payment.amount'), value: cny(p.amount) },
    { label: t('payment.txnNo'), value: p.studentTxnNo },
    { label: t('payment.submittedAt'), value: formatDateTime(p.submittedAt) },
    { label: t('payment.expiresAt'), value: formatDateTime(p.expiresAt) },
    { label: t('payment.verifiedAt'), value: formatDateTime(p.verifiedAt) },
    { label: t('payment.rejectionReason'), value: p.rejectionReason },
  ]
})

async function load() {
  if (props.paymentId == null) return
  loading.value = true
  try {
    payment.value = await getPayment(props.paymentId)
  }
  finally {
    loading.value = false
  }
}

watch(() => [props.modelValue, props.paymentId] as const, ([open]) => {
  if (open) load()
  else payment.value = null
}, { immediate: true })

function errorDetail(e: unknown): string {
  const err = e as { response?: { data?: { detail?: string } }, message?: string }
  return err.response?.data?.detail || err.message || t('state.errorTitle')
}

/** Runs one decision; a failure (409 already decided, 403 own payment) is shown and the row reloaded. */
async function decide(action: () => Promise<StaffPayment>, successKey: string) {
  deciding.value = true
  try {
    await action()
    ElMessage.success(t(successKey))
    rejectOpen.value = false
    emit('update:modelValue', false)
    emit('decided')
  }
  catch (e) {
    ElMessage.error(errorDetail(e))
    rejectOpen.value = false
    emit('decided')
    await load()
  }
  finally {
    deciding.value = false
  }
}

async function doConfirm() {
  const p = payment.value
  if (!p) return
  const ok = await confirm({
    title: t('payment.confirmTitle'),
    message: t('payment.confirmMessage', {
      amount: cny(p.amount), reference: p.reference, student: p.applicantName,
    }),
  })
  if (ok) await decide(() => confirmPayment(p.id), 'payment.confirmed')
}

function doReject(reason: string) {
  const p = payment.value
  if (p) decide(() => rejectPayment(p.id, reason), 'payment.rejected')
}
</script>

<style scoped>
.pd__h { margin: 20px 0 8px; font-size: 14px; }
</style>
