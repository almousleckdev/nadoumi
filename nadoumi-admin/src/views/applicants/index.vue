<template>
  <div class="nad-page">
    <PageHeader
      :title="t('applicant.title')"
      :subtitle="t('applicant.subtitle')"
    >
      <template #actions>
        <el-button
          v-if="userStore.hasPerm('nad:applicant:create')"
          type="primary"
          :icon="Plus"
          @click="createOpen = true"
        >
          {{ t('applicant.new') }}
        </el-button>
      </template>
    </PageHeader>

    <FilterBar
      :dirty="dirty"
      @clear="clearFilters"
    >
      <SearchInput
        v-model="query.name"
        :placeholder="t('applicant.searchPlaceholder')"
        @search="applyFilters"
      />
      <el-select
        v-model="query.status"
        :placeholder="t('applicant.status')"
        clearable
        style="width: 160px"
        @change="applyFilters"
      >
        <el-option
          v-for="s in STATUSES"
          :key="s"
          :label="titleCase(s)"
          :value="s"
        />
      </el-select>
      <el-input
        v-model="query.nationality"
        :placeholder="t('applicant.nationality')"
        maxlength="2"
        style="width: 130px"
        @keyup.enter="applyFilters"
      />
    </FilterBar>

    <DataTable
      storage-key="applicants"
      :columns="columns"
      :rows="rows"
      :loading="loading"
      :error="error"
      :total="total"
      :page="query.page"
      :page-size="query.size"
      clickable-rows
      :empty-title="t('applicant.emptyTitle')"
      :empty-description="t('applicant.emptyDesc')"
      @update:page="(p: number) => { query.page = p; reload() }"
      @update:page-size="(s: number) => { query.size = s; query.page = 0; reload() }"
      @retry="reload"
      @row-click="(row) => goToDetail(Number(row.id))"
    >
      <template #cell-givenName="{ row }">
        <div class="who">
          <Avatar
            :name="`${row.givenName} ${row.familyName}`"
            :size="30"
          />
          <span class="who__name">{{ row.givenName }} {{ row.familyName }}</span>
        </div>
      </template>
      <template #cell-passportNo="{ value }">
        <span :class="{ masked: value === MASK }">{{ value ?? '' }}</span>
      </template>
      <template #cell-status="{ value }">
        <StatusBadge :status="value" />
      </template>
      <template #cell-createdAt="{ value }">
        {{ fmtDate(value) }}
      </template>
      <template #cell-actions="{ row }">
        <el-button
          v-if="row.status !== 'ARCHIVED' && userStore.hasPerm('nad:applicant:archive')"
          link
          type="danger"
          @click.stop="onArchive(row as Applicant)"
        >
          {{ t('applicant.archive') }}
        </el-button>
      </template>
    </DataTable>

    <Drawer
      :model-value="createOpen"
      :title="t('applicant.new')"
      :saving="creating"
      :save-label="t('applicant.create')"
      :size="480"
      @update:model-value="createOpen = $event"
      @save="submitCreate"
    >
      <el-form
        ref="createRef"
        :model="createForm"
        :rules="createRules"
        label-position="top"
      >
        <el-form-item
          :label="t('applicant.given')"
          prop="givenName"
        >
          <el-input v-model="createForm.givenName" />
        </el-form-item>
        <el-form-item
          :label="t('applicant.family')"
          prop="familyName"
        >
          <el-input v-model="createForm.familyName" />
        </el-form-item>
        <el-form-item
          :label="t('applicant.nationality')"
          prop="nationality"
        >
          <el-input
            v-model="createForm.nationality"
            maxlength="2"
            placeholder="ISO alpha-2 (e.g. MR)"
          />
        </el-form-item>
        <el-form-item
          :label="t('applicant.contactEmail')"
          prop="email"
        >
          <el-input v-model="createForm.email" />
        </el-form-item>
        <el-form-item
          :label="t('applicant.invitedEmail')"
          prop="invitedEmail"
        >
          <el-input
            v-model="createForm.invitedEmail"
            :placeholder="t('applicant.invitedEmailHint')"
          />
        </el-form-item>
      </el-form>
    </Drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  archiveApplicant, createApplicant, listApplicants,
  type Applicant, type ApplicantStatus,
} from '@/api/applicant'
import { useUserStore } from '@/stores/user'
import { useConfirm } from '@/composables/useConfirm'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/ui/FilterBar.vue'
import SearchInput from '@/components/ui/SearchInput.vue'
import DataTable from '@/components/ui/DataTable.vue'
import type { DataTableColumn } from '@/components/ui/types'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import Avatar from '@/components/ui/Avatar.vue'
import Drawer from '@/components/ui/Drawer.vue'

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
const { confirm } = useConfirm()

