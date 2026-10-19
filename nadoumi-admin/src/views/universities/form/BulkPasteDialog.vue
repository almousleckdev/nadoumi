<script setup lang="ts" generic="T">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ParseResult } from '../bulkPaste'

const props = defineProps<{
  modelValue: boolean
  title: string
  hint: string
  placeholder: string
  parse: (text: string) => ParseResult<T>
}>()
const emit = defineEmits<{ 'update:modelValue': [v: boolean], 'confirm': [rows: T[]] }>()

const { t } = useI18n()
const text = ref('')
const result = computed(() => props.parse(text.value))

watch(() => props.modelValue, (open) => {
  if (open) text.value = ''
})

function confirm() {
  if (!result.value.rows.length) return
  emit('confirm', result.value.rows)
  emit('update:modelValue', false)
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    width="520"
    append-to-body
    @update:model-value="v => emit('update:modelValue', v)"
  >
    <p class="bulk__hint">
      {{ hint }}
    </p>
    <el-input
      v-model="text"
      type="textarea"
      :rows="9"
      :placeholder="placeholder"
      spellcheck="false"
    />
    <p
      v-if="text.trim()"
      class="bulk__summary"
    >
      {{ t('bulkPaste.summary', { n: result.rows.length, skipped: result.skipped }) }}
    </p>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">
        {{ t('common.cancel') }}
      </el-button>
      <el-button
        type="primary"
        :disabled="!result.rows.length"
        @click="confirm"
      >
        {{ t('bulkPaste.add', { n: result.rows.length }) }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.bulk__hint { margin: 0 0 10px; font-size: 12px; line-height: 1.5; color: var(--nad-ink-soft); white-space: pre-line; }
.bulk__summary { margin: 8px 0 0; font-size: 12px; color: var(--nad-ink-soft); }
</style>
