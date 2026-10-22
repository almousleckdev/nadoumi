<template>
  <div class="nad-page">
    <PageHeader
      :title="t('scholarship.title')"
      :subtitle="t('scholarship.subtitle')"
    >
      <template #actions>
        <el-button
          v-if="userStore.hasPerm('nad:scholarship:create')"
          type="primary"
          :icon="Plus"
          @click="openCreate"
        >
          {{ t('scholarship.new') }}
        </el-button>
      </template>
    </PageHeader>

    <FilterBar
      :dirty="dirty"
      @clear="clearFilters"
    >
      <SearchInput
        v-model="filters.q"
        :placeholder="t('scholarship.searchPlaceholder')"
        @search="reload"
      />
      <el-input
        v-model="filters.country"
        :placeholder="t('scholarship.country')"
        maxlength="2"
        style="width: 110px"
        @keyup.enter="reload"
      />
      <el-select
        v-model="filters.funding"
        :placeholder="t('scholarship.fundingModel')"
        clearable
        style="width: 160px"
        @change="reload"
      >
        <el-option
          v-for="f in FUNDING"
          :key="f"
          :value="f"
          :label="t(`scholarship.funding.${f}`)"
        />
      </el-select>
      <el-select
        v-model="filters.publishStatus"
        :placeholder="t('scholarship.publishStatus')"
        clearable
        style="width: 150px"
        @change="reload"
      >
        <el-option
          value="DRAFT"
          label="DRAFT"
        />
        <el-option
          value="PUBLISHED"
          label="PUBLISHED"
        />
      </el-select>
    </FilterBar>

    <DataTable
      storage-key="scholarships"
      :columns="columns"
      :rows="rows"
      :loading="loading"
      :error="error"
      :total="total"
      :page="page"
      :page-size="size"
      clickable-rows
      :empty-title="t('scholarship.emptyTitle')"
      :empty-description="t('scholarship.emptyDesc')"
      @update:page="(p: number) => { page = p; load() }"
      @update:page-size="(s: number) => { size = s; page = 0; load() }"
      @retry="load"
      @row-click="(row) => router.push(`/scholarships/${row.view.id}`)"
    >
      <template #cell-media="{ row }">
        <div class="s-media">
          <img
            v-if="logoOf(row as Scholarship)"
            :src="logoOf(row as Scholarship)"
            alt=""
            class="s-media__logo"
            loading="lazy"
          >
          <img
            v-if="coverOf(row as Scholarship)"
            :src="coverOf(row as Scholarship)"
            alt=""
            class="s-media__cover"
            loading="lazy"
          >
          <span
            v-if="!logoOf(row as Scholarship) && !coverOf(row as Scholarship)"
            class="s-media__none"
          >&mdash;</span>
        </div>
      </template>
      <template #cell-referenceCode="{ row }">
        <span class="s-ref">{{ (row as Scholarship).view.referenceCode || '' }}</span>
      </template>
      <template #cell-title="{ row }">
        <div class="s-title">
          <span>{{ (row as Scholarship).view.title }}</span>
          <el-tag
            v-if="(row as Scholarship).view.hot"
            size="small"
            type="warning"
            effect="plain"
            disable-transitions
          >
            {{ t('scholarship.hot') }}
          </el-tag>
          <el-tag
            v-else-if="(row as Scholarship).view.featured"
            size="small"
            type="primary"
            effect="plain"
            disable-transitions
          >
            {{ t('scholarship.featured') }}
          </el-tag>
        </div>
      </template>
      <template #cell-country="{ row }">
        <span class="mono">{{ (row as Scholarship).view.country }}</span>
      </template>
      <template #cell-funding="{ row }">
        {{ t(`scholarship.funding.${(row as Scholarship).view.fundingModel}`) }}
      </template>
      <template #cell-levels="{ row }">
        <div class="s-tags">
          <el-tag
            v-for="l in (row as Scholarship).view.levels"
            :key="l"
            size="small"
            effect="plain"
            disable-transitions
          >
            {{ t(`scholarship.level.${l}`) }}
          </el-tag>
        </div>
      </template>
      <template #cell-categories="{ row }">
        <div class="s-tags">
          <el-tag
            v-for="c in (row as Scholarship).view.categories"
            :key="c"
            size="small"
            type="info"
            effect="plain"
            disable-transitions
          >
            {{ c }}
          </el-tag>
        </div>
      </template>
      <template #cell-publishStatus="{ row }">
        <StatusBadge
          :status="(row as Scholarship).publishStatus"
          :map="{ PUBLISHED: 'success', DRAFT: 'neutral' }"
        />
      </template>
      <template #cell-actions="{ row }">
        <el-button
          v-if="userStore.hasPerm('nad:scholarship:edit')"
          link
          size="small"
          @click.stop="openEdit(row as Scholarship)"
        >
          {{ t('common.edit') }}
        </el-button>
        <el-button
          v-if="userStore.hasPerm('nad:scholarship:remove')"
          link
          size="small"
          type="danger"
          @click.stop="onDelete(row as Scholarship)"
        >
          {{ t('common.delete') }}
        </el-button>
      </template>
    </DataTable>

    <ScholarshipDrawer
      v-model="drawerOpen"
      :scholarship="editing"
      @saved="onSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { listScholarships, deleteScholarship, type Scholarship } from '@/api/scholarship'
