<template>
  <el-dialog
    v-model="open"
    :title="t('payment.rejectTitle')"
    width="480px"
    destroy-on-close
  >
    <el-form
      label-position="top"
      @submit.prevent="submit"
    >
      <el-form-item
        :label="t('payment.rejectReason')"
        required
      >
        <el-input
          v-model="reason"
          type="textarea"
          :rows="4"
          :maxlength="MAX_REASON_CHARS"
          show-word-limit
          :placeholder="t('payment.rejectPlaceholder')"
          data-test="reject-reason"
        />
      </el-form-item>
      <p
        v-if="reason && !valid"
        class="pr__hint"
        data-test="reject-hint"
      >
        {{ t('payment.reasonTooShort', { min: MIN_REASON_CHARS }) }}
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
        data-test="reject-confirm"
        @click="submit"
      >
        {{ t('payment.reject') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'

const MIN_REASON_CHARS = 3
const MAX_REASON_CHARS = 300

defineProps<{ busy?: boolean }>()
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
.pr__hint { margin: 0; font-size: 12px; color: #b91c1c; }
</style>
