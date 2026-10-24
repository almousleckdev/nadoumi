<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { fetchApplicationForm } from '@/api/application'

const props = defineProps<{ applicationId: number, reference: string | null }>()
const { t } = useI18n()

const url = ref('')
const blob = ref<Blob | null>(null)
const loading = ref(true)
const failed = ref(false)

onMounted(async () => {
  try {
    blob.value = await fetchApplicationForm(props.applicationId)
    url.value = URL.createObjectURL(blob.value)
  }
  catch {
    failed.value = true
  }
  finally {
    loading.value = false
  }
})
onBeforeUnmount(() => { if (url.value) URL.revokeObjectURL(url.value) })

function download() {
  if (!url.value) return
  const link = document.createElement('a')
  link.href = url.value
  link.download = `${props.reference ?? `application-${props.applicationId}`}-form.pdf`
  link.click()
}
</script>

<template>
  <div v-loading="loading">
    <p class="fp-help">
      {{ t('applications.formHelp') }}
    </p>
    <p
      v-if="failed"
      class="fp-help"
    >
      {{ t('applications.formFailed') }}
    </p>
    <template v-else-if="url">
      <el-button
        type="primary"
        class="fp-download"
        data-test="form-download"
        @click="download"
      >
        {{ t('applications.downloadForm') }}
      </el-button>
      <iframe
        :src="url"
        class="fp-frame"
        :title="t('applications.tabForm')"
      />
    </template>
  </div>
</template>

<style scoped>
.fp-help { margin: 0 0 12px; font-size: 13px; color: var(--nad-ink-soft, #64748b); }
.fp-download { margin-bottom: 12px; }
.fp-frame { display: block; width: 100%; height: 720px; border: 1px solid var(--nad-line, #e2e8f0); border-radius: 6px; }
</style>
