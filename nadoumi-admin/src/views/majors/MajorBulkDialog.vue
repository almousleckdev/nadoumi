<template>
  <el-dialog
    :model-value="modelValue"
    :title="t('majors.bulkTitle')"
    width="560px"
    @update:model-value="(v: boolean) => emit('update:modelValue', v)"
  >
    <p class="hint">
      {{ t('majors.bulkHint') }}
    </p>
    <el-input
      v-model="text"
      type="textarea"
      :rows="10"
      :placeholder="t('majors.bulkPlaceholder')"
    />
    <el-select
      v-model="discipline"
      clearable
      :placeholder="t('majors.bulkDiscipline')"
      class="discipline"
    >
      <el-option
        v-for="d in DISCIPLINES"
        :key="d"
        :value="d"
        :label="t(`majors.disciplines.${d}`)"
      />
    </el-select>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">
        {{ t('common.cancel') }}
      </el-button>
      <el-button
        type="primary"
        :loading="saving"
        :disabled="!text.trim()"
        @click="save"
      >
        {{ t('majors.bulkAdd') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { bulkMajors, DISCIPLINES } from '@/api/major'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean], saved: [] }>()

const { t } = useI18n()
const text = ref('')
const discipline = ref('')
const saving = ref(false)

watch(() => props.modelValue, (open) => {
  if (open) {
    text.value = ''
    discipline.value = ''
  }
})

async function save() {
  saving.value = true
  try {
    const result = await bulkMajors(text.value, discipline.value || null)
    ElMessage.success(t('majors.bulkDone', { created: result.created, total: result.majors.length }))
    emit('update:modelValue', false)
    emit('saved')
  }
  finally {
    saving.value = false
  }
}
</script>

<style scoped>
.hint { margin: 0 0 8px; color: var(--el-text-color-secondary); font-size: 13px; }
.discipline { margin-top: 12px; width: 100%; }
</style>
