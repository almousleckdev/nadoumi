<template>
  <div class="nad-page">
    <PageHeader
      :title="t('news.title')"
      :subtitle="t('news.subtitle')"
    >
      <template #actions>
        <el-button
          v-if="userStore.hasPerm('nad:article:create')"
          type="primary"
          :icon="Plus"
          @click="router.push('/news/new')"
        >
          {{ t('news.new') }}
        </el-button>
      </template>
    </PageHeader>

    <FilterBar
      :dirty="dirty"
      @clear="clearFilters"
    >
      <SearchInput
        v-model="filters.q"
        :placeholder="t('news.searchPlaceholder')"
        @search="reload"
      />
      <el-select
        v-model="filters.status"
        :placeholder="t('news.status')"
        clearable
        style="width: 160px"
        @change="reload"
      >
        <el-option
          v-for="s in ARTICLE_STATUSES"
          :key="s"
          :label="t(`news.statusMap.${s}`)"
          :value="s"
        />
      </el-select>
      <el-select
        v-model="filters.language"
        :placeholder="t('news.language')"
        clearable
        style="width: 140px"
        @change="reload"
      >
        <el-option
          v-for="l in ARTICLE_LANGUAGES"
          :key="l"
          :label="t(`news.languageMap.${l}`)"
          :value="l"
        />
      </el-select>
    </FilterBar>

    <DataTable
      storage-key="news"
      :columns="columns"
      :rows="rows"
      :loading="loading"
      :error="error"
      :total="total"
      :page="page"
      :page-size="size"
      :clickable-rows="userStore.hasPerm('nad:article:view')"
      :empty-title="t('news.emptyTitle')"
      :empty-description="t('news.emptyDesc')"
      @update:page="(p: number) => { page = p; load() }"
      @update:page-size="(s: number) => { size = s; page = 0; load() }"
      @row-click="(row) => open(row as Article)"
      @retry="load"
    >
      <template #cell-title="{ row }">
        <div class="n-title">
          <span class="n-title__text">{{ row.title }}</span>
          <span class="n-title__ref">#{{ row.id }} · {{ row.slug }}</span>
        </div>
      </template>
      <template #cell-status="{ value }">
        <StatusBadge
          :status="value"
          :map="{ PUBLISHED: 'success', DRAFT: 'neutral', UNPUBLISHED: 'warning' }"
          :label="t(`news.statusMap.${value}`)"
        />
      </template>
      <template #cell-publishedAt="{ value }">
        {{ value ? formatDateTime(value) : '' }}
      </template>
      <template #cell-actions="{ row }">
        <el-button
          v-if="userStore.hasPerm('nad:article:publish') && (row as Article).status !== 'PUBLISHED' && (row as Article).coverMediaId"
          link
          size="small"
          type="primary"
          @click.stop="onPublish(row as Article)"
        >
          {{ t('news.publish') }}
        </el-button>
        <el-button
          v-if="userStore.hasPerm('nad:article:publish') && (row as Article).status === 'PUBLISHED'"
          link
          size="small"
          @click.stop="onUnpublish(row as Article)"
        >
          {{ t('news.unpublish') }}
        </el-button>
        <el-button
          v-if="userStore.hasPerm('nad:article:remove') && (row as Article).status !== 'PUBLISHED'"
          link
          size="small"
          type="danger"
          @click.stop="onDelete(row as Article)"
        >
          {{ t('common.delete') }}
        </el-button>
      </template>
    </DataTable>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  ARTICLE_LANGUAGES, ARTICLE_STATUSES, deleteArticle, listArticles, publishArticle, unpublishArticle,
  type Article,
} from '@/api/news'
import { formatDateTime } from '@/utils/date'
import { useUserStore } from '@/stores/user'
import { useConfirm } from '@/composables/useConfirm'
import { usePagedList } from '@/composables/usePagedList'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/ui/FilterBar.vue'
import SearchInput from '@/components/ui/SearchInput.vue'
import DataTable from '@/components/ui/DataTable.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import type { DataTableColumn } from '@/components/ui/types'

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
const { confirm } = useConfirm()

const emptyFilters = () => ({ q: '', status: '', language: '' })
const { rows, total, loading, error, filters, page, size, dirty, load, reload, clearFilters } =
  usePagedList<Article, ReturnType<typeof emptyFilters>>({
    emptyFilters,
    firstPage: 0,
    size: 20,
    fetch: (f, { page, size }) => listArticles({
      q: f.q || undefined,
      status: f.status || undefined,
      language: f.language || undefined,
      page,
      size,
    }),
  })

const columns: DataTableColumn[] = [
  { prop: 'title', label: t('news.articleTitle'), minWidth: 280 },
  { prop: 'language', label: t('news.language'), width: 100 },
  { prop: 'status', label: t('news.status'), width: 130 },
  { prop: 'commentCount', label: t('news.comments'), width: 110 },
  { prop: 'publishedAt', label: t('news.publishedAt'), width: 170 },
  { prop: 'actions', label: t('common.actions'), width: 190, align: 'right' },
]

function open(a: Article) {
  router.push(`/news/${a.id}`)
}

async function onPublish(a: Article) {
  await publishArticle(a.id)
  ElMessage.success(t('news.published'))
  load()
}

async function onUnpublish(a: Article) {
  const ok = await confirm({
    title: t('news.unpublishTitle'),
    message: t('news.unpublishConfirm', { title: a.title }),
    confirmText: t('news.unpublish'),
    tone: 'danger',
  })
  if (!ok) return
  await unpublishArticle(a.id)
  ElMessage.success(t('news.unpublished'))
  load()
}

async function onDelete(a: Article) {
  const ok = await confirm({
    title: t('news.deleteTitle'),
    message: t('news.deleteConfirm', { title: a.title }),
    confirmText: t('common.delete'),
    tone: 'danger',
  })
  if (!ok) return
  await deleteArticle(a.id)
  ElMessage.success(t('common.deleted'))
  load()
}

onMounted(load)
</script>

<style scoped>
.n-title {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.n-title__text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.n-title__ref {
  font-size: 12px;
  color: var(--nad-ink-soft, #64748b);
}
</style>
