<script setup lang="ts">
withDefaults(defineProps<{ atStart?: boolean, atEnd?: boolean, label?: string }>(), {
  atStart: false,
  atEnd: false,
  label: undefined,
})
const emit = defineEmits<{ prev: [], next: [] }>()
const { t } = useI18n()
</script>

<template>
  <div class="flex items-center gap-1.5" :aria-label="label">
    <button
      type="button"
      class="inline-flex h-9 w-9 items-center justify-center rounded-full border border-slate-200 text-slate-600 transition-colors hover:border-slate-300 hover:bg-slate-50 disabled:opacity-40 disabled:hover:bg-transparent"
      :disabled="atStart"
      :aria-label="t('common.previous')"
      @click="emit('prev')"
    >
      <svg viewBox="0 0 20 20" fill="none" class="h-4 w-4" aria-hidden="true">
        <path d="M12.5 4.5 7 10l5.5 5.5" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" />
      </svg>
    </button>
    <button
      type="button"
      class="inline-flex h-9 w-9 items-center justify-center rounded-full border border-slate-200 text-slate-600 transition-colors hover:border-slate-300 hover:bg-slate-50 disabled:opacity-40 disabled:hover:bg-transparent"
      :disabled="atEnd"
      :aria-label="t('common.next')"
      @click="emit('next')"
    >
      <svg viewBox="0 0 20 20" fill="none" class="h-4 w-4" aria-hidden="true">
        <path d="M7.5 4.5 13 10l-5.5 5.5" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" />
      </svg>
    </button>
  </div>
</template>
