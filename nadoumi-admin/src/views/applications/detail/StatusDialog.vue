<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { listNextSteps, type NextStep } from '@/api/application'
import { NOTE_REQUIRED_STEPS } from '../vocabulary'

const open = defineModel<boolean>({ required: true })
const props = defineProps<{ saving: boolean, applicationId: number }>()
const emit = defineEmits<{ submit: [payload: { code: string, note: string }] }>()

const { t } = useI18n()
const steps = ref<NextStep[]>([])
const loading = ref(false)
const code = ref('')
const note = ref('')
const needsNote = computed(() => NOTE_REQUIRED_STEPS.includes(code.value))

watch(open, async (isOpen) => {
  if (!isOpen) return
  code.value = ''
  note.value = ''
  loading.value = true
  try {
    steps.value = await listNextSteps(props.applicationId)
    const first = steps.value.find(s => s.blockedReason === null)
    code.value = first?.code ?? ''
  }
  finally {
    loading.value = false
  }
})
</script>

<template>
  <el-dialog
    v-model="open"
    :title="t('applications.changeStatus')"
    width="520px"
    append-to-body
  >
    <p class="sd-help">
      {{ t('applications.changeHelp') }}
    </p>
    <div v-loading="loading">
      <p
        v-if="!loading && !steps.length"
        class="sd-help"
      >
        {{ t('applications.noSteps') }}
      </p>
      <el-radio-group
        v-model="code"
        class="sd-steps"
      >
        <el-radio
          v-for="s in steps"
          :key="s.code"
          :value="s.code"
          :disabled="s.blockedReason !== null"
          border
          class="sd-step"
          :data-test="`step-${s.code}`"
        >
          <span class="sd-label">{{ s.label }}</span>
          <span
            v-if="s.decision"
            class="sd-sub"
          >{{ t('applications.recordsDecision', { decision: s.decision }) }}</span>
          <span
            v-if="s.blockedReason"
            class="sd-blocked"
          >{{ s.blockedReason }}</span>
        </el-radio>
      </el-radio-group>
      <el-form
        v-if="steps.length"
        label-position="top"
        class="sd-note"
      >
        <el-form-item
          :label="t('applications.note')"
          :required="needsNote"
        >
          <el-input
            v-model="note"
            type="textarea"
            :rows="3"
            :placeholder="needsNote ? t('applications.noteRequiredHint') : t('applications.noteHint')"
            data-test="status-note"
          />
        </el-form-item>
      </el-form>
    </div>
    <template #footer>
      <el-button @click="open = false">
        {{ t('common.cancel') }}
      </el-button>
      <el-button
        type="primary"
        :loading="saving"
        :disabled="!code || (needsNote && !note.trim())"
        data-test="status-submit"
        @click="emit('submit', { code, note: note.trim() })"
      >
        {{ t('applications.confirmChange') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.sd-help { margin: 0 0 12px; font-size: 13px; color: var(--nad-ink-soft, #64748b); }
.sd-steps { display: grid; gap: 8px; width: 100%; }
.sd-step { height: auto; margin: 0; padding: 10px 14px; white-space: normal; width: 100%; }
.sd-step :deep(.el-radio__label) { display: grid; gap: 2px; }
.sd-label { font-weight: 600; }
.sd-sub { font-size: 12px; color: var(--nad-ink-soft, #64748b); }
.sd-blocked { font-size: 12px; color: var(--el-color-danger); }
.sd-note { margin-top: 14px; }
</style>
