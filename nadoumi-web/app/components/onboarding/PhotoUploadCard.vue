<script setup lang="ts">
import { PHOTO_ACCEPT, PHOTO_MAX_MB, PHOTO_MIME, PHOTO_MIN_PX } from '~/constants/passport'
import { dataUrlToBlob, imageSize, readAsDataUrl } from '~/utils/files'

/** Profile photo: pick, check size, crop to a square, save. Stored privately; shown through a signed URL. */
const props = defineProps<{ applicantId: number }>()
const emit = defineEmits<{ changed: [] }>()
const { t } = useI18n()
const { uploadPhoto, photoUrl } = useApplicant()
const { busy, error, notice, run } = useAsyncAction()

const savedUrl = ref('')
const rawSrc = ref('')
const croppedSrc = ref('')
const problem = ref('')

async function loadSaved() {
  savedUrl.value = await photoUrl(props.applicantId).then(r => r.url).catch(() => '')
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
  rawSrc.value = source
  croppedSrc.value = ''
}

async function save() {
  const saved = await run(async () => {
    await uploadPhoto(props.applicantId, await dataUrlToBlob(croppedSrc.value))
    await loadSaved()
    return true
  }, t('onboarding.photo.saved'))
  if (!saved) return
  rawSrc.value = ''
  croppedSrc.value = ''
  emit('changed')
}

const status = computed(() => (savedUrl.value ? 'done' : 'pending'))
</script>

<template>
  <DocumentCard :title="t('onboarding.photo.title')" :guidance="t('onboarding.photo.guidance')" :status="status">
    <NAlert v-if="problem" tone="danger">{{ problem }}</NAlert>
    <NAlert v-if="error" tone="danger">{{ error }}</NAlert>
    <NAlert v-if="notice" tone="success">{{ notice }}</NAlert>

    <div class="grid gap-4 sm:grid-cols-[auto_1fr] sm:items-start">
      <figure class="mx-auto h-28 w-28 shrink-0 overflow-hidden rounded-full border border-slate-200 bg-slate-100">
        <img v-if="croppedSrc || savedUrl" :src="croppedSrc || savedUrl" :alt="t('onboarding.photo.current')" class="h-full w-full object-cover">
        <figcaption v-else class="flex h-full w-full items-center justify-center text-xs text-slate-400">
          {{ t('onboarding.photo.none') }}
        </figcaption>
      </figure>

      <div class="grid gap-3">
        <ImageCropper v-if="rawSrc && !croppedSrc" :src="rawSrc" :aspect="1" :size="240" @crop="croppedSrc = $event" />
        <DocumentDropzone
          :accept="PHOTO_ACCEPT"
          :mime="PHOTO_MIME"
          :max-mb="PHOTO_MAX_MB"
          :bad-type-message="t('onboarding.upload.badType')"
          :replace="Boolean(savedUrl)"
          @select="onSelect"
        />
        <div v-if="croppedSrc">
          <NButton size="sm" :loading="busy" @click="save">{{ t('onboarding.photo.save') }}</NButton>
        </div>
      </div>
    </div>
  </DocumentCard>
</template>
