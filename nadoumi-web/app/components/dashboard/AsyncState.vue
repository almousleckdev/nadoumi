<script setup lang="ts">
withDefaults(defineProps<{ pending: boolean; error?: string; empty?: boolean }>(), {
  error: '',
  empty: false,
})
const { t } = useI18n()
</script>

<template>
  <div>
    <slot v-if="pending" name="loading">
      <div class="py-8 text-center"><NSpinner /></div>
    </slot>
    <slot v-else-if="error" name="error">
      <NAlert tone="danger">{{ error }}</NAlert>
    </slot>
    <slot v-else-if="empty" name="empty">
      <p class="py-8 text-center text-sm text-slate-500">{{ t('common.nothingYet') }}</p>
    </slot>
    <slot v-else />
  </div>
</template>
