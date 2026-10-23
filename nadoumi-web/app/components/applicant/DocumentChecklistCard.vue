<script setup lang="ts">
import type { ChecklistItemDto } from '~/types/checklist'
import type { StudentDocumentDto } from '~/types/documents'
import { allMandatoryAttached, replacesExisting } from '~/utils/checklistView'

/**
 * The documents this application needs, set by the programme or scholarship, with an upload per row. It tells its
 * parent whether every mandatory document is in, so Submit can follow. Nothing is shown when none are required.
 */
const props = defineProps<{ applicationId: number, applicantId: number }>()
const emit = defineEmits<{ ready: [value: boolean] }>()

const { t } = useI18n()
const { checklist } = useApplications()
const documents = useDocuments()
const { busy, error, run } = useAsyncAction()
const { file: previewFile, open: previewOpen, show: showPreview } = useDocumentPreview()
const forms = useTemplateDownload(() => props.applicationId)

const items = ref<ChecklistItemDto[]>([])
const stored = ref(new Map<number, StudentDocumentDto>())
const loaded = ref(false)

async function load() {
  items.value = await checklist(props.applicationId).catch(() => [])
  const mine = await documents.list(props.applicantId, props.applicationId).catch(() => [])
  stored.value = new Map(mine.map((d: StudentDocumentDto) => [d.id, d]))
  loaded.value = true
  emit('ready', allMandatoryAttached(items.value))
}

const versionOf = (item: ChecklistItemDto) => (item.documentId === null ? undefined : stored.value.get(item.documentId)?.currentVersion ?? undefined)

async function view(item: ChecklistItemDto) {
  const version = versionOf(item)
  if (item.documentId === null || !version) return
  await run(() => showPreview(t(`scholarships.doc.${item.docType}`, item.docType), item.documentId!, version))
}

async function upload(item: ChecklistItemDto, file: File) {
  const done = await run(async () => {
    if (replacesExisting(item) && item.documentId !== null) await documents.replace(item.documentId, file)
    else await documents.create(props.applicantId, item.docType, file, props.applicationId)
    return true
  }, t('checklist.uploaded'))
  if (done) await load()
}

onMounted(() => Promise.all([load(), forms.load()]))
</script>

<template>
  <SectionCard v-if="items.length" :title="t('checklist.title')" data-test="checklist">
    <p class="mb-3 text-sm text-slate-600">{{ t('checklist.blurb') }}</p>
    <NAlert v-if="error" tone="danger" class="mb-3">{{ error }}</NAlert>
    <ul class="divide-y divide-slate-100">
      <ChecklistRow
        v-for="item in items"
        :key="item.docType"
        :item="item"
        :busy="busy"
        :content-type="versionOf(item)?.contentType"
        :forms="forms.items.value"
        :downloading-form="forms.downloading.value"
        @upload="upload(item, $event)"
        @view="view(item)"
        @download-form="forms.download($event)"
      />
    </ul>
    <DocumentViewer v-model="previewOpen" :file="previewFile" />
  </SectionCard>
</template>