const MASK = '••••'
const STATUSES: ApplicantStatus[] = ['DRAFT', 'ACTIVE', 'UNLINKED', 'ARCHIVED']

const columns: DataTableColumn[] = [
  { prop: 'givenName', label: t('applicant.name'), minWidth: 200 },
  { prop: 'nationality', label: t('applicant.nationality'), width: 110, align: 'center' },
  { prop: 'email', label: t('applicant.email'), minWidth: 200 },
  { prop: 'passportNo', label: t('applicant.passport'), width: 130 },
  { prop: 'status', label: t('applicant.status'), width: 140 },
  { prop: 'createdAt', label: t('applicant.registered'), width: 170 },
  { prop: 'actions', label: '', width: 110, align: 'right' },
]

const loading = ref(false)
const error = ref<string | null>(null)
const rows = ref<Applicant[]>([])
const total = ref(0)
const query = reactive({ name: '', status: '', nationality: '', page: 0, size: 20 })

const dirty = computed(() => Boolean(query.name || query.status || query.nationality))

function titleCase(s: string) {
  return s.charAt(0) + s.slice(1).toLowerCase()
}
function fmtDate(v: string | null): string {
  if (!v) return ''
  const d = new Date(v.replace(' ', 'T'))
  return Number.isNaN(d.getTime()) ? v : d.toLocaleDateString()
}
function goToDetail(id: number) {
  router.push(`/applicants/${id}`)
}

async function reload() {
  loading.value = true
  error.value = null
  try {
    const res = await listApplicants({
      name: query.name || undefined,
      status: query.status || undefined,
      nationality: query.nationality || undefined,
      page: query.page,
      size: query.size,
    })
    rows.value = res.content
    total.value = res.totalElements
  }
  catch (e) {
    error.value = (e as Error)?.message || t('state.errorTitle')
  }
  finally {
    loading.value = false
  }
}

function applyFilters() {
  query.page = 0
  reload()
}
function clearFilters() {
  query.name = ''
  query.status = ''
  query.nationality = ''
  applyFilters()
}

async function onArchive(row: Applicant) {
  const ok = await confirm({
    title: t('applicant.archiveTitle'),
    message: t('applicant.archiveConfirm', { name: `${row.givenName} ${row.familyName}` }),
    confirmText: t('applicant.archive'),
    tone: 'danger',
  })
  if (!ok) return
  await archiveApplicant(row.id)
  ElMessage.success(t('applicant.archived'))
  reload()
}

// create
const createOpen = ref(false)
const creating = ref(false)
const createRef = ref<FormInstance>()
const createForm = reactive({ givenName: '', familyName: '', nationality: '', email: '', invitedEmail: '' })
const createRules = {
  givenName: [{ required: true, trigger: 'blur', message: t('applicant.required') }],
  familyName: [{ required: true, trigger: 'blur', message: t('applicant.required') }],
  invitedEmail: [{ required: true, type: 'email', trigger: 'blur', message: t('applicant.emailInvalid') }],
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
    createOpen.value = false
    Object.assign(createForm, { givenName: '', familyName: '', nationality: '', email: '', invitedEmail: '' })
    applyFilters()
  }
  finally {
    creating.value = false
  }
}

onMounted(reload)
</script>

<style scoped>
.who {
  display: flex;
  align-items: center;
  gap: 10px;
}
.who__name {
  font-weight: 550;
}
.masked {
  color: var(--nad-ink-faint);
  letter-spacing: 0.1em;
}
</style>
