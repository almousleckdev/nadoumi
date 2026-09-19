<script setup lang="ts">
/** The keyword search box every catalog list page uses; committed on submit, not per keystroke. */
defineProps<{ id: string, label: string, placeholder: string }>()
const model = defineModel<string>({ required: true })
defineEmits<{ submit: [] }>()
const { t } = useI18n()
</script>

<template>
  <form role="search" class="flex items-center gap-2" @submit.prevent="$emit('submit')">
    <label :for="id" class="sr-only">{{ label }}</label>
    <input
      :id="id"
      v-model="model"
      type="search"
      :placeholder="placeholder"
      class="w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus-visible:border-brand-500"
    >
    <NButton type="submit" size="sm">{{ t('common.search') }}</NButton>
  </form>
</template>
