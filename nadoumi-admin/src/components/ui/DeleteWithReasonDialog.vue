<template>
  <el-dialog
    v-model="open"
    :title="title"
    width="480px"
    destroy-on-close
  >
    <p class="dwr__body">
      {{ message }}
    </p>
    <el-form
      label-position="top"
      @submit.prevent="submit"
    >
      <el-form-item
        :label="t('common.reasonLabel')"
        required
      >
        <el-input
          v-model="reason"
          type="textarea"
          :rows="3"
          :maxlength="MAX_REASON_CHARS"
          show-word-limit
          :placeholder="t('common.reasonPlaceholder')"
          data-test="delete-reason"
        />
      </el-form-item>
      <p
        v-if="reason && !valid"
        class="dwr__hint"
        data-test="delete-hint"
      >
        {{ t('common.reasonTooShort', { min: MIN_REASON_CHARS }) }}
      </p>
    </el-form>
    <template #footer>
      <el-button @click="open = false">
        {{ t('common.cancel') }}
      </el-button>
      <el-button
        type="danger"
        :disabled="!valid"
        :loading="busy"
        data-test="delete-confirm"
        @click="submit"
      >
        {{ t('common.delete') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'

// keep in step with the server's @Size(min = 5, max = 300) on the delete requests
const MIN_REASON_CHARS = 5
const MAX_REASON_CHARS = 300

defineProps<{ title: string, message: string, busy?: boolean }>()
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

<style scoped>
.dwr__body { margin: 0 0 12px; color: var(--el-text-color-regular); }
.dwr__hint { margin: 0; font-size: 12px; color: #b91c1c; }
</style>
