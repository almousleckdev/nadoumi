<template>
  <div class="nad-page">
    <PageHeader
      :title="t('jobs.title')"
      :subtitle="t('jobs.subtitle')"
    >
      <template #actions>
        <el-button
          v-if="userStore.hasPerm('monitor:job:add')"
          type="primary"
          :icon="Plus"
          @click="open()"
        >
          {{ t('jobs.new') }}
        </el-button>
      </template>
    </PageHeader>

    <FilterBar
      :dirty="Boolean(filters.jobName || filters.status)"
      @clear="clearFilters"
    >
      <el-input
        v-model="filters.jobName"
        :placeholder="t('jobs.name')"
        clearable
        style="width: 220px"
        @keyup.enter="reload"
        @clear="reload"
      />
      <el-select
        v-model="filters.status"
        :placeholder="t('jobs.status')"
        clearable
        style="width: 140px"
        @change="reload"
      >
        <el-option
          :label="t('jobs.running')"
          value="0"
        />
        <el-option
          :label="t('jobs.paused')"
          value="1"
        />
      </el-select>
      <el-button
        :icon="Search"
        @click="reload"
      >
        {{ t('common.search') }}
      </el-button>
    </FilterBar>

    <DataTable
      :rows="rows"
      :columns="columns"
      :loading="loading"
      :error="error"
      row-key="jobId"
      @retry="load"
    >
      <template #cell-status="{ row }">
        <el-switch
          :model-value="row.status === '0'"
          :disabled="!userStore.hasPerm('monitor:job:changeStatus')"
          @change="(v) => toggle(row as SysJob, Boolean(v))"
        />
      </template>
      <template #cell-actions="{ row }">
        <el-button
          v-if="userStore.hasPerm('monitor:job:changeStatus')"
          link
          type="primary"
          @click="doRun(row as SysJob)"
        >
          {{ t('jobs.runOnce') }}
        </el-button>
        <el-button
          link
          type="primary"
          @click="open(row.jobId)"
        >
          {{ t('common.edit') }}
        </el-button>
        <el-button
          v-if="userStore.hasPerm('monitor:job:remove')"
          link
          type="danger"
          @click="doDelete(row as SysJob)"
        >
          {{ t('common.delete') }}
        </el-button>
      </template>
    </DataTable>

    <Pagination
      v-model:page="filters.pageNum"
      v-model:size="filters.pageSize"
      :total="total"
      @change="load"
    />

    <Drawer
      v-model="drawerOpen"
      :title="t(editing ? 'jobs.edit' : 'jobs.new')"
      :saving="saving"
      size="460"
      @save="save"
    >
      <el-form
        v-if="drawerOpen"
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
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/ui/FilterBar.vue'
import DataTable from '@/components/ui/DataTable.vue'
import Pagination from '@/components/ui/Pagination.vue'
import Drawer from '@/components/ui/Drawer.vue'
import { useConfirm } from '@/composables/useConfirm'
import { useUserStore } from '@/stores/user'
import {
  listJobs, getJob, createJob, updateJob, deleteJobs, changeJobStatus, runJob, type SysJob,
} from '@/api/monitor'

const { t } = useI18n()
const { confirm } = useConfirm()
const userStore = useUserStore()

const rows = ref<SysJob[]>([])
const total = ref(0)
const loading = ref(false)
const error = ref<string | null>(null)
const filters = reactive({ jobName: '', status: '', pageNum: 1, pageSize: 10 })

const drawerOpen = ref(false)
const saving = ref(false)
const editing = ref<number | undefined>()
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

const columns = computed(() => [
  { prop: 'jobName', label: t('jobs.name'), minWidth: 160 },
  { prop: 'jobGroup', label: t('jobs.group'), width: 100 },
  { prop: 'invokeTarget', label: t('jobs.invokeTarget'), minWidth: 200 },
  { prop: 'cronExpression', label: t('jobs.cron'), width: 150 },
  { prop: 'status', label: t('jobs.statusLabel'), width: 90, align: 'center' as const },
  { prop: 'actions', label: t('common.actions'), width: 210, align: 'right' as const },
])

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await listJobs({
      jobName: filters.jobName || undefined,
      status: filters.status || undefined,
      pageNum: filters.pageNum,
      pageSize: filters.pageSize,
    })
    rows.value = res.rows
    total.value = res.total
  }
  catch (e) {
    error.value = (e as Error)?.message || 'Could not load'
  }
  finally {
    loading.value = false
  }
}
function reload() { filters.pageNum = 1; load() }
function clearFilters() { filters.jobName = ''; filters.status = ''; reload() }

async function open(id?: number) {
  editing.value = id
  Object.assign(form, blank())
  drawerOpen.value = true
  if (id != null) Object.assign(form, (await getJob(id)).data)
}
async function save() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (editing.value != null) await updateJob(form)
    else await createJob(form)
    ElMessage.success(t('common.saved'))
    drawerOpen.value = false
    load()
  }
  finally {
    saving.value = false
  }
}
async function toggle(row: SysJob, running: boolean) {
  const next = running ? '0' : '1'
  try {
    await changeJobStatus(row.jobId, next)
    row.status = next
    ElMessage.success(t('common.saved'))
  }
  catch { /* toast in request.ts */ }
}
async function doRun(row: SysJob) {
  if (!(await confirm({ title: t('jobs.runOnce'), message: t('jobs.runConfirm', { name: row.jobName }) }))) return
  await runJob(row.jobId, row.jobGroup)
  ElMessage.success(t('jobs.triggered'))
}
async function doDelete(row: SysJob) {
  if (!(await confirm({ title: t('jobs.deleteTitle'), message: t('jobs.deleteConfirm', { name: row.jobName }), tone: 'danger' }))) return
  await deleteJobs([row.jobId])
  ElMessage.success(t('common.deleted'))
  load()
}

onMounted(load)
</script>
