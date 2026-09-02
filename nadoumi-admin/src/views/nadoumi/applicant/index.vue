<template>
  <div class="nad-page">
    <div class="nad-toolbar">
      <el-input
        v-model="query.name"
        :placeholder="t('applicant.given') + ' / ' + t('applicant.family')"
        clearable
        style="width: 240px"
        @keyup.enter="reload"
      />
      <el-select v-model="query.status" :placeholder="t('applicant.status')" clearable style="width: 160px">
        <el-option v-for="s in STATUSES" :key="s" :label="s" :value="s" />
      </el-select>
      <el-button type="primary" @click="reload">{{ t('common.search') }}</el-button>
      <el-button @click="resetQuery">{{ t('common.reset') }}</el-button>
      <div class="spacer" />
      <el-button type="primary" :icon="Plus" @click="openCreate">{{ t('common.add') }}</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="givenName" :label="t('applicant.given')" />
      <el-table-column prop="familyName" :label="t('applicant.family')" />
      <el-table-column prop="nationality" :label="t('applicant.nationality')" width="120" />
      <el-table-column prop="email" :label="t('applicant.email')" />
      <el-table-column prop="phone" :label="t('applicant.phone')" width="150" />
      <el-table-column prop="status" :label="t('applicant.status')" width="120">
        <template #default="scope">
          <el-tag :type="statusType((scope.row as ApplicantRow).status)" disable-transitions>
            {{ (scope.row as ApplicantRow).status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('common.actions')" width="120">
        <template #default="scope">
          <el-button
            v-if="(scope.row as ApplicantRow).status !== 'ARCHIVED'"
            link
            type="danger"
            @click="archive(scope.row as ApplicantRow)"
          >
            {{ t('common.delete') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      style="margin-top: 12px; justify-content: flex-end"
      layout="prev, pager, next, total"
      :total="total"
      :page-size="query.size"
      :current-page="query.page + 1"
      @current-change="(p: number) => { query.page = p - 1; reload() }"
    />

    <el-dialog v-model="createVisible" :title="t('common.add')" width="480px">
      <el-form ref="createRef" :model="createForm" :rules="createRules" label-width="150px">
        <el-form-item :label="t('applicant.given')" prop="givenName">
          <el-input v-model="createForm.givenName" />
        </el-form-item>
        <el-form-item :label="t('applicant.family')" prop="familyName">
          <el-input v-model="createForm.familyName" />
        </el-form-item>
        <el-form-item :label="t('applicant.nationality')" prop="nationality">
          <el-input v-model="createForm.nationality" maxlength="2" placeholder="ISO alpha-2" />
        </el-form-item>
        <el-form-item :label="t('applicant.email')" prop="email">
          <el-input v-model="createForm.email" />
        </el-form-item>
        <el-form-item :label="t('applicant.invitedEmail')" prop="invitedEmail">
          <el-input v-model="createForm.invitedEmail" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreate">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { archiveApplicant, createApplicant, listApplicants, type ApplicantRow } from '@/api/applicant'

const { t } = useI18n()

const STATUSES = ['DRAFT', 'ACTIVE', 'UNLINKED', 'ARCHIVED']

const loading = ref(false)
const rows = ref<ApplicantRow[]>([])
const total = ref(0)
const query = reactive({ name: '', status: '', page: 0, size: 20 })

function statusType(s: string): 'success' | 'info' | 'warning' | undefined {
  if (s === 'ACTIVE') return 'success'
  if (s === 'ARCHIVED') return 'info'
  if (s === 'UNLINKED') return 'warning'
  return undefined
}

async function reload() {
  loading.value = true
  try {
    const res = await listApplicants({
      name: query.name || undefined,
      status: query.status || undefined,
      page: query.page,
      size: query.size,
    })
    rows.value = res.content
    total.value = res.totalElements
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.name = ''
  query.status = ''
  query.page = 0
  reload()
}

async function archive(row: ApplicantRow) {
  await ElMessageBox.confirm(`${t('common.delete')} #${row.id}?`, { type: 'warning' })
  await archiveApplicant(row.id)
  ElMessage.success(t('common.confirm'))
  reload()
}

// create
const createVisible = ref(false)
const creating = ref(false)
const createRef = ref<FormInstance>()
const createForm = reactive({ givenName: '', familyName: '', nationality: '', email: '', invitedEmail: '' })
const createRules = {
  givenName: [{ required: true, trigger: 'blur', message: t('applicant.given') }],
  familyName: [{ required: true, trigger: 'blur', message: t('applicant.family') }],
  invitedEmail: [{ required: true, type: 'email', trigger: 'blur', message: t('applicant.invitedEmail') }],
}

function openCreate() {
  createForm.givenName = createForm.familyName = createForm.nationality = ''
  createForm.email = createForm.invitedEmail = ''
  createVisible.value = true
}

async function submitCreate() {
  await createRef.value?.validate()
  creating.value = true
  try {
    await createApplicant({
      givenName: createForm.givenName,
      familyName: createForm.familyName,
      nationality: createForm.nationality || undefined,
      email: createForm.email || undefined,
      invitedEmail: createForm.invitedEmail,
    })
    ElMessage.success(t('applicant.createdOk'))
    createVisible.value = false
    reload()
  } finally {
    creating.value = false
  }
}

onMounted(reload)
</script>
