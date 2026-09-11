<script setup lang="ts">
withDefaults(defineProps<{
  pending: boolean
  error?: boolean
  isEmpty: boolean
  /** grid min column width */
  minCol?: string
  skeletonCount?: number
}>(), { error: false, minCol: '17rem', skeletonCount: 6 })

const emit = defineEmits<{ retry: [] }>()
const { t } = useI18n()
</script>

<template>
  <div>
    <div
      v-if="pending"
      class="grid gap-5"
      :style="{ gridTemplateColumns: `repeat(auto-fill, minmax(${minCol}, 1fr))` }"
      aria-hidden="true"
    >
      <div
        v-for="i in skeletonCount"
        :key="i"
        class="h-64 animate-pulse rounded-xl bg-slate-100"
      />
    </div>

    <NAlert v-else-if="error" tone="danger">
      {{ t('errors.loadSection') }}
      <button type="button" class="ms-2 font-medium underline" @click="emit('retry')">
        {{ t('common.retry') }}
      </button>
    </NAlert>

    <div v-else-if="isEmpty">
      <slot name="empty">
        <div class="rounded-xl border border-dashed border-slate-200 px-6 py-14 text-center">
          <p class="font-display text-lg font-semibold text-slate-900">{{ t('catalog.noResultsTitle') }}</p>
          <p class="mt-1 text-sm text-slate-600">{{ t('catalog.noResultsBody') }}</p>
        </div>
      </slot>
    </div>

    <div
      v-else
      class="grid gap-5"
      :style="{ gridTemplateColumns: `repeat(auto-fill, minmax(${minCol}, 1fr))` }"
    >
      <slot />
    </div>
  </div>
</template>
