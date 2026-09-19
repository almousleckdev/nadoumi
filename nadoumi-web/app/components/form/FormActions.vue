<script setup lang="ts">
/** The submit (and optional cancel) row that closes every form. */
withDefaults(defineProps<{ busy: boolean, submitLabel?: string, cancellable?: boolean }>(), {
  submitLabel: undefined, cancellable: false,
})
defineEmits<{ cancel: [] }>()
const { t } = useI18n()
</script>

<template>
  <div class="flex gap-2 sm:col-span-2">
    <NButton type="submit" size="sm" :loading="busy">{{ submitLabel ?? t('common.save') }}</NButton>
    <NButton v-if="cancellable" variant="secondary" size="sm" @click="$emit('cancel')">{{ t('common.cancel') }}</NButton>
  </div>
</template>
