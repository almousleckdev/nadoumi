<script setup lang="ts">
/**
 * A compact single-file picker: a small preview, one button and a hint. Used for the profile photo (circle)
 * and the passport scan (document). The parent decides what to do with the chosen file.
 */
const props = withDefaults(defineProps<{
  accept: string
  mime: RegExp
  maxMb: number
  badTypeMessage: string
  label: string
  hint?: string
  /** `circle` for a portrait, `document` for a page. */
  shape?: 'circle' | 'document'
  /** Image to preview: the saved file or the one just chosen. */
  src?: string
  /** The chosen file is not an image, so show a file mark instead of a preview. */
  pdf?: boolean
  placeholder?: string
}>(), { shape: 'circle', hint: '', src: '', pdf: false, placeholder: '' })
const emit = defineEmits<{ select: [file: File] }>()

const inputId = useId()
const failed = ref(false)
watch(() => props.src, () => { failed.value = false })

const { error, onInput } = useFilePick(() => props, file => emit('select', file))
const showImage = computed(() => Boolean(props.src) && !props.pdf && !failed.value)
</script>

<template>
  <div class="flex items-center gap-4">
    <div
      class="flex shrink-0 items-center justify-center overflow-hidden border border-slate-200 bg-slate-50 text-slate-400"
      :class="shape === 'circle' ? 'h-24 w-24 rounded-full' : 'h-24 w-[4.5rem] rounded-lg'"
    >
      <img v-if="showImage" :src="src" alt="" class="h-full w-full object-cover" @error="failed = true">
      <span v-else-if="pdf" class="text-xs font-semibold text-slate-500">PDF</span>
      <svg v-else-if="shape === 'circle'" viewBox="0 0 24 24" class="h-10 w-10" fill="none" stroke="currentColor" stroke-width="1.4" aria-hidden="true">
        <path d="M20 21a8 8 0 1 0-16 0M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z" stroke-linecap="round" stroke-linejoin="round" />
      </svg>
      <svg v-else viewBox="0 0 24 24" class="h-9 w-9" fill="none" stroke="currentColor" stroke-width="1.4" aria-hidden="true">
        <rect x="4" y="3" width="16" height="18" rx="2" /><circle cx="12" cy="9" r="2.5" /><path d="M8 16h8" />
      </svg>
    </div>

    <div class="min-w-0">
      <label
        :for="inputId"
        class="inline-flex cursor-pointer items-center rounded-md border border-slate-200 bg-white px-3 py-1.5 text-sm font-semibold text-slate-900 transition-colors focus-within:outline focus-within:outline-2 focus-within:outline-brand-500 hover:bg-slate-50"
      >
        {{ label }}
        <input :id="inputId" type="file" :accept="accept" class="sr-only" @change="onInput">
      </label>
      <p v-if="hint" class="mt-1.5 text-xs text-slate-500">{{ hint }}</p>
      <p v-if="error" class="mt-1.5 text-xs font-medium text-red-600" role="alert">{{ error }}</p>
    </div>
  </div>
</template>
