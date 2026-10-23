<template>
  <div class="nad-page">
    <PageHeader
      :title="t('majors.title')"
      :subtitle="t('majors.subtitle')"
    >
      <template #actions>
        <el-button
          v-if="userStore.hasPerm('nad:major:add')"
          @click="bulkOpen = true"
        >
          {{ t('majors.bulk') }}
        </el-button>
        <el-button
          v-if="userStore.hasPerm('nad:major:add')"
          type="primary"
          :icon="Plus"
          @click="openCreate"
        >
          {{ t('majors.new') }}
        </el-button>
      </template>
    </PageHeader>

    <div class="filters">
      <el-input
        v-model="q"
        clearable
        :placeholder="t('majors.search')"
        :prefix-icon="Search"
        class="filters__q"
        @input="onSearch"
        @clear="onSearch"
      />
      <el-select
        v-model="discipline"
        clearable
        :placeholder="t('majors.discipline')"
        class="filters__discipline"
        @change="onSearch"
      >
        <el-option
          v-for="d in DISCIPLINES"
          :key="d"
          :value="d"
          :label="t(`majors.disciplines.${d}`)"
        />
      </el-select>
    </div>

    <ErrorState
      v-if="error"
      :message="error"
      @retry="load"
    />
    <template v-else>
      <el-table
        v-loading="loading"
        :data="rows"
      >
        <el-table-column
          :label="t('majors.name')"
          prop="name"
          min-width="220"
        />
        <el-table-column
          :label="t('majors.nameCn')"
          prop="nameCn"
          min-width="160"
        />
        <el-table-column
          :label="t('majors.discipline')"
          width="160"
        >
          <template #default="{ row }">
            {{ row.discipline ? t(`majors.disciplines.${row.discipline}`, row.discipline) : '' }}
          </template>
        </el-table-column>
        <el-table-column
          :label="t('majors.programs')"
          prop="programCount"
          width="110"
          align="center"
        />
        <el-table-column
          :label="t('common.actions')"
          width="230"
          align="right"
        >
          <template #default="{ row }">
            <el-button
              v-if="userStore.hasPerm('nad:major:edit')"
              link
              type="primary"
              @click="openEdit(row as Major)"
            >
              {{ t('common.edit') }}
            </el-button>
            <el-button
              v-if="userStore.hasPerm('nad:major:edit')"
              link
              type="primary"
              @click="openMerge(row as Major)"
            >
              {{ t('majors.merge') }}
            </el-button>
            <el-button
              v-if="userStore.hasPerm('nad:major:remove')"
              link
              type="danger"
              :disabled="row.programCount > 0"
              :title="row.programCount > 0 ? t('majors.inUse') : undefined"
              @click="doDelete(row as Major)"
            >
              {{ t('common.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-if="total > size"
        v-model:current-page="pageNo"
        class="pager"
        layout="prev, pager, next, total"
        :page-size="size"
        :total="total"
        @current-change="load"
      />
    </template>

    <MajorDialog
      v-model="dialogOpen"
      :major="editing"
      @saved="load"
    />
    <MajorBulkDialog
      v-model="bulkOpen"
      @saved="load"
    />

    <el-dialog
      v-model="mergeOpen"
      :title="t('majors.mergeTitle')"
      width="460px"
    >
      <p class="hint">
        {{ t('majors.mergeHint', { name: merging?.name ?? '' }) }}
      </p>
      <el-select
        v-model="mergeInto"
        filterable
        remote
        :remote-method="searchTargets"
        :loading="targetsLoading"
        :placeholder="t('majors.mergeInto')"
        style="width: 100%"
      >
        <el-option
          v-for="m in targets"
          :key="m.id"
          :value="m.id"
          :label="m.nameCn ? `${m.name} · ${m.nameCn}` : m.name"
        />
      </el-select>
      <template #footer>
        <el-button @click="mergeOpen = false">
          {{ t('common.cancel') }}
        </el-button>
        <el-button
          type="primary"
          :disabled="!mergeInto"
          :loading="merges"
          @click="doMerge"
        >
          {{ t('majors.merge') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import { useConfirm } from '@/composables/useConfirm'
import { useUserStore } from '@/stores/user'
import { deleteMajor, DISCIPLINES, listMajors, mergeMajor, type Major } from '@/api/major'
import MajorBulkDialog from './MajorBulkDialog.vue'
import MajorDialog from './MajorDialog.vue'

const SEARCH_DEBOUNCE_MS = 300
const TARGET_LIMIT = 20

const { t } = useI18n()
const { confirm } = useConfirm()
const userStore = useUserStore()

const rows = ref<Major[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const q = ref('')
const discipline = ref('')
const pageNo = ref(1)
const size = 20
const total = ref(0)

const dialogOpen = ref(false)
const bulkOpen = ref(false)
const editing = ref<Major | null>(null)

const mergeOpen = ref(false)
const merging = ref<Major | null>(null)
const mergeInto = ref<number | null>(null)
const targets = ref<Major[]>([])
const targetsLoading = ref(false)
const merges = ref(false)

let searchTimer: ReturnType<typeof setTimeout> | undefined

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await listMajors({ q: q.value || undefined, discipline: discipline.value || undefined, page: pageNo.value - 1, size })
    rows.value = res.content
    total.value = res.totalElements
  }
  catch (e) {
    error.value = (e as Error)?.message || 'Could not load'
  }
  finally {
    loading.value = false
  }
}

function onSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    pageNo.value = 1
    load()
  }, SEARCH_DEBOUNCE_MS)
}

function openCreate() {
  editing.value = null
  dialogOpen.value = true
}

function openEdit(major: Major) {
  editing.value = major
  dialogOpen.value = true
}

async function doDelete(major: Major) {
  if (!(await confirm({
    title: t('majors.deleteTitle'),
    message: t('majors.deleteConfirm', { name: major.name }),
    tone: 'danger',
  }))) return
  await deleteMajor(major.id)
  ElMessage.success(t('common.deleted'))
  await load()
}

async function searchTargets(query: string) {
  targetsLoading.value = true
  try {
    const res = await listMajors({ q: query || undefined, size: TARGET_LIMIT })
    targets.value = res.content.filter(m => m.id !== merging.value?.id)
  }
  finally {
    targetsLoading.value = false
  }
}

async function openMerge(major: Major) {
  merging.value = major
  mergeInto.value = null
  mergeOpen.value = true
  await searchTargets('')
}

async function doMerge() {
  if (!merging.value || !mergeInto.value) return
  merges.value = true
  try {
    await mergeMajor(merging.value.id, mergeInto.value)
    ElMessage.success(t('majors.merged'))
    mergeOpen.value = false
    await load()
  }
  finally {
    merges.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.filters { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.filters__q { width: 320px; max-width: 100%; }
.filters__discipline { width: 200px; }
.pager { margin-top: 16px; justify-content: flex-end; }
.hint { margin: 0 0 12px; color: var(--el-text-color-secondary); font-size: 13px; }
</style>
