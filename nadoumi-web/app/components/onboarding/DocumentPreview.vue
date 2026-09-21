<script setup lang="ts">
/**
 * Plain preview of a document: an image as-is, or a file card when it can't be shown inline.
 * `type` is the mime type when known (a freshly-picked local file always has one). For an
 * already-saved file we may only have a signed URL and no stored content type — in that case
 * we try rendering it as an image and fall back to the file card if the browser can't load it
 * (e.g. it's a PDF), rather than assuming a type we don't actually know.
 */
const props = defineProps<{ src: string; type?: string; name?: string }>()
const { t } = useI18n()

const imageFailed = ref(false)
watch(() => props.src, () => { imageFailed.value = false })

const isPdf = computed(() => props.type === 'application/pdf' || props.src.startsWith('data:application/pdf'))
const showFileCard = computed(() => isPdf.value || imageFailed.value)
</script>

<template>
  <div
    v-if="!showFileCard"
    class="flex h-72 items-center justify-center overflow-hidden rounded-lg border border-slate-200 bg-slate-900/5"
  >
    <img :src="src" :alt="name ?? t('onboarding.doc.previewAlt')" class="h-full w-full object-contain" @error="imageFailed = true">
  </div>
  <div v-else class="flex items-center gap-3 rounded-lg border border-slate-200 bg-slate-50 p-4 text-sm">
    <span class="rounded bg-white px-2 py-1 text-xs font-semibold text-slate-500">{{ isPdf ? 'PDF' : t('onboarding.doc.fileBadge') }}</span>
    <span class="min-w-0 flex-1 truncate text-slate-700">{{ name ?? 'document' }}</span>
    <a :href="src" target="_blank" rel="noopener" class="shrink-0 font-medium text-brand-700 hover:underline">{{ t('onboarding.doc.viewFile') }}</a>
  </div>
</template>
