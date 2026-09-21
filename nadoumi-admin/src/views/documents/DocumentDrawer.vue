<template>
  <el-drawer
    :model-value="modelValue"
    :title="t('documents.detailTitle')"
    size="560"
    @update:model-value="(v: boolean) => emit('update:modelValue', v)"
  >
    <template v-if="doc">
      <div class="doc__head">
        <h3 class="doc__type">
          {{ typeLabel }}
        </h3>
        <StatusBadge
          :status="doc.status"
          :label="t(`documents.statusMap.${doc.status}`, doc.status)"
        />
      </div>

      <DescriptionList :items="facts" />

      <el-alert
        v-if="doc.status === 'REJECTED' && doc.rejectionReason"
        class="doc__reason"
        type="error"
        :closable="false"
        show-icon
        :title="t('documents.rejectionReason')"
        :description="doc.rejectionReason"
      />

      <div
        v-if="canVerify || canReject"
        class="doc__actions"
      >
        <el-button
          v-if="canVerify"
          type="success"
          :loading="busy"
          data-test="verify"
          @click="onVerify"
        >
          {{ t('documents.verify') }}
        </el-button>
        <el-button
          v-if="canReject"
          type="danger"
          plain
          :disabled="busy"
          data-test="reject"
          @click="rejectOpen = true"
        >
          {{ t('documents.reject') }}
        </el-button>
      </div>

      <h4 class="doc__h4">
        {{ t('documents.versions') }}
      </h4>
      <p
        v-if="!doc.versions.length"
        class="doc__empty"
      >
        {{ t('documents.noVersions') }}
      </p>
      <ul
        v-else
        class="doc__versions"
      >
        <li
          v-for="v in doc.versions"
          :key="v.id"
          class="doc__version"
          data-test="version"
        >
          <div class="doc__version-main">
            <b>v{{ v.versionNo }}</b>
            <span>{{ v.contentType }} · {{ formatSize(v.sizeBytes) }}</span>
            <span class="doc__version-when">{{ t('documents.uploaded') }} {{ v.uploadedAt }}</span>
          </div>
          <StatusBadge
            :status="v.verificationStatus"
            :label="t(`documents.verificationMap.${v.verificationStatus}`, v.verificationStatus)"
          />
          <el-button
            v-if="canDownload"
            size="small"
            text
            type="primary"
            :loading="downloading === v.versionNo"
            data-test="download"
            @click="onDownload(v.versionNo)"
          >
            {{ t('documents.download') }}
          </el-button>
        </li>
      </ul>

      <h4 class="doc__h4">
        {{ t('documents.history') }}
      </h4>
      <el-timeline v-if="doc.events.length">
        <el-timeline-item
          v-for="ev in doc.events"
          :key="ev.id"
          :timestamp="ev.at"
          placement="top"
        >
          <b>{{ t(`documents.eventMap.${ev.eventType}`, ev.eventType) }}</b>
          <span class="doc__actor">#{{ ev.actorUserId }}</span>
          <span
            v-if="ev.detail"
            class="doc__note"
          >{{ ev.detail }}</span>
        </el-timeline-item>
      </el-timeline>
      <p
        v-else
        class="doc__empty"
      >
        {{ t('documents.noHistory') }}
      </p>
    </template>

    <el-dialog
      v-model="rejectOpen"
      :title="t('documents.rejectTitle')"
      width="440px"
      append-to-body
    >
      <p class="doc__hint">
        {{ t('documents.rejectHint') }}
      </p>
      <el-input
        v-model="reason"
        type="textarea"
        :rows="4"
        maxlength="500"
        show-word-limit
        :placeholder="t('documents.reasonPlaceholder')"
        data-test="reason"
      />
      <p
        v-if="reasonError"
        class="doc__error"
      >
        {{ reasonError }}
      </p>
      <template #footer>
        <el-button @click="rejectOpen = false">
          {{ t('common.cancel') }}
        </el-button>
        <el-button
          type="danger"
          :loading="busy"
          data-test="confirm-reject"
          @click="onReject"
        >
          {{ t('documents.reject') }}
        </el-button>
      </template>
    </el-dialog>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import DescriptionList from '@/components/ui/DescriptionList.vue'
