<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { listPayments, type StaffPayment } from '@/api/payment'
import { useUserStore } from '@/stores/user'
import { formatDateTime } from '@/utils/date'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import DescriptionList from '@/components/ui/DescriptionList.vue'
import PaymentProof from '@/views/payments/PaymentProof.vue'

const MAX_PAYMENTS = 20

const props = defineProps<{ applicationId: number }>()
const { t, te } = useI18n()
const router = useRouter()
const userStore = useUserStore()

const payments = ref<StaffPayment[]>([])
const loading = ref(true)
const allowed = userStore.hasPerm('nad:payment:list')

onMounted(async () => {
  try {
    if (allowed) {
      const page = await listPayments({ applicationId: props.applicationId, size: MAX_PAYMENTS })
      payments.value = page.content
    }
  }
  finally {
    loading.value = false
  }
})

const money = (p: StaffPayment) => `${p.currency === 'CNY' ? '¥' : p.currency} ${p.amount}`
const kindText = (k: string) => (te(`payment.kindMap.${k}`) ? t(`payment.kindMap.${k}`) : k)
const statusText = (s: string) => (te(`payment.statusMap.${s}`) ? t(`payment.statusMap.${s}`) : s)
</script>

<template>
  <div v-loading="loading">
    <p
      v-if="!allowed"
      class="pp-note"
    >
      {{ t('applications.noPaymentAccess') }}
    </p>
    <p
      v-else-if="!loading && !payments.length"
      class="pp-note"
    >
      {{ t('applications.noPayment') }}
    </p>
    <template v-else>
      <section
        v-for="payment in payments"
        :key="payment.id"
        class="pp-item"
      >
        <h4 class="pp-h pp-kind">
          {{ kindText(payment.kind) }}
        </h4>
        <DescriptionList
          :items="[
            { label: t('payment.status'), value: '', slot: 'status' },
            { label: t('payment.amount'), value: money(payment) },
            { label: t('payment.reference'), value: payment.reference },
            { label: t('payment.txnNo'), value: payment.studentTxnNo },
            { label: t('payment.submittedAt'), value: formatDateTime(payment.submittedAt) },
            { label: t('payment.verifiedAt'), value: formatDateTime(payment.verifiedAt) },
          ]"
        >
          <template #status>
            <StatusBadge
              :status="payment.status"
              :label="statusText(payment.status)"
            />
          </template>
        </DescriptionList>
        <h4 class="pp-h">
          {{ t('payment.proof') }}
        </h4>
        <PaymentProof
          :payment-id="payment.id"
          :has-proof="payment.hasProof"
        />
        <el-button
          v-if="payment.status === 'PROOF_SUBMITTED'"
          type="primary"
          class="pp-go"
          @click="router.push('/payments')"
        >
          {{ t('applications.verifyInPayments') }}
        </el-button>
      </section>
    </template>
  </div>
</template>

<style scoped>
.pp-note { margin: 0 0 12px; font-size: 13px; color: var(--nad-ink-soft, #64748b); }
.pp-h { margin: 18px 0 8px; font-size: 14px; }
.pp-item + .pp-item { margin-top: 24px; padding-top: 16px; border-top: 1px solid var(--nad-border, #e2e8f0); }
.pp-kind { margin-top: 0; }
.pp-go { margin-top: 16px; }
</style>
