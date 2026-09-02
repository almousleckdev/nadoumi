<template>
  <el-drawer
    :model-value="modelValue"
    :title="title"
    :size="size"
    :close-on-click-modal="!saving"
    :close-on-press-escape="!saving"
    @update:model-value="(v: boolean) => emit('update:modelValue', v)"
    @closed="emit('closed')"
  >
    <div class="drawer__body">
      <slot />
    </div>
    <template #footer>
      <div class="drawer__footer">
        <el-button
          :disabled="saving"
          @click="emit('update:modelValue', false)"
        >
          {{ cancelLabel || t('common.cancel') }}
        </el-button>
        <el-button
          type="primary"
          :loading="saving"
          @click="emit('save')"
        >
          {{ saveLabel || t('common.save') }}
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

withDefaults(defineProps<{
  modelValue: boolean
  title: string
  saving?: boolean
  saveLabel?: string
  cancelLabel?: string
  size?: number | string
}>(), { saving: false, saveLabel: undefined, cancelLabel: undefined, size: 420 })

const emit = defineEmits<{
  'update:modelValue': [v: boolean]
  'save': []
  'closed': []
}>()

const { t } = useI18n()
</script>

<style scoped>
.drawer__body {
  padding-bottom: 8px;
}
.drawer__footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
