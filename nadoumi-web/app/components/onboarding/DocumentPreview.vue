<script setup lang="ts">
/** Plain preview of an uploaded document: an image as-is, or a file card for PDFs (no renderer bundled). */
const props = defineProps<{ src: string; type: string; name?: string }>()
const { t } = useI18n()

const isPdf = computed(() => props.type === 'application/pdf' || props.src.startsWith('data:application/pdf'))
</script>

<template>
  <div
    v-if="!isPdf"
    class="flex h-72 items-center justify-center overflow-hidden rounded-lg border border-slate-200 bg-slate-900/5"
  >
    <img :src="src" :alt="name ?? t('onboarding.doc.previewAlt')" class="h-full w-full object-contain">
  </div>
  <div v-else class="flex items-center gap-3 rounded-lg border border-slate-200 bg-slate-50 p-4 text-sm">
    <span class="rounded bg-white px-2 py-1 text-xs font-semibold text-slate-500">PDF</span>
    <span class="min-w-0 truncate text-slate-700">{{ name ?? 'document.pdf' }}</span>
  </div>
</template>
