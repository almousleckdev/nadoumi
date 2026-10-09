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
        v-model="filters.name"
        :placeholder="t('applicant.searchPlaceholder')"
        @search="reload"
      />
      <el-select
        v-model="filters.status"
        :placeholder="t('applicant.status')"
        clearable
        style="width: 160px"
        @change="reload"
      >
        <el-option
          v-for="s in STATUSES"
          :key="s"
          :label="titleCase(s)"
          :value="s"
        />
      </el-select>
      <el-checkbox
        v-model="filters.incomplete"
        data-test="show-incomplete"
        @change="reload"
      >
        {{ t('applicant.showIncomplete') }}
      </el-checkbox>
      <el-input
        v-model="filters.nationality"
        :placeholder="t('applicant.nationality')"
        maxlength="2"
        style="width: 130px"
        @keyup.enter="reload"
      />
    </FilterBar>

    <div
      v-if="hiddenSignUps > 0 && !filters.incomplete"
      class="hidden-note"
      role="status"
      data-test="hidden-signups"
    >
      <span>{{ t('applicant.hiddenSignUps', { n: hiddenSignUps }) }}</span>
      <el-button
        link
        type="primary"
        data-test="show-hidden-signups"
        @click="showIncomplete"
      >
        {{ t('applicant.showThem') }}
      </el-button>
    </div>

    <DataTable
      storage-key="applicants"
      :columns="columns"
      :rows="rows"
      :loading="loading"
      :error="error"
      :total="total"
      :page="page"
      :page-size="size"
      clickable-rows
      :empty-title="t('applicant.emptyTitle')"
      :empty-description="t('applicant.emptyDesc')"
      @update:page="(p: number) => { page = p; load() }"
      @update:page-size="(s: number) => { size = s; page = 0; load() }"
      @retry="load"
      @row-click="(row) => goToDetail(String(row.publicId))"
    >
      <template #cell-givenName="{ row }">
        <div class="who">
          <Avatar
            :name="nameOf(row as Applicant)"
            :src="(row as Applicant).photoUrl || undefined"
            :size="30"
          />
          <span class="who__name">{{ nameOf(row as Applicant) }}</span>
        </div>
      </template>
      <template #cell-passportNo="{ value }">
        <span :class="{ masked: value === MASK }">{{ value ?? '' }}</span>
      </template>
      <template #cell-status="{ value }">
        <StatusBadge :status="value" />
      </template>
      <template #cell-createdAt="{ value }">
        {{ formatDate(value) }}
      </template>
      <template #cell-actions="{ row }">
        <el-button
          v-if="userStore.hasPerm('nad:applicant:delete')"
          link
          type="danger"
          data-test="delete-applicant"
          @click.stop="onDelete(row as Applicant)"
        >
          {{ t('applicant.delete') }}
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
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  deleteApplicant, createApplicant, getIncompleteSignUpCount, listApplicants,
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
import { usePagedList } from '@/composables/usePagedList'
import { formatDate } from '@/utils/date'
import { applicantDisplayName } from '@/utils/applicantLabels'
import { titleCase } from '@/utils/text'

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
const { confirm } = useConfirm()

const MASK = '••••'
const emptyFilters = () => ({ name: '', status: '', nationality: '', incomplete: false })
const { rows, total, loading, error, filters, page, size, dirty, load, reload, clearFilters } =
  usePagedList<Applicant, ReturnType<typeof emptyFilters>>({
    emptyFilters,
    firstPage: 0,
    size: 20,
    fetch: (f, { page, size }) => listApplicants({
      name: f.name || undefined,
      status: f.status || undefined,
      nationality: f.nationality || undefined,
      incomplete: f.incomplete || undefined,
      page,
      size,
    }),
  })

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

const nameOf = (a: Applicant) => applicantDisplayName(a, t('applicant.unnamed'))

const hiddenSignUps = ref(0)

async function loadHiddenSignUps() {
  try {
    hiddenSignUps.value = (await getIncompleteSignUpCount()).count
  }
  catch {
    hiddenSignUps.value = 0 // the banner is a convenience; the list itself still works
  }
}

function showIncomplete() {
  filters.incomplete = true
  void reload()
}

function goToDetail(id: string) {
  router.push(`/applicants/${id}`)
}

async function onDelete(row: Applicant) {
  const ok = await confirm({
    title: t('applicant.deleteTitle'),
    message: t('applicant.deleteConfirm', { name: nameOf(row) }),
    confirmText: t('applicant.delete'),
    tone: 'danger',
  })
  if (!ok) return
  try {
    await deleteApplicant(row.publicId)
    ElMessage.success(t('applicant.deleted'))
    await load()
    void loadHiddenSignUps()
  }
  catch (e) {
    ElMessage.error((e as Error)?.message || t('applicant.deleteFailed'))
  }
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
    reload()
  }
  finally {
    creating.value = false
  }
}

onMounted(() => { void load(); void loadHiddenSignUps() })
</script>

<style scoped>
.hidden-note {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding: 10px 14px;
  border: 1px solid var(--nad-line, #e5e7eb);
  border-radius: 8px;
  background: var(--nad-surface-muted, #f8fafc);
  font-size: 13px;
  color: var(--nad-ink-soft);
}
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
