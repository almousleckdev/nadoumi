<script setup lang="ts">
const props = withDefaults(defineProps<{ value: string; forbidden?: string[]; confirm?: string }>(), {
  forbidden: () => [],
  confirm: undefined,
})
const { t } = useI18n()

const result = computed(() => passwordChecks(props.value, {
  forbidden: props.forbidden,
  confirm: props.confirm,
}))

const showMismatch = computed(() =>
  props.confirm !== undefined && props.confirm.length > 0 && props.value !== props.confirm)
</script>

<template>
  <div class="rounded-md border border-slate-200 bg-slate-50/60 p-3">
    <p v-if="result.strong" class="flex items-center gap-1.5 text-sm font-medium text-emerald-700">
      <span aria-hidden="true">✓</span>{{ t('auth.pwStrong') }}
    </p>
    <template v-else>
      <p class="mb-1.5 text-xs font-medium text-slate-500">{{ t('auth.pwRequirements') }}</p>
      <ul class="grid gap-1">
        <li
          v-for="rule in result.rules"
          :key="rule.key"
          class="flex items-center gap-1.5 text-xs transition-colors"
          :class="rule.ok ? 'text-emerald-700' : 'text-slate-500'"
        >
          <span class="inline-flex h-3.5 w-3.5 items-center justify-center" aria-hidden="true">
            {{ rule.ok ? '✓' : '•' }}
          </span>
          {{ t(rule.key) }}
        </li>
      </ul>
    </template>
    <p v-if="showMismatch" role="alert" class="mt-1.5 text-xs text-red-600">{{ t('validation.password.mismatch') }}</p>
  </div>
</template>
