<script setup lang="ts">
/**
 * Passport page: pick → validate → large readable preview (zoom / rotate) →
 * confirm readable. Client-only. Upload REQUIRES BACKEND (Document slice).
 */
const { t } = useI18n()

const MAX_BYTES = 10 * 1024 * 1024

const src = ref('')
const fileType = ref('')
const fileName = ref('')
const readable = ref(false)
const error = ref('')

async function onPick(event: Event) {
  error.value = ''
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  if (!/^(image\/(jpeg|png)|application\/pdf)$/.test(file.type)) { error.value = t('onboarding.upload.badTypePassport'); return }
  if (file.size > MAX_BYTES) { error.value = t('onboarding.upload.tooBig', { mb: 10 }); return }
  src.value = await readAsDataUrl(file)
  fileType.value = file.type
  fileName.value = file.name
  readable.value = false
}
function remove() {
  src.value = ''
  fileType.value = ''
  fileName.value = ''
  readable.value = false
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
</script>

<template>
  <NCard>
    <div class="flex items-start justify-between gap-3">
      <div>
        <h3 class="font-display font-semibold text-slate-900">{{ t('onboarding.passport.title') }}</h3>
        <p class="mt-1 text-sm text-slate-500">{{ t('onboarding.passport.guidance') }}</p>
      </div>
      <NBadge tone="warning">{{ t('onboarding.badge.plannedBackend') }}</NBadge>
    </div>

    <NAlert v-if="error" tone="danger" class="mt-4">{{ error }}</NAlert>

    <div class="mt-4 grid gap-3">
      <DocumentPreview v-if="src" :src="src" :type="fileType" :name="fileName" />
      <div v-else class="flex h-40 items-center justify-center rounded-lg border border-dashed border-slate-300 text-sm text-slate-400">
        {{ t('onboarding.passport.none') }}
      </div>

      <NCheckbox v-if="src && fileType !== 'application/pdf'" id="passport-readable" v-model="readable">
        {{ t('onboarding.passport.confirmReadable') }}
      </NCheckbox>

      <div class="flex flex-wrap gap-2">
        <label class="cursor-pointer">
          <input type="file" accept="image/jpeg,image/png,application/pdf" class="sr-only" @change="onPick">
          <span class="inline-flex items-center rounded-md border border-slate-200 bg-white px-3 py-1.5 text-sm font-semibold hover:bg-slate-50">
            {{ src ? t('onboarding.upload.replace') : t('onboarding.upload.choose') }}
          </span>
        </label>
        <NButton v-if="src" size="sm" variant="ghost" @click="remove">{{ t('onboarding.upload.remove') }}</NButton>
        <NButton size="sm" disabled>{{ t('onboarding.upload.save') }}</NButton>
      </div>
      <p class="text-xs text-slate-400">{{ t('onboarding.upload.notYet') }} · {{ t('onboarding.passport.formats') }}</p>
    </div>
  </NCard>
</template>
