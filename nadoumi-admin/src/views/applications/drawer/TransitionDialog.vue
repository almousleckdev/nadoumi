<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'

const open = defineModel<boolean>({ required: true })
defineProps<{ saving: boolean }>()
const emit = defineEmits<{ submit: [payload: { code: string, reason: string }] }>()

const { t } = useI18n()

const form = reactive({ code: '', reason: '' })
const error = ref('')

watch(open, (isOpen) => {
  if (!isOpen) return
  form.code = ''
  form.reason = ''
  error.value = ''
})

function submit() {
  const code = form.code.trim()
  error.value = code ? '' : t('applications.transitionCodeRequired')
  if (!code) return
  emit('submit', { code, reason: form.reason.trim() })
}
</script>

<template>
  <el-dialog
    v-model="open"
    :title="t('applications.transition')"
    width="440px"
    append-to-body
  >
    <el-form
      label-position="top"
      @submit.prevent="submit"
    >
      <el-form-item
        :label="t('applications.transitionCode')"
        :error="error"
      >
        <el-input
          v-model="form.code"
          data-test="transition-code"
          :placeholder="t('applications.transitionCodeHint')"
        />
      </el-form-item>
      <el-form-item :label="t('applications.reason')">
        <el-input
          v-model="form.reason"
          type="textarea"
          :rows="3"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="open = false">
        {{ t('common.cancel') }}
      </el-button>
      <el-button
        type="primary"
        :loading="saving"
        data-test="transition-submit"
        @click="submit"
      >
        {{ t('common.confirm') }}
      </el-button>
    </template>
  </el-dialog>
</template>
