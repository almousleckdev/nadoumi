<template>
  <div
    class="err"
    role="alert"
  >
    <el-icon
      class="err__icon"
      :size="36"
    >
      <WarningFilled />
    </el-icon>
    <p class="err__title">
      {{ title || t('state.errorTitle') }}
    </p>
    <p
      v-if="message"
      class="err__msg"
    >
      {{ message }}
    </p>
    <el-button
      v-if="retryable"
      size="small"
      :icon="Refresh"
      @click="emit('retry')"
    >
      {{ t('state.retry') }}
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { Refresh } from '@element-plus/icons-vue'

withDefaults(defineProps<{
  message?: string
  title?: string
  retryable?: boolean
}>(), { message: undefined, title: undefined, retryable: true })

const emit = defineEmits<{ retry: [] }>()
const { t } = useI18n()
</script>

<style scoped>
.err {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 36px 24px;
  gap: 6px;
}
.err__icon {
  color: var(--el-color-danger);
}
.err__title {
  margin: 6px 0 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--nad-ink);
}
.err__msg {
  margin: 0 0 8px;
  font-size: 13px;
  color: var(--nad-ink-soft);
  max-width: 48ch;
}
</style>
