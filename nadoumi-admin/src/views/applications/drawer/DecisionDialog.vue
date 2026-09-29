<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { DECISION_TYPES } from '../vocabulary'

const open = defineModel<boolean>({ required: true })
defineProps<{ saving: boolean }>()
const emit = defineEmits<{ submit: [payload: { decisionType: string, outcome: string, rationale: string }] }>()

const { t } = useI18n()

const form = reactive({ decisionType: '', outcome: '', rationale: '' })
const error = ref('')

watch(open, (isOpen) => {
  if (!isOpen) return
  form.decisionType = DECISION_TYPES[0]!
  form.outcome = ''
  form.rationale = ''
  error.value = ''
})

function submit() {
  const payload = {
    decisionType: form.decisionType.trim(),
    outcome: form.outcome.trim(),
    rationale: form.rationale.trim(),
  }
  error.value = payload.decisionType && payload.outcome && payload.rationale ? '' : t('applications.decisionRequired')
  if (error.value) return
  emit('submit', payload)
}
</script>

<template>
  <el-dialog
    v-model="open"
    :title="t('applications.recordDecision')"
    width="440px"
    append-to-body
  >
    <el-form
      label-position="top"
      @submit.prevent="submit"
    >
      <el-form-item :label="t('applications.decisionType')">
        <el-select
          v-model="form.decisionType"
          filterable
          allow-create
          style="width: 100%"
        >
          <el-option
            v-for="ty in DECISION_TYPES"
            :key="ty"
            :label="ty"
            :value="ty"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('applications.outcome')">
        <el-input
          v-model="form.outcome"
          data-test="decision-outcome"
        />
      </el-form-item>
      <el-form-item
        :label="t('applications.rationale')"
        :error="error"
      >
        <el-input
          v-model="form.rationale"
          type="textarea"
          :rows="3"
          data-test="decision-rationale"
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
        data-test="decision-submit"
        @click="submit"
      >
        {{ t('common.confirm') }}
      </el-button>
    </template>
  </el-dialog>
</template>
