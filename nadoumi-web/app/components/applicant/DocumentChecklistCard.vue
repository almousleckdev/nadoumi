<script setup lang="ts">
import type { ChecklistItemDto } from '~/types/checklist'
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

const items = ref<ChecklistItemDto[]>([])
const loaded = ref(false)

async function load() {
  items.value = await checklist(props.applicationId).catch(() => [])
  loaded.value = true
  emit('ready', allMandatoryAttached(items.value))
}

async function upload(item: ChecklistItemDto, file: File) {
  const done = await run(async () => {
    if (replacesExisting(item) && item.documentId !== null) await documents.replace(item.documentId, file)
    else await documents.create(props.applicantId, item.docType, file, props.applicationId)
    return true
  }, t('checklist.uploaded'))
  if (done) await load()
}

onMounted(load)
</script>

<template>
  <SectionCard v-if="items.length" :title="t('checklist.title')" data-test="checklist">
    <p class="mb-3 text-sm text-slate-600">{{ t('checklist.blurb') }}</p>
    <NAlert v-if="error" tone="danger" class="mb-3">{{ error }}</NAlert>
    <ul class="divide-y divide-slate-100">
      <ChecklistRow v-for="item in items" :key="item.docType" :item="item" :busy="busy" @upload="upload(item, $event)" />
    </ul>
  </SectionCard>
</template>
