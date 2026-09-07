<template>
  <Drawer
    :model-value="modelValue"
    :title="t(isEdit ? 'tasks.edit' : 'tasks.new')"
    :saving="saving"
    size="480"
    @update:model-value="(v: boolean) => emit('update:modelValue', v)"
    @save="save"
    @closed="reset"
  >
    <el-form
      v-if="modelValue"
      ref="formRef"
      v-loading="loading"
      :model="form"
      :rules="rules"
      label-width="120px"
    >
      <el-form-item
        :label="t('tasks.taskTitle')"
        prop="title"
      >
        <el-input v-model="form.title" />
      </el-form-item>
      <el-form-item :label="t('tasks.description')">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="4"
        />
      </el-form-item>
      <el-form-item :label="t('tasks.priority')">
        <el-radio-group v-model="form.priority">
          <el-radio-button value="LOW">
            {{ t('tasks.priorityMap.LOW') }}
          </el-radio-button>
          <el-radio-button value="MEDIUM">
            {{ t('tasks.priorityMap.MEDIUM') }}
          </el-radio-button>
          <el-radio-button value="HIGH">
            {{ t('tasks.priorityMap.HIGH') }}
          </el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item :label="t('tasks.assignee')">
        <el-select
          v-model="form.assigneeUserId"
          clearable
          filterable
          style="width: 100%"
        >
          <el-option
            v-for="m in staff"
            :key="m.userId"
            :label="`${m.nickName} (${m.userName})`"
            :value="m.userId"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('tasks.dueDate')">
        <el-date-picker
          v-model="form.dueDate"
          type="date"
          value-format="YYYY-MM-DD"
          style="width: 100%"
        />
      </el-form-item>
    </el-form>
  </Drawer>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import Drawer from '@/components/ui/Drawer.vue'
import { getTask, createTask, updateTask, type TaskInput } from '@/api/hr'
import { listUsers, type SysUserRow } from '@/api/system'

const props = defineProps<{ modelValue: boolean, taskId?: number }>()
const emit = defineEmits<{ 'update:modelValue': [v: boolean], 'saved': [] }>()
const { t } = useI18n()

const isEdit = computed(() => props.taskId != null)
const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)
const staff = ref<SysUserRow[]>([])

const blank = (): TaskInput => ({
  title: '', description: '', priority: 'MEDIUM', assigneeUserId: null, dueDate: null,
})
const form = reactive<TaskInput>(blank())
const rules = { title: [{ required: true, trigger: 'blur', message: t('common.required') }] }

async function open() {
  Object.assign(form, blank())
  loading.value = true
  try {
    staff.value = (await listUsers({ userType: '00', pageNum: 1, pageSize: 200 })).rows
    if (isEdit.value) {
      const task = await getTask(props.taskId!)
      Object.assign(form, {
        title: task.title, description: task.description ?? '', priority: task.priority,
        assigneeUserId: task.assigneeUserId, dueDate: task.dueDate,
      })
    }
  }
  finally {
    loading.value = false
  }
}

async function save() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (isEdit.value) await updateTask(props.taskId!, { ...form })
    else await createTask({ ...form })
    ElMessage.success(t('common.saved'))
    emit('saved')
  }
  finally {
    saving.value = false
  }
}

function reset() { Object.assign(form, blank()) }
watch(() => props.modelValue, (o) => { if (o) open() })
</script>
