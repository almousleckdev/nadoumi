<template>
  <div class="nad-card sec">
    <div class="sec__head">
      <h3 class="sec__title">
        {{ t('department.sectionTitle') }}
      </h3>
      <el-button
        v-if="can('nad:department:add')"
        size="small"
        :icon="Plus"
        @click="openCreate"
      >
        {{ t('department.add') }}
      </el-button>
    </div>
    <p class="sec__hint">
      {{ t('department.sectionHint') }}
    </p>

    <el-table
      v-if="rows.length"
      v-loading="loading"
      :data="rows"
    >
      <el-table-column
        :label="t('department.name')"
        prop="name"
        min-width="200"
      />
      <el-table-column
        :label="t('department.nameCn')"
        prop="nameCn"
        min-width="160"
      />
      <el-table-column
        :label="t('department.majors')"
        width="110"
        align="center"
      >
        <template #default="{ row }">
          {{ (row as Department).programCount }}
        </template>
      </el-table-column>
      <el-table-column
        :label="t('common.actions')"
        width="140"
        align="right"
      >
        <template #default="{ row }">
          <el-button
            v-if="can('nad:department:edit')"
            link
            size="small"
            @click="openEdit(row as Department)"
          >
            {{ t('common.edit') }}
          </el-button>
          <el-button
            v-if="can('nad:department:remove')"
            link
            size="small"
            type="danger"
            @click="onDelete(row as Department)"
          >
            {{ t('common.delete') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <p
      v-else
      class="muted"
    >
      {{ t('department.none') }}
    </p>

    <el-dialog
      v-model="dialogOpen"
      :title="editing ? t('department.edit') : t('department.add')"
      width="420"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
      >
        <el-form-item
          :label="t('department.name')"
          prop="name"
        >
          <el-input
            v-model="form.name"
            maxlength="160"
          />
        </el-form-item>
        <el-form-item :label="t('department.nameCn')">
          <el-input
            v-model="form.nameCn"
            maxlength="160"
          />
        </el-form-item>
        <el-form-item :label="t('department.sortOrder')">
          <el-input-number
            v-model="form.sortOrder"
            :min="0"
            controls-position="right"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">
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
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useConfirm } from '@/composables/useConfirm'
import { useUserStore } from '@/stores/user'
import {
  listDepartments, createDepartment, updateDepartment, deleteDepartment, type Department,
} from '@/api/university'

const props = defineProps<{ universityId: number }>()
const can = (p: string) => userStore.hasPerm(p) || userStore.hasPerm('nad:university:edit')
const { t } = useI18n()
const { confirm } = useConfirm()
const userStore = useUserStore()

const rows = ref<Department[]>([])
const loading = ref(false)
const dialogOpen = ref(false)
const saving = ref(false)
const editing = ref<Department | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({ name: '', nameCn: '', sortOrder: 0 })
const rules = { name: [{ required: true, trigger: 'blur', message: t('common.required') }] }

async function load() {
  if (!can('nad:department:list')) return
  loading.value = true
  try {
    rows.value = await listDepartments(props.universityId)
  }
  catch { rows.value = [] }
  finally { loading.value = false }
}

function openCreate() {
  editing.value = null
  Object.assign(form, { name: '', nameCn: '', sortOrder: rows.value.length })
  dialogOpen.value = true
}
function openEdit(d: Department) {
  editing.value = d
  Object.assign(form, { name: d.name, nameCn: d.nameCn ?? '', sortOrder: d.sortOrder })
  dialogOpen.value = true
}

async function save() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const body = {
      name: form.name.trim(),
      nameCn: form.nameCn.trim() || null,
      sortOrder: form.sortOrder,
    }
    if (editing.value) await updateDepartment(props.universityId, editing.value.id, body)
    else await createDepartment(props.universityId, body)
    ElMessage.success(t('common.saved'))
    dialogOpen.value = false
    load()
  }
  finally {
    saving.value = false
  }
}

async function onDelete(d: Department) {
  if (d.programCount > 0) {
    ElMessage.warning(t('department.inUse'))
    return
  }
  if (!(await confirm({
    title: t('department.deleteTitle'),
    message: t('department.deleteConfirm', { name: d.name }),
    tone: 'danger',
  }))) return
  await deleteDepartment(props.universityId, d.id)
  ElMessage.success(t('common.deleted'))
  load()
}

onMounted(load)
defineExpose({ reload: load })
</script>

<style scoped>
.sec { padding: 16px 20px; margin-bottom: 16px; }
.sec__head { display: flex; align-items: center; justify-content: space-between; }
.sec__title {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--nad-ink-faint);
}
.sec__hint { margin: 6px 0 12px; font-size: 12px; color: var(--nad-ink-soft); }
.muted { color: var(--nad-ink-soft); font-size: 13px; margin: 0; }
</style>
