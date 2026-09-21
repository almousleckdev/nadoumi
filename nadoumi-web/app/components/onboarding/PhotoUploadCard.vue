<script setup lang="ts">
import { PHOTO_ACCEPT, PHOTO_MAX_MB, PHOTO_MIME, PHOTO_MIN_PX } from '~/constants/passport'
import { imageSize, readAsDataUrl } from '~/utils/files'

/** Profile photo: pick, check size, save as-is. Stored privately; shown through a signed URL.
 *  Collapses to a compact summary once a photo exists — the full picker only shows while
 *  there's nothing saved yet, or the student explicitly asks to replace it. */
const props = defineProps<{ applicantId: number }>()
const emit = defineEmits<{ changed: [] }>()
const { t } = useI18n()
const { uploadPhoto, photoUrl } = useApplicant()
const { busy, error, notice, run } = useAsyncAction()

const savedUrl = ref('')
const picked = ref<File | null>(null)
const previewSrc = ref('')
const problem = ref('')
const expanded = ref(false)

async function loadSaved() {
  savedUrl.value = await photoUrl(props.applicantId).then(r => r.url).catch(() => '')
  expanded.value = !savedUrl.value
}
onMounted(loadSaved)

async function onSelect(file: File) {
  problem.value = ''
  const source = await readAsDataUrl(file)
  const { width, height } = await imageSize(source)
  if (width < PHOTO_MIN_PX || height < PHOTO_MIN_PX) {
    problem.value = t('onboarding.photo.tooSmall', { px: PHOTO_MIN_PX })
    return
  }
  picked.value = file
  previewSrc.value = source
}

function cancelReplace() {
  picked.value = null
  previewSrc.value = ''
  problem.value = ''
  expanded.value = false
}

async function save() {
  const file = picked.value
  if (!file) return
  const saved = await run(async () => {
    await uploadPhoto(props.applicantId, file)
    await loadSaved()
    return true
  }, t('onboarding.photo.saved'))
  if (!saved) return
  picked.value = null
  previewSrc.value = ''
  expanded.value = false
  emit('changed')
}

const status = computed(() => (savedUrl.value ? 'done' : 'pending'))
</script>

<template>
  <DocumentCard :title="t('onboarding.photo.title')" :guidance="expanded ? t('onboarding.photo.guidance') : ''" :status="status">
    <NAlert v-if="problem" tone="danger">{{ problem }}</NAlert>
    <NAlert v-if="error" tone="danger">{{ error }}</NAlert>
    <NAlert v-if="notice" tone="success">{{ notice }}</NAlert>

    <div v-if="!expanded" class="flex items-center gap-4">
      <figure class="h-24 w-24 shrink-0 overflow-hidden rounded-full border border-slate-200 bg-slate-100 sm:h-28 sm:w-28">
        <img :src="savedUrl" :alt="t('onboarding.photo.current')" class="h-full w-full object-cover">
      </figure>
      <div class="flex-1">
        <p class="text-sm font-medium text-slate-700">{{ t('onboarding.photo.current') }}</p>
        <NButton class="mt-2" size="sm" variant="secondary" @click="expanded = true">{{ t('onboarding.upload.replace') }}</NButton>
      </div>
    </div>

    <div v-else class="grid gap-4 sm:grid-cols-[auto_1fr] sm:items-start">
      <figure class="mx-auto h-28 w-28 shrink-0 overflow-hidden rounded-full border border-slate-200 bg-slate-100">
        <img v-if="previewSrc || savedUrl" :src="previewSrc || savedUrl" :alt="t('onboarding.photo.current')" class="h-full w-full object-cover">
        <figcaption v-else class="flex h-full w-full items-center justify-center text-xs text-slate-400">
          {{ t('onboarding.photo.none') }}
        </figcaption>
      </figure>

      <div class="grid gap-3">
        <DocumentDropzone
          :accept="PHOTO_ACCEPT"
          :mime="PHOTO_MIME"
          :max-mb="PHOTO_MAX_MB"
          :bad-type-message="t('onboarding.upload.badType')"
          :replace="Boolean(savedUrl)"
          @select="onSelect"
        />
        <div v-if="picked" class="flex gap-2">
          <NButton size="sm" :loading="busy" @click="save">{{ t('onboarding.photo.save') }}</NButton>
          <NButton size="sm" variant="ghost" :disabled="busy" @click="cancelReplace">{{ t('common.cancel') }}</NButton>
        </div>
        <div v-else-if="savedUrl">
          <NButton size="sm" variant="ghost" @click="expanded = false">{{ t('common.cancel') }}</NButton>
        </div>
      </div>
    </div>
  </DocumentCard>
</template>
