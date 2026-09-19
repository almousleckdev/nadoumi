<script setup lang="ts">
/** Keeps a slot that loads its own data (async setup) from leaving a blank gap: a spinner shows meanwhile. */
const { t } = useI18n()
</script>

<template>
  <Suspense>
    <slot />
    <template #fallback>
      <div class="flex justify-center py-10"><NSpinner size="md" :label="t('common.loading')" /></div>
    </template>
  </Suspense>
</template>
