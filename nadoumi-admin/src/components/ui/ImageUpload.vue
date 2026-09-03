<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type UploadProps, type UploadRawFile } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import { getToken } from '@/utils/auth'

const props = withDefaults(defineProps<{
  modelValue: string | null
  /** square logo vs wide banner framing */
  aspect?: 'square' | 'wide'
  maxMb?: number
}>(), { aspect: 'wide', maxMb: 5 })

const emit = defineEmits<{ 'update:modelValue': [v: string | null] }>()

const { t } = useI18n()

const action = `${import.meta.env.VITE_APP_BASE_API || '/dev-api'}/common/upload`
const headers = computed(() => ({ Authorization: `Bearer ${getToken() ?? ''}` }))

const beforeUpload: UploadProps['beforeUpload'] = (raw: UploadRawFile) => {
  if (!raw.type.startsWith('image/')) {
    ElMessage.error(t('imageUpload.badType'))
    return false
  }
  if (raw.size / 1024 / 1024 > props.maxMb) {
    ElMessage.error(t('imageUpload.tooBig', { mb: props.maxMb }))
    return false
  }
  return true
}

interface UploadResult { code: number, msg?: string, url?: string }
function onSuccess(res: UploadResult) {
  if (res.code === 200 && res.url) {
    emit('update:modelValue', res.url)
  }
  else {
    ElMessage.error(res.msg || t('imageUpload.failed'))
  }
}
function onError() {
  ElMessage.error(t('imageUpload.failed'))
}
function clear() {
  emit('update:modelValue', null)
}
</script>

<template>
  <div
    class="img-upload"
    :class="aspect"
  >
    <div
      v-if="modelValue"
      class="img-upload__preview"
    >
      <img
        :src="modelValue"
        alt=""
      >
      <button
        type="button"
        class="img-upload__remove"
        :title="t('common.delete')"
        @click="clear"
      >
        <el-icon><Delete /></el-icon>
      </button>
    </div>
    <el-upload
      v-else
      :action="action"
      :headers="headers"
      :show-file-list="false"
      accept="image/*"
      :before-upload="beforeUpload"
      :on-success="onSuccess"
      :on-error="onError"
      drag
      class="img-upload__drop"
    >
      <el-icon class="img-upload__icon">
        <Plus />
      </el-icon>
      <div class="img-upload__hint">
        {{ t('imageUpload.choose') }}
      </div>
    </el-upload>
  </div>
</template>

<style scoped>
.img-upload__preview {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--nad-border, #e5e7eb);
  width: fit-content;
}
.img-upload__preview img {
  display: block;
  object-fit: cover;
  background: #f4f4f5;
}
.square .img-upload__preview img,
.square :deep(.el-upload-dragger) {
  width: 120px;
  height: 120px;
}
.wide .img-upload__preview img,
.wide :deep(.el-upload-dragger) {
  width: 280px;
  height: 140px;
}
.img-upload__remove {
  position: absolute;
  top: 6px;
  right: 6px;
  border: none;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  border-radius: 6px;
  padding: 4px 6px;
  cursor: pointer;
  display: inline-flex;
}
.img-upload :deep(.el-upload-dragger) {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 0;
}
.img-upload__icon {
  font-size: 22px;
  color: var(--nad-ink-faint, #9ca3af);
}
.img-upload__hint {
  font-size: 12px;
  color: var(--nad-ink-soft, #6b7280);
}
</style>
