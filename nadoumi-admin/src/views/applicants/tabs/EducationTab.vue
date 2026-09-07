<template>
  <div>
    <div
      v-if="canEdit"
      class="tab-toolbar"
    >
      <el-button
        size="small"
        :icon="Plus"
        @click="openAdd"
      >
        {{ t('applicant.addEducation') }}
      </el-button>
    </div>

    <StatePanel
      :loading="list.loading.value"
      :error="list.error.value"
      :empty="list.items.value.length === 0"
      :empty-title="t('applicant.noEducation')"
      @retry="list.reload"
    >
      <el-table :data="list.items.value">
        <el-table-column
          :label="t('applicant.institution')"
          prop="institution"
          min-width="200"
        />
        <el-table-column
          :label="t('applicant.level')"
          prop="level"
          width="130"
        />
        <el-table-column
          :label="t('applicant.field')"
          prop="field"
          min-width="150"
        />
        <el-table-column
          :label="t('applicant.gpa')"
          width="110"
        >
          <template #default="{ row }">
            {{ row.gpa != null ? `${row.gpa}${row.gpaScale ? ' / ' + row.gpaScale : ''}` : '' }}
          </template>
        </el-table-column>
        <el-table-column
          :label="t('applicant.period')"
          width="190"
        >
          <template #default="{ row }">
            {{ period(row.startDate, row.endDate) }}
          </template>
        </el-table-column>
        <el-table-column
          v-if="canEdit"
          width="110"
          align="right"
        >
          <template #default="{ row }">
            <el-button
              link
              size="small"
              @click="openEdit(row as Education)"
            >
              {{ t('common.edit') }}
            </el-button>
            <el-button
              link
              size="small"
              type="danger"
              @click="onDelete(row as Education)"
            >
              {{ t('common.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </StatePanel>

    <Drawer
      v-model="open"
      :title="editing ? t('applicant.editEducation') : t('applicant.addEducation')"
      :saving="saving"
      @save="save"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
      >
        <el-form-item
          :label="t('applicant.institution')"
          prop="institution"
        >
          <el-input v-model="form.institution" />
        </el-form-item>
        <el-form-item
          :label="t('applicant.level')"
          prop="level"
        >
          <el-input
            v-model="form.level"
            placeholder="BSC / MSC / PHD…"
          />
        </el-form-item>
        <el-form-item
          :label="t('applicant.field')"
          prop="field"
        >
          <el-input v-model="form.field" />
        </el-form-item>
        <div class="row2">
          <el-form-item :label="t('applicant.gpa')">
            <el-input
              v-model="form.gpa"
              type="number"
            />
          </el-form-item>
          <el-form-item :label="t('applicant.gpaScale')">
            <el-input
              v-model="form.gpaScale"
              type="number"
            />
          </el-form-item>
        </div>
        <div class="row2">
          <el-form-item :label="t('applicant.startDate')">
            <el-date-picker
              v-model="form.startDate"
              type="date"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item :label="t('applicant.endDate')">
            <el-date-picker
              v-model="form.endDate"
              type="date"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </el-form-item>
        </div>
      </el-form>
    </Drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  listEducation, addEducation, updateEducation, deleteEducation,
  type Education, type EducationInput,
} from '@/api/applicant'
import { useResourceList } from '@/composables/useResourceList'
import { useConfirm } from '@/composables/useConfirm'
import StatePanel from '@/components/ui/StatePanel.vue'
import Drawer from '@/components/ui/Drawer.vue'

const props = defineProps<{ id: string, canEdit: boolean }>()
const emit = defineEmits<{ count: [n: number] }>()

const { t } = useI18n()
const { confirm } = useConfirm()
const list = useResourceList<Education>(() => listEducation(props.id))

watch(list.items, v => emit('count', v.length))
onMounted(list.load)

function period(a: string | null, b: string | null): string {
  if (!a && !b) return ''
  const f = (d: string) => new Date(d).toLocaleDateString()
  return `${a ? f(a) : '…'} to ${b ? f(b) : t('applicant.present')}`
}

const open = ref(false)
const saving = ref(false)
const editing = ref<Education | null>(null)
const formRef = ref<FormInstance>()
const blank = { institution: '', level: '', field: '', gpa: '', gpaScale: '', startDate: '', endDate: '' }
const form = reactive({ ...blank })
const rules = {
  institution: [{ required: true, trigger: 'blur', message: t('applicant.required') }],
}

function openAdd() {
  editing.value = null
  Object.assign(form, blank)
  open.value = true
}
function openEdit(row: Education) {
  editing.value = row
  Object.assign(form, {
    institution: row.institution, level: row.level ?? '', field: row.field ?? '',
    gpa: row.gpa ?? '', gpaScale: row.gpaScale ?? '',
    startDate: row.startDate ?? '', endDate: row.endDate ?? '',
  })
  open.value = true
}

function payload(): EducationInput {
  return {
    institution: form.institution.trim(),
    level: form.level || null,
    field: form.field || null,
    gpa: form.gpa === '' ? null : Number(form.gpa),
    gpaScale: form.gpaScale === '' ? null : Number(form.gpaScale),
    startDate: form.startDate || null,
    endDate: form.endDate || null,
  }
}

async function save() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (editing.value) await updateEducation(props.id, editing.value.id, payload())
    else await addEducation(props.id, payload())
    ElMessage.success(t('common.saved'))
    open.value = false
    await list.reload()
  }
  finally {
    saving.value = false
  }
}

async function onDelete(row: Education) {
  const ok = await confirm({
    message: t('applicant.deleteEducationConfirm', { name: row.institution }),
    confirmText: t('common.delete'),
    tone: 'danger',
  })
  if (!ok) return
  await deleteEducation(props.id, row.id)
  ElMessage.success(t('common.deleted'))
  await list.reload()
}
</script>

<style scoped>
.tab-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}
.row2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
</style>