import { useUserStore } from '@/stores/user'
import { useConfirm } from '@/composables/useConfirm'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/ui/FilterBar.vue'
import SearchInput from '@/components/ui/SearchInput.vue'
import DataTable from '@/components/ui/DataTable.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import type { DataTableColumn } from '@/components/ui/types'
import ScholarshipDrawer from './ScholarshipDrawer.vue'
import { usePagedList } from '@/composables/usePagedList'
import { assetUrl } from '@/utils/asset'

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
const { confirm } = useConfirm()

const FUNDING = ['FULLY', 'PARTIAL', 'SELF'] as const
const emptyFilters = () => ({ q: '', country: '', funding: '', publishStatus: '' })
const { rows, total, loading, error, filters, page, size, dirty, load, reload, clearFilters } =
  usePagedList<Scholarship, ReturnType<typeof emptyFilters>>({
    emptyFilters,
    firstPage: 0,
    size: 20,
    fetch: (f, { page, size }) => listScholarships({
      q: f.q || undefined,
      country: f.country || undefined,
      funding: f.funding || undefined,
      publishStatus: f.publishStatus || undefined,
      page,
      size,
    }),
  })

const columns: DataTableColumn[] = [
  { prop: 'media', label: t('scholarship.secMedia'), width: 130 },
  { prop: 'referenceCode', label: t('scholarship.referenceCode'), width: 130 },
  { prop: 'title', label: t('scholarship.title'), minWidth: 240 },
  { prop: 'country', label: t('scholarship.country'), width: 80, align: 'center' },
  { prop: 'funding', label: t('scholarship.fundingModel'), width: 130 },
  { prop: 'levels', label: t('scholarship.levels'), minWidth: 200 },
  { prop: 'categories', label: t('scholarship.categories'), minWidth: 160 },
  { prop: 'publishStatus', label: t('scholarship.publishStatus'), width: 120 },
  { prop: 'actions', label: '', width: 130, align: 'right' },
]

const logoOf = (s: Scholarship) => assetUrl(s.view.heroUrl ?? s.view.heroImageUrl)
const coverOf = (s: Scholarship) => assetUrl(s.view.coverUrl ?? s.view.coverImageUrl)

const drawerOpen = ref(false)
const editing = ref<Scholarship | null>(null)
function openCreate() { editing.value = null; drawerOpen.value = true }
function openEdit(s: Scholarship) { editing.value = s; drawerOpen.value = true }
function onSaved() { load() }

async function onDelete(s: Scholarship) {
  const ok = await confirm({
    title: t('scholarship.deleteTitle'),
    message: t('scholarship.deleteConfirm', { name: s.view.title }),
    confirmText: t('common.delete'),
    tone: 'danger',
  })
  if (!ok) return
  await deleteScholarship(s.view.id)
  ElMessage.success(t('common.deleted'))
  load()
}

onMounted(load)
</script>

<style scoped>
.mono { font-variant-numeric: tabular-nums; letter-spacing: 0.03em; }
.s-title { display: flex; align-items: center; gap: 8px; }
.s-media { display: flex; align-items: center; gap: 6px; }
.s-media__logo { width: 36px; height: 36px; border-radius: 8px; object-fit: cover; border: 1px solid var(--nad-border, #e5e7eb); }
.s-media__cover { width: 64px; height: 36px; border-radius: 6px; object-fit: cover; border: 1px solid var(--nad-border, #e5e7eb); }
.s-media__none { color: var(--nad-ink-soft); }
.s-tags { display: flex; flex-wrap: wrap; gap: 4px; }
.s-ref { font-variant-numeric: tabular-nums; font-size: 12px; color: var(--nad-ink-soft); }
</style>
