<template>
  <el-dialog
    :model-value="modelValue"
    :title="major ? t('majors.edit') : t('majors.new')"
    width="460px"
    @update:model-value="(v: boolean) => emit('update:modelValue', v)"
    @closed="reset"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      @submit.prevent="save"
    >
      <el-form-item
        :label="t('majors.name')"
        prop="name"
      >
        <el-input
          v-model="form.name"
          maxlength="200"
        />
      </el-form-item>
      <el-form-item :label="t('majors.nameCn')">
        <el-input
          v-model="form.nameCn"
          maxlength="200"
        />
      </el-form-item>
      <el-form-item :label="t('majors.discipline')">
        <el-select
          v-model="form.discipline"
          clearable
          style="width: 100%"
        >
          <el-option
            v-for="d in DISCIPLINES"
            :key="d"
            :value="d"
            :label="t(`majors.disciplines.${d}`)"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">
        {{ t('common.cancel') }}
      </el-button>
      <el-button
        type="primary"
        :loading="saving"
        @click="save"
      >
        {{ t('common.save') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import { createMajor, updateMajor, DISCIPLINES, type Major } from '@/api/major'

const props = defineProps<{ modelValue: boolean, major: Major | null }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean], saved: [] }>()

const { t } = useI18n()
const formRef = ref<FormInstance>()
const saving = ref(false)
const form = reactive({ name: '', nameCn: '', discipline: '' })
const rules = { name: [{ required: true, message: () => t('majors.nameRequired'), trigger: 'blur' }] }

watch(() => props.modelValue, (open) => {
  if (!open) return
  form.name = props.major?.name ?? ''
  form.nameCn = props.major?.nameCn ?? ''
  form.discipline = props.major?.discipline ?? ''
})

function reset() {
  formRef.value?.clearValidate()
}

async function save() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const body = { name: form.name.trim(), nameCn: form.nameCn.trim() || null, discipline: form.discipline || null }
    if (props.major) await updateMajor(props.major.id, body)
    else await createMajor(body)
    ElMessage.success(t('common.saved'))
    emit('update:modelValue', false)
    emit('saved')
  }
  finally {
    saving.value = false
  }
}
</script>
