<script setup lang="ts">
import type { ApplicantDto } from '~/types/catalog'

/** Who the application is for: every student this account may act for. */
defineProps<{ applicants: ApplicantDto[], modelValue: number | null }>()
const emit = defineEmits<{ 'update:modelValue': [id: number] }>()

const { t } = useI18n()

function label(a: ApplicantDto) {
  return `${a.givenName} ${a.familyName}`.trim() || t('apply.unnamedStudent')
}
</script>

<template>
  <fieldset class="grid gap-2">
    <legend class="sr-only">{{ t('apply.studentLegend') }}</legend>
    <label
      v-for="a in applicants"
      :key="a.id"
      class="flex cursor-pointer items-center gap-3 rounded-xl border p-3 text-sm"
      :class="a.id === modelValue ? 'border-slate-900 bg-slate-50' : 'border-slate-200 bg-white hover:bg-slate-50'"
      :data-test="`student-${a.id}`"
    >
      <input
        type="radio"
        name="apply-student"
        class="h-4 w-4 accent-slate-900"
        :checked="a.id === modelValue"
        @change="emit('update:modelValue', a.id)"
      >
      <span class="grid min-w-0">
        <span class="truncate font-medium text-slate-900">{{ label(a) }}</span>
        <span v-if="a.email" class="truncate text-xs text-slate-500">{{ a.email }}</span>
      </span>
    </label>
  </fieldset>
</template>
