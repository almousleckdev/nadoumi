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
const { busy, error, run } = useAsyncAction()
const showAdd = ref(false)

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
  if (await run(() => documents.create(current.id, docType, file), t('dashboard.docs.added'))) {
    showAdd.value = false
    await loadDocuments()
  }
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
  <div class="mx-auto grid max-w-5xl gap-8">
    <header>
      <h1 class="font-display text-2xl font-bold tracking-tight text-slate-900">{{ t('dashboard.documentsTitle') }}</h1>
      <p class="mt-1 text-sm text-slate-600">{{ t('dashboard.documentsBlurb') }}</p>
    </header>

    <AsyncState :pending="pending">
      <template #loading>
        <div class="grid gap-4 sm:grid-cols-2">
          <NSkeleton class="h-56 w-full rounded-2xl" />
          <NSkeleton class="h-56 w-full rounded-2xl" />
        </div>
      </template>

      <NAlert v-if="!applicant" tone="warning">
        {{ t('dashboard.createProfileBlurb') }}
        <NuxtLink :to="localePath('/dashboard/profile')" class="ms-1 underline">{{ t('dashboard.quickProfile') }}</NuxtLink>
      </NAlert>

      <div v-else class="grid gap-10">
        <NAlert v-if="error" tone="danger">{{ error }}</NAlert>

        <section :aria-label="t('dashboard.docs.identityTitle')">
          <h2 class="font-display text-lg font-semibold text-slate-900">{{ t('dashboard.docs.identityTitle') }}</h2>
          <p class="mt-1 text-sm text-slate-600">{{ t('dashboard.docs.identityBlurb') }}</p>
          <div class="mt-4 grid gap-4 lg:grid-cols-2">
            <div class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <PhotoUploadCard :applicant-id="applicant.id" @changed="load" />
            </div>
            <div class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <PassportUploadCard
                :applicant-id="applicant.id"
                :profile="{ givenName: applicant.givenName, familyName: applicant.familyName, dob: applicant.dob }"
                @changed="load"
                @edit-profile="navigateTo(localePath('/dashboard/profile'))"
              />
            </div>
          </div>
        </section>

        <section :aria-label="t('dashboard.docs.listTitle')">
          <div class="flex flex-wrap items-end justify-between gap-3">
            <div>
              <h2 class="font-display text-lg font-semibold text-slate-900">
                {{ t('dashboard.docs.listTitle') }}
                <span v-if="docs.length" class="ms-1 rounded-full bg-slate-100 px-2 py-0.5 align-middle text-xs font-semibold text-slate-600" data-test="docs-count">{{ docs.length }}</span>
              </h2>
              <p class="mt-1 text-sm text-slate-600">{{ t('dashboard.docs.listBlurb') }}</p>
            </div>
            <NButton v-if="!typesUnavailable" size="sm" :variant="showAdd ? 'secondary' : 'primary'" :aria-expanded="showAdd" data-test="docs-add-toggle" @click="showAdd = !showAdd">
              {{ showAdd ? t('common.cancel') : t('dashboard.docs.add') }}
            </NButton>
          </div>

          <Transition name="panel">
            <div v-if="showAdd || typesUnavailable" class="mt-4 rounded-2xl border border-brand-200 bg-brand-50/40 p-5" data-test="docs-add-panel">
              <NAlert v-if="typesUnavailable" tone="warning" data-test="types-unavailable">{{ t('dashboard.docs.typesUnavailable') }}</NAlert>
              <template v-else>
                <p class="mb-4 text-sm text-slate-600">{{ t('dashboard.docs.addBlurb') }}</p>
                <AddDocumentForm :types="typeOptions" :busy="busy" @submit="onCreate" />
              </template>
            </div>
          </Transition>

          <div class="mt-4">
            <AsyncState :pending="docsPending" :error="docsError" :empty="!docs.length">
              <template #loading>
                <div class="grid gap-3 sm:grid-cols-2">
                  <NSkeleton class="h-36 w-full rounded-2xl" />
                  <NSkeleton class="h-36 w-full rounded-2xl" />
                </div>
              </template>
              <template #error>
                <NAlert tone="danger">
                  {{ docsError }}
                  <button type="button" class="ms-2 font-medium underline" @click="loadDocuments">{{ t('common.retry') }}</button>
                </NAlert>
              </template>
              <template #empty>
                <div class="grid justify-items-center gap-2 rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center" data-test="docs-empty">
                  <span class="grid h-12 w-12 place-items-center rounded-full bg-slate-100 text-slate-500" aria-hidden="true">
                    <svg viewBox="0 0 24 24" class="h-6 w-6" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M7 3h7l4 4v14H7V3Z" stroke-linejoin="round" /><path d="M14 3v5h4M10 13h5M10 17h5" stroke-linecap="round" /></svg>
                  </span>
                  <p class="text-sm text-slate-600">{{ t('dashboard.docs.empty') }}</p>
                </div>
              </template>

              <ul class="grid gap-3 sm:grid-cols-2">
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
          </div>
        </section>
      </div>
    </AsyncState>
  </div>
</template>

<style scoped>
.panel-enter-active, .panel-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.panel-enter-from, .panel-leave-to { opacity: 0; transform: translateY(-8px); }
@media (prefers-reduced-motion: reduce) {
  .panel-enter-active, .panel-leave-active { transition: opacity 0.01s; }
  .panel-enter-from, .panel-leave-to { transform: none; }
}
</style>
