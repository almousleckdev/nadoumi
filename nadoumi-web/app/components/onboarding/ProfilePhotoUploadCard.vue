<script setup lang="ts">
/**
 * Profile photo: pick → validate → crop 1:1 → preview. Client-only.
 * The actual upload REQUIRES BACKEND (Document slice); the button is disabled.
 */
const { t } = useI18n()

const MAX_BYTES = 5 * 1024 * 1024
const MIN_DIM = 400

const rawSrc = ref('')
const croppedSrc = ref('')
const cropping = ref(false)
const error = ref('')

async function onPick(event: Event) {
  error.value = ''
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  if (!/^image\/(jpeg|png|webp)$/.test(file.type)) { error.value = t('onboarding.upload.badType'); return }
  if (file.size > MAX_BYTES) { error.value = t('onboarding.upload.tooBig', { mb: 5 }); return }

  const dataUrl = await readAsDataUrl(file)
  const dims = await imageSize(dataUrl)
  if (dims.w < MIN_DIM || dims.h < MIN_DIM) { error.value = t('onboarding.photo.tooSmall', { px: MIN_DIM }); return }

  rawSrc.value = dataUrl
  croppedSrc.value = ''
  cropping.value = true
}

function onCropped(dataUrl: string) {
  croppedSrc.value = dataUrl
  cropping.value = false
}
function remove() {
  rawSrc.value = ''
  croppedSrc.value = ''
  cropping.value = false
  error.value = ''
}

function readAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const r = new FileReader()
    r.onload = () => resolve(r.result as string)
    r.onerror = () => reject(r.error)
    r.readAsDataURL(file)
  })
}
function imageSize(src: string): Promise<{ w: number; h: number }> {
  return new Promise((resolve) => {
    const img = new Image()
    img.onload = () => resolve({ w: img.naturalWidth, h: img.naturalHeight })
    img.onerror = () => resolve({ w: 0, h: 0 })
    img.src = src
  })
}
</script>

<template>
  <NCard>
    <div class="flex items-start justify-between gap-3">
      <div>
        <h3 class="font-display font-semibold text-slate-900">{{ t('onboarding.photo.title') }}</h3>
        <p class="mt-1 text-sm text-slate-500">{{ t('onboarding.photo.guidance') }}</p>
      </div>
      <NBadge tone="warning">{{ t('onboarding.badge.plannedBackend') }}</NBadge>
    </div>

    <NAlert v-if="error" tone="danger" class="mt-4">{{ error }}</NAlert>

    <div class="mt-4 grid gap-4 sm:grid-cols-[auto,1fr] sm:items-start">
      <div class="mx-auto h-28 w-28 shrink-0 overflow-hidden rounded-full border border-slate-200 bg-slate-100">
        <img v-if="croppedSrc" :src="croppedSrc" alt="" class="h-full w-full object-cover">
        <div v-else class="flex h-full w-full items-center justify-center text-xs text-slate-400">
          {{ t('onboarding.photo.none') }}
        </div>
      </div>

      <div class="grid gap-3">
        <ImageCropper v-if="cropping" :src="rawSrc" :aspect="1" :size="240" @crop="onCropped" />
        <div class="flex flex-wrap gap-2">
          <label class="cursor-pointer">
            <input type="file" accept="image/jpeg,image/png,image/webp" class="sr-only" @change="onPick">
            <span class="inline-flex items-center rounded-md border border-slate-200 bg-white px-3 py-1.5 text-sm font-semibold hover:bg-slate-50">
              {{ croppedSrc || rawSrc ? t('onboarding.upload.replace') : t('onboarding.upload.choose') }}
            </span>
          </label>
          <NButton v-if="croppedSrc || rawSrc" size="sm" variant="ghost" @click="remove">{{ t('onboarding.upload.remove') }}</NButton>
          <NButton size="sm" disabled>{{ t('onboarding.upload.save') }}</NButton>
        </div>
        <p class="text-xs text-slate-400">{{ t('onboarding.upload.notYet') }}</p>
      </div>
    </div>
  </NCard>
</template>
