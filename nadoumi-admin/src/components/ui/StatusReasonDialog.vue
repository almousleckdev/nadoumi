<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'

const MIN_REASON_CHARS = 5
const MAX_REASON_CHARS = 255

const props = defineProps<{ title: string, message: string, confirmText?: string, busy?: boolean }>()
const open = defineModel<boolean>({ required: true })
const emit = defineEmits<{ confirm: [reason: string] }>()

const { t } = useI18n()
const reason = ref('')

const trimmed = computed(() => reason.value.trim())
const valid = computed(() => trimmed.value.length >= MIN_REASON_CHARS)

watch(open, (isOpen) => {
  if (isOpen) reason.value = ''
})

function submit() {
  if (valid.value) emit('confirm', trimmed.value)
}
</script>

<template>
  <el-dialog
    v-model="open"
    :title="props.title"
    width="480px"
    destroy-on-close
  >
    <p class="reason__message">
      {{ props.message }}
    </p>
    <el-form
      label-position="top"
      @submit.prevent="submit"
    >
      <el-form-item
        :label="t('statusReason.label')"
        required
      >
        <el-input
          v-model="reason"
          type="textarea"
          :rows="4"
          :maxlength="MAX_REASON_CHARS"
          show-word-limit
          :placeholder="t('statusReason.placeholder')"
          data-test="reason-input"
        />
      </el-form-item>
      <p
        v-if="reason && !valid"
        class="reason__hint"
        data-test="reason-hint"
      >
        {{ t('statusReason.tooShort', { min: MIN_REASON_CHARS }) }}
      </p>
    </el-form>
    <template #footer>
      <el-button @click="open = false">
        {{ t('common.cancel') }}
      </el-button>
      <el-button
        type="danger"
        :disabled="!valid"
        :loading="props.busy"
        data-test="reason-confirm"
        @click="submit"
      >
        {{ props.confirmText || t('common.confirm') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.reason__message {
  margin: 0 0 14px;
  font-size: 14px;
  color: var(--nad-ink-soft);
}
.reason__hint {
  margin: -8px 0 0;
  font-size: 12px;
  color: var(--el-color-danger);
}
</style>