import { rejectDocument, verifyDocument, type StaffDocument } from '@/api/document'
import { openDocumentFile } from '@/utils/documentFile'
import { useUserStore } from '@/stores/user'

const props = defineProps<{ modelValue: boolean, doc: StaffDocument | null, typeLabel: string }>()
const emit = defineEmits<{ 'update:modelValue': [v: boolean], 'changed': [] }>()

const { t } = useI18n()
const userStore = useUserStore()

const busy = ref(false)
const downloading = ref<number | null>(null)
const rejectOpen = ref(false)
const reason = ref('')
const reasonError = ref('')

const KB = 1024
const MB = KB * 1024
const formatSize = (bytes: number) => (bytes >= MB ? `${(bytes / MB).toFixed(1)} MB` : `${Math.max(1, Math.round(bytes / KB))} KB`)

// A verdict needs an uploaded file to judge; the backend enforces the same rule.
const reviewable = computed(() => Boolean(props.doc?.versions.length))
const canVerify = computed(() => reviewable.value && props.doc?.status !== 'VERIFIED' && userStore.hasPerm('nad:document:verify'))
const canReject = computed(() => reviewable.value && props.doc?.status !== 'REJECTED' && userStore.hasPerm('nad:document:reject'))
const canDownload = computed(() => userStore.hasPerm('nad:document:download'))

const facts = computed(() => {
  const d = props.doc
  if (!d) return []
  return [
    { label: t('documents.applicant'), value: `#${d.applicantId}` },
    { label: t('documents.application'), value: d.applicationId ? `#${d.applicationId}` : '' },
    { label: t('documents.expiresOn'), value: d.expiresOn ?? '' },
    { label: t('documents.reviewer'), value: d.reviewerUserId ? `#${d.reviewerUserId}` : '' },
  ]
})

watch(() => props.modelValue, (open) => {
  if (!open) {
    rejectOpen.value = false
    reason.value = ''
    reasonError.value = ''
  }
})

async function onVerify() {
  if (!props.doc) return
  busy.value = true
  try {
    await verifyDocument(props.doc.id)
    ElMessage.success(t('documents.verified'))
    emit('changed')
  }
  finally {
    busy.value = false
  }
}

async function onReject() {
  if (!props.doc) return
  const text = reason.value.trim()
  if (!text) {
    reasonError.value = t('common.required')
    return
  }
  reasonError.value = ''
  busy.value = true
  try {
    await rejectDocument(props.doc.id, text)
    ElMessage.success(t('documents.rejected'))
    rejectOpen.value = false
    reason.value = ''
    emit('changed')
  }
  finally {
    busy.value = false
  }
}

async function onDownload(versionNo: number) {
  if (!props.doc) return
  downloading.value = versionNo
  try {
    await openDocumentFile(props.doc.id, versionNo)
  }
  finally {
    downloading.value = null
  }
}
</script>

<style scoped>
.doc__head { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.doc__type { margin: 0; font-size: 16px; }
.doc__reason { margin: 14px 0 0; }
.doc__actions { display: flex; gap: 8px; margin: 16px 0 4px; }
.doc__h4 { margin: 20px 0 8px; font-size: 13px; }
.doc__versions { list-style: none; margin: 0; padding: 0; display: grid; gap: 8px; }
.doc__version {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid var(--nad-line, #e5e7eb);
  border-radius: 8px;
}
.doc__version-main { display: flex; flex-direction: column; gap: 2px; flex: 1; font-size: 13px; }
.doc__version-when { font-size: 12px; color: var(--nad-ink-soft, #64748b); }
.doc__empty { color: var(--nad-ink-soft, #64748b); font-size: 13px; }
.doc__actor { margin-left: 6px; font-size: 12px; color: var(--nad-ink-soft, #64748b); }
.doc__note { display: block; font-size: 12px; color: var(--nad-ink-faint, #9ca3af); white-space: pre-wrap; }
.doc__hint { margin: 0 0 10px; font-size: 13px; color: var(--nad-ink-soft, #64748b); }
.doc__error { margin: 6px 0 0; font-size: 12px; color: var(--nad-danger, #dc2626); }
</style>
