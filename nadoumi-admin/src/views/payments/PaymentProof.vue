<template>
  <div
    v-loading="loading"
    class="pp"
  >
    <p
      v-if="!hasProof"
      class="pp__note"
    >
      {{ t('payment.noProof') }}
    </p>
    <p
      v-else-if="failed"
      class="pp__note"
    >
      {{ t('payment.proofFailed') }}
    </p>
    <img
      v-else-if="imageUrl"
      :src="imageUrl"
      :alt="t('payment.proof')"
      class="pp__img"
      data-test="proof-image"
    >
    <a
      v-else-if="fileUrl"
      :href="fileUrl"
      target="_blank"
      rel="noopener"
      data-test="proof-file"
    >
      {{ t('payment.openFile') }}
    </a>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { fetchPaymentProof } from '@/api/payment'

const props = defineProps<{ paymentId: number | null, hasProof: boolean }>()
const { t } = useI18n()

const loading = ref(false)
const failed = ref(false)
const imageUrl = ref('')
const fileUrl = ref('')

function release() {
  if (imageUrl.value) URL.revokeObjectURL(imageUrl.value)
  if (fileUrl.value) URL.revokeObjectURL(fileUrl.value)
  imageUrl.value = ''
  fileUrl.value = ''
}

async function load() {
  release()
  failed.value = false
  if (props.paymentId == null || !props.hasProof) return
  const id = props.paymentId
  loading.value = true
  try {
    const blob = await fetchPaymentProof(id)
    if (props.paymentId !== id) return
    const url = URL.createObjectURL(blob)
    if (blob.type.startsWith('image/')) imageUrl.value = url
    else fileUrl.value = url
  }
  catch {
    failed.value = true
  }
  finally {
    loading.value = false
  }
}

watch(() => [props.paymentId, props.hasProof] as const, load, { immediate: true })
onBeforeUnmount(release)
</script>

<style scoped>
.pp { min-height: 40px; }
.pp__img { max-width: 100%; border: 1px solid var(--nad-line); border-radius: 6px; }
.pp__note { margin: 0; color: var(--nad-ink-faint); font-size: 14px; }
</style>
