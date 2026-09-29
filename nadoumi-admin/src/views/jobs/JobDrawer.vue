<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import Drawer from '@/components/ui/Drawer.vue'
import { createJob, getJob, updateJob, type SysJob } from '@/api/monitor'

const props = defineProps<{ modelValue: boolean, jobId?: number }>()
const emit = defineEmits<{ 'update:modelValue': [v: boolean], 'saved': [] }>()

const { t } = useI18n()

const saving = ref(false)
const formRef = ref<FormInstance>()
const blank = () => ({
  jobName: '', jobGroup: 'DEFAULT', invokeTarget: '', cronExpression: '',
  misfirePolicy: '3', concurrent: '1', status: '1',
})
const form = reactive<Partial<SysJob>>(blank())
const rules = {
  jobName: [{ required: true, trigger: 'blur', message: t('common.required') }],
  invokeTarget: [{ required: true, trigger: 'blur', message: t('common.required') }],
  cronExpression: [{ required: true, trigger: 'blur', message: t('common.required') }],
}

watch(() => props.modelValue, async (open) => {
  if (!open) return
  Object.assign(form, blank())
  if (props.jobId != null) Object.assign(form, (await getJob(props.jobId)).data)
}, { immediate: true })

async function save() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (props.jobId != null) await updateJob(form)
    else await createJob(form)
    ElMessage.success(t('common.saved'))
    emit('update:modelValue', false)
    emit('saved')
  }
  finally {
    saving.value = false
  }
}
</script>

<template>
  <Drawer
    :model-value="modelValue"
    :title="t(jobId != null ? 'jobs.edit' : 'jobs.new')"
    :saving="saving"
    size="460"
    @update:model-value="v => emit('update:modelValue', v)"
    @save="save"
  >
    <el-form
      v-if="modelValue"
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="130px"
    >
      <el-form-item
        :label="t('jobs.name')"
        prop="jobName"
      >
        <el-input v-model="form.jobName" />
      </el-form-item>
      <el-form-item :label="t('jobs.group')">
        <el-select
          v-model="form.jobGroup"
          style="width: 100%"
        >
          <el-option
            label="DEFAULT"
            value="DEFAULT"
          />
          <el-option
            label="SYSTEM"
            value="SYSTEM"
          />
        </el-select>
      </el-form-item>
      <el-form-item
        :label="t('jobs.invokeTarget')"
        prop="invokeTarget"
      >
        <el-input
          v-model="form.invokeTarget"
          placeholder="beanName.method('arg')"
        />
      </el-form-item>
      <el-form-item
        :label="t('jobs.cron')"
        prop="cronExpression"
      >
        <el-input
          v-model="form.cronExpression"
          placeholder="0/15 * * * * ?"
        />
      </el-form-item>
      <el-form-item :label="t('jobs.misfire')">
        <el-radio-group v-model="form.misfirePolicy">
          <el-radio value="1">
            {{ t('jobs.misfire1') }}
          </el-radio>
          <el-radio value="2">
            {{ t('jobs.misfire2') }}
          </el-radio>
          <el-radio value="3">
            {{ t('jobs.misfire3') }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item :label="t('jobs.concurrent')">
        <el-switch
          v-model="form.concurrent"
          active-value="0"
          inactive-value="1"
        />
      </el-form-item>
      <el-form-item :label="t('jobs.statusLabel')">
        <el-switch
          v-model="form.status"
          active-value="0"
          inactive-value="1"
        />
      </el-form-item>
    </el-form>
  </Drawer>
</template>
