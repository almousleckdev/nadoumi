<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { fetchLetter, listLetters, uploadLetter, type Letter, type LetterKind } from '@/api/application'
import { formatDateTime } from '@/utils/date'

const props = defineProps<{ applicationId: number, canUpload: boolean }>()
const emit = defineEmits<{ changed: [] }>()
const { t } = useI18n()

const KINDS: LetterKind[] = ['ADMISSION_LETTER', 'JW202']
const VIEW_URL_LIFETIME_MS = 60_000

const letters = ref<Letter[]>([])
const loading = ref(true)
const busy = ref<LetterKind | null>(null)

const byKind = computed(() => new Map(letters.value.map(l => [l.kind, l])))

async function load() {
  loading.value = true
  try {
    letters.value = await listLetters(props.applicationId)
  }
  finally {
    loading.value = false
  }
}

async function onFile(kind: LetterKind, event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  busy.value = kind
  try {
    await uploadLetter(props.applicationId, kind, file)
    ElMessage.success(t('applications.letters.uploaded'))
    await load()
    emit('changed')
  }
  catch {
    // the shared request client already toasts the server's message
  }
  finally {
    busy.value = null
  }
}

async function view(kind: LetterKind) {
  try {
    const objectUrl = URL.createObjectURL(await fetchLetter(props.applicationId, kind))
    window.open(objectUrl, '_blank', 'noopener')
    setTimeout(() => URL.revokeObjectURL(objectUrl), VIEW_URL_LIFETIME_MS)
  }
  catch {
    ElMessage.error(t('applications.fileFailed'))
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <p class="lp-help">
      {{ t('applications.letters.help') }}
    </p>
    <ul class="lp-list">
      <li
        v-for="kind in KINDS"
        :key="kind"
        class="lp-row"
        :data-test="`letter-${kind}`"
      >
        <div>
          <strong>{{ t(`applications.letters.kind.${kind}`) }}</strong>
          <p class="lp-sub">
            <template v-if="byKind.get(kind)">
              {{ byKind.get(kind)!.fileName }} · {{ formatDateTime(byKind.get(kind)!.uploadedAt) }}
            </template>
            <template v-else>
              {{ t('applications.letters.missing') }}
            </template>
          </p>
        </div>
        <div class="lp-actions">
          <el-button
            v-if="byKind.get(kind)"
            size="small"
            @click="view(kind)"
          >
            {{ t('applications.view') }}
          </el-button>
          <label
            v-if="canUpload"
            class="lp-upload"
          >
            <input
              type="file"
              accept="application/pdf,image/jpeg,image/png"
              hidden
              :data-test="`upload-${kind}`"
              @change="onFile(kind, $event)"
            >
            <el-button
              size="small"
              type="primary"
              plain
              tag="span"
              :loading="busy === kind"
            >
              {{ byKind.get(kind) ? t('applications.letters.replace') : t('applications.letters.upload') }}
            </el-button>
          </label>
        </div>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.lp-help { margin: 0 0 14px; font-size: 13px; color: var(--nad-ink-soft, #64748b); }
.lp-list { list-style: none; margin: 0; padding: 0; display: grid; gap: 10px; }
.lp-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 12px 14px; border: 1px solid var(--nad-line, #e2e8f0); border-radius: 8px; flex-wrap: wrap; }
.lp-sub { margin: 2px 0 0; font-size: 12px; color: var(--nad-ink-soft, #64748b); }
.lp-actions { display: flex; gap: 8px; align-items: center; }
.lp-upload { cursor: pointer; }
</style>
