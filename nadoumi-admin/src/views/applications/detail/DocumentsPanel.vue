<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { listDocumentTypes, listStaffDocuments, type StaffDocument } from '@/api/document'
import { openDocumentFile } from '@/utils/documentFile'
import { formatDateTime } from '@/utils/date'
import StatusBadge from '@/components/ui/StatusBadge.vue'

const props = defineProps<{ applicantId: number }>()
const { t, te } = useI18n()

const docs = ref<StaffDocument[]>([])
const typeLabels = ref(new Map<string, string>())
const loading = ref(true)
const failed = ref(false)

const latest = (d: StaffDocument) => d.versions.reduce<StaffDocument['versions'][number] | null>(
  (best, v) => (best === null || v.versionNo > best.versionNo ? v : best), null)

const rows = computed(() => docs.value.map(d => ({ doc: d, version: latest(d) })))

onMounted(async () => {
  try {
    const [list, types] = await Promise.all([listStaffDocuments({ applicantId: props.applicantId }), listDocumentTypes()])
    docs.value = list
    typeLabels.value = new Map(types.data.map(e => [e.dictValue, e.dictLabel]))
  }
  catch {
    failed.value = true
  }
  finally {
    loading.value = false
  }
})

const label = (code: string) => typeLabels.value.get(code) ?? code

async function view(d: StaffDocument, versionNo: number, contentType: string) {
  try {
    await openDocumentFile(d.id, versionNo, 'view', contentType)
  }
  catch {
    ElMessage.error(t('applications.fileFailed'))
  }
}

async function download(d: StaffDocument, versionNo: number, contentType: string) {
  try {
    await openDocumentFile(d.id, versionNo, 'download', contentType)
  }
  catch {
    ElMessage.error(t('applications.fileFailed'))
  }
}

const statusText = (status: string) => (te(`documents.statusMap.${status}`) ? t(`documents.statusMap.${status}`) : status)
</script>

<template>
  <div v-loading="loading">
    <p
      v-if="failed"
      class="dp-note"
    >
      {{ t('applications.documentsFailed') }}
    </p>
    <p
      v-else-if="!loading && !rows.length"
      class="dp-note"
    >
      {{ t('applications.noDocuments') }}
    </p>
    <el-table
      v-else
      :data="rows"
      size="small"
    >
      <el-table-column
        :label="t('documents.type')"
        min-width="180"
      >
        <template #default="{ row }">
          {{ label(row.doc.docType) }}
        </template>
      </el-table-column>
      <el-table-column
        :label="t('documents.status')"
        width="130"
      >
        <template #default="{ row }">
          <StatusBadge
            :status="row.doc.status"
            :label="statusText(row.doc.status)"
          />
        </template>
      </el-table-column>
      <el-table-column
        :label="t('documents.uploaded')"
        min-width="150"
      >
        <template #default="{ row }">
          {{ row.version ? formatDateTime(row.version.uploadedAt) : '' }}
        </template>
      </el-table-column>
      <el-table-column
        width="190"
        align="right"
      >
        <template #default="{ row }">
          <template v-if="row.version">
            <el-button
              size="small"
              data-test="doc-view"
              @click="view(row.doc, row.version.versionNo, row.version.contentType)"
            >
              {{ t('applications.view') }}
            </el-button>
            <el-button
              size="small"
              type="primary"
              plain
              data-test="doc-download"
              @click="download(row.doc, row.version.versionNo, row.version.contentType)"
            >
              {{ t('documents.download') }}
            </el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.dp-note { margin: 0; font-size: 13px; color: var(--nad-ink-soft, #64748b); }
</style>
