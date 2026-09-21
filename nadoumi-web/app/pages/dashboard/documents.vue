<script setup lang="ts">
import type { ApplicantDto } from '~/types/catalog'
import type { DocumentTypeOption, StudentDocumentDto } from '~/types/documents'
import { saveBlob } from '~/utils/documents'
definePageMeta({ layout: 'dashboard', middleware: ['auth', 'onboarding'] })

const { t } = useI18n()
const localePath = useLocalePath()
const { activeApplicantId } = useSession()
const { listMine, get } = useApplicant()
const documents = useDocuments()
const { busy, notice, error, run } = useAsyncAction()

const applicant = ref<ApplicantDto | null>(null)
const pending = ref(true)

async function load() {
  pending.value = true
  const mine = await listMine().catch(() => [])
  const chosen = mine.find(a => a.id === activeApplicantId.value) ?? mine[0] ?? null
  applicant.value = chosen ? await get(chosen.id) : null
  pending.value = false
}
await load()

const docs = ref<StudentDocumentDto[]>([])
const docsPending = ref(true)
const docsError = ref('')
const typeOptions = ref<DocumentTypeOption[]>([])
const typesUnavailable = ref(false)

const typeLabels = computed(() => new Map(typeOptions.value.map((o: DocumentTypeOption) => [o.value, o.label])))
const labelFor = (code: string) => typeLabels.value.get(code) ?? code

async function loadDocuments() {
  const current = applicant.value
  if (!current) {
    docsPending.value = false
    return
  }
  docsPending.value = true
  docsError.value = ''
  try {
    docs.value = await documents.list(current.id)
  }
  catch {
    docsError.value = t('errors.loadSection')
  }
  finally {
    docsPending.value = false
  }
}

async function loadTypes() {
  try {
    typeOptions.value = await documents.types()
    typesUnavailable.value = typeOptions.value.length === 0
  }
  catch {
    typesUnavailable.value = true
  }
}

// Client side only: these calls need the student's session cookie, which a server render does not forward.
onMounted(() => Promise.all([loadDocuments(), loadTypes()]))

async function onCreate(docType: string, file: File) {
  const current = applicant.value
  if (!current) return
  if (await run(() => documents.create(current.id, docType, file), t('dashboard.docs.added'))) await loadDocuments()
}

async function onReplace(doc: StudentDocumentDto, file: File) {
  if (await run(() => documents.replace(doc.id, file), t('dashboard.docs.replaced'))) await loadDocuments()
}

async function onRemove(doc: StudentDocumentDto) {
  const done = await run(async () => {
    await documents.remove(doc.id)
    return true
  }, t('dashboard.docs.removed'))
  if (done) await loadDocuments()
}

async function onDownload(doc: StudentDocumentDto) {
  const version = doc.currentVersion
  if (!version) return
  await run(async () => {
    const access = await documents.fileAccess(doc.id, version.versionNo)
    if (access.kind === 'url') window.open(access.url, '_blank', 'noopener')
    else saveBlob(access.blob, access.filename)
  })
}

useSeo(t('dashboard.documentsTitle'), t('dashboard.documentsBlurb'))
</script>

<template>
  <div class="grid gap-6">
    <header>
      <h1 class="font-display text-xl font-bold text-slate-900">{{ t('dashboard.documentsTitle') }}</h1>
      <p class="mt-1 text-sm text-slate-500">{{ t('dashboard.documentsBlurb') }}</p>
    </header>

    <AsyncState :pending="pending">
      <template #loading>
        <div class="grid gap-4">
          <NSkeleton class="h-32 w-full rounded-lg" />
          <NSkeleton class="h-40 w-full rounded-lg" />
        </div>
      </template>

      <NAlert v-if="!applicant" tone="warning">
        {{ t('dashboard.createProfileBlurb') }}
        <NuxtLink :to="localePath('/dashboard/profile')" class="ms-1 underline">{{ t('dashboard.quickProfile') }}</NuxtLink>
      </NAlert>

      <div v-else class="grid gap-8">
        <section class="grid gap-4">
          <div>
            <h2 class="font-display text-lg font-semibold text-slate-900">{{ t('dashboard.docs.identityTitle') }}</h2>
            <p class="mt-1 text-sm text-slate-500">{{ t('dashboard.docs.identityBlurb') }}</p>
          </div>
          <PhotoUploadCard :applicant-id="applicant.id" @changed="load" />
          <PassportUploadCard
            :applicant-id="applicant.id"
            :profile="{ givenName: applicant.givenName, familyName: applicant.familyName, dob: applicant.dob }"
            @changed="load"
            @edit-profile="navigateTo(localePath('/dashboard/profile'))"
          />
        </section>

        <section class="grid gap-4">
          <div>
            <h2 class="font-display text-lg font-semibold text-slate-900">{{ t('dashboard.docs.listTitle') }}</h2>
            <p class="mt-1 text-sm text-slate-500">{{ t('dashboard.docs.listBlurb') }}</p>
          </div>

          <NAlert v-if="notice" tone="success">{{ notice }}</NAlert>
          <NAlert v-if="error" tone="danger">{{ error }}</NAlert>

          <AsyncState :pending="docsPending" :error="docsError" :empty="!docs.length">
            <template #loading>
              <div class="grid gap-3">
                <NSkeleton class="h-28 w-full rounded-lg" />
                <NSkeleton class="h-28 w-full rounded-lg" />
              </div>
            </template>
            <template #error>
              <NAlert tone="danger">
                {{ docsError }}
                <button type="button" class="ms-2 font-medium underline" @click="loadDocuments">{{ t('common.retry') }}</button>
              </NAlert>
            </template>
            <template #empty>
              <p class="rounded-lg border border-dashed border-slate-300 p-6 text-center text-sm text-slate-500" data-test="docs-empty">
                {{ t('dashboard.docs.empty') }}
              </p>
            </template>

            <ul class="grid gap-3">
              <DocumentRow
                v-for="doc in docs"
                :key="doc.id"
                :doc="doc"
                :type-label="labelFor(doc.docType)"
                :busy="busy"
                @download="onDownload(doc)"
                @replace="onReplace(doc, $event)"
                @remove="onRemove(doc)"
              />
            </ul>
          </AsyncState>

          <div class="rounded-lg border border-slate-200 bg-white p-4 sm:p-5">
            <h3 class="mb-3 font-display font-semibold text-slate-900">{{ t('dashboard.docs.add') }}</h3>
            <NAlert v-if="typesUnavailable" tone="warning" data-test="types-unavailable">{{ t('dashboard.docs.typesUnavailable') }}</NAlert>
            <AddDocumentForm v-else :types="typeOptions" :busy="busy" @submit="onCreate" />
          </div>
        </section>
      </div>
    </AsyncState>
  </div>
</template>
