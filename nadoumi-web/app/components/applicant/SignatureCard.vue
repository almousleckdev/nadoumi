<script setup lang="ts">
import { SIGNATURE_ACCEPT, SIGNATURE_MAX_MB, SIGNATURE_MIME } from '~/constants/signature'
import { validateFile } from '~/utils/files'

/**
 * The student's signature, given once and printed on the application form. Draw it, or upload a picture of it. It
 * tells its parent whether one is on file so Submit can follow (an application is not valid without it).
 */
const props = defineProps<{ applicantId: number }>()
const emit = defineEmits<{ ready: [value: boolean] }>()

const { t } = useI18n()
const api = useApplicantSignature(() => props.applicantId)
const { busy, error, run } = useAsyncAction()

const pad = ref<{ toBlob: () => Promise<Blob | null>, clear: () => void } | null>(null)
const signed = ref(false)
const signedAt = ref<string | null>(null)
const imageSrc = ref('')
const editing = ref(false)
const hasInk = ref(false)
const problem = ref('')
const inputId = useId()

function revoke() {
  if (imageSrc.value) URL.revokeObjectURL(imageSrc.value)
  imageSrc.value = ''
}

async function load() {
  const status = await api.status().catch(() => null)
  signed.value = status?.signed === true
  signedAt.value = status?.signedAt ?? null
  revoke()
  if (signed.value) {
    try {
      imageSrc.value = URL.createObjectURL(await api.image())
    }
    catch {
      imageSrc.value = ''
    }
  }
  editing.value = !signed.value
  emit('ready', signed.value)
}

async function store(png: Blob) {
  const done = await run(() => api.save(png), t('signature.saved'))
  if (done) {
    pad.value?.clear()
    await load()
  }
}

async function saveDrawing() {
  const png = await pad.value?.toBlob()
  if (png) await store(png)
}

async function onPick(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  problem.value = validateFile(file, { mime: SIGNATURE_MIME, maxMb: SIGNATURE_MAX_MB }) ? t('signature.badType') : ''
  if (!problem.value) await store(file)
}

onMounted(load)
onBeforeUnmount(revoke)
</script>

<template>
  <SectionCard :title="t('signature.title')" data-test="signature-card">
    <p class="mb-3 text-sm text-slate-600">{{ t('signature.blurb') }}</p>
    <NAlert v-if="error" tone="danger" class="mb-3">{{ error }}</NAlert>

    <div v-if="signed && !editing" class="grid gap-3" data-test="signed">
      <img v-if="imageSrc" :src="imageSrc" :alt="t('signature.title')" class="h-24 w-auto max-w-full self-start rounded-lg border border-slate-200 bg-white p-2">
      <p class="text-sm text-slate-500">{{ t('signature.onFile') }}</p>
      <div>
        <NButton variant="secondary" size="sm" data-test="redo" @click="editing = true">{{ t('signature.redo') }}</NButton>
      </div>
    </div>

    <div v-else class="grid gap-4">
      <SignaturePad ref="pad" @change="hasInk = $event" />
      <div class="flex flex-wrap items-center gap-3">
        <NButton :loading="busy" :disabled="!hasInk" data-test="save-signature" @click="saveDrawing">{{ t('signature.save') }}</NButton>
        <label
          :for="inputId"
          class="relative inline-flex cursor-pointer items-center rounded-md border border-slate-200 bg-white px-3 py-1.5 text-sm font-semibold text-slate-900 transition-colors focus-within:border-brand-500 hover:bg-slate-50"
          :class="busy ? 'pointer-events-none opacity-60' : ''"
        >
          {{ t('signature.upload') }}
          <input :id="inputId" type="file" :accept="SIGNATURE_ACCEPT" class="sr-only" data-test="upload-signature" :disabled="busy" @change="onPick">
        </label>
        <NButton v-if="signed" variant="ghost" size="sm" @click="editing = false">{{ t('common.cancel') }}</NButton>
      </div>
      <NAlert v-if="problem" tone="danger">{{ problem }}</NAlert>
    </div>
  </SectionCard>
</template>
