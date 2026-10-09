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
      <template #cell-cover="{ row }">
        <img
          v-if="(row as Article).coverUrl && !brokenCovers.has((row as Article).id)"
          class="n-thumb"
          :src="assetUrl((row as Article).coverUrl)"
          :alt="t('news.coverAlt', { title: (row as Article).title })"
          loading="lazy"
          @error="brokenCovers.add((row as Article).id)"
        >
        <span
          v-else
          class="n-thumb n-thumb--empty"
          :title="t('news.noCover')"
        >
          <el-icon :size="18"><Picture /></el-icon>
        </span>
      </template>
      <template #cell-title="{ row }">
        <div class="n-title">
          <span class="n-title__text">{{ row.title || t('news.untitled') }}</span>
          <span class="n-title__ref">/{{ row.slug }}</span>
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
        <div class="n-actions">
          <el-button
            link
            size="small"
            :icon="View"
            @click.stop="preview(row as Article)"
          >
            {{ t('news.preview') }}
          </el-button>
          <el-button
            v-if="userStore.hasPerm('nad:article:edit')"
            link
            size="small"
            type="primary"
            :icon="EditPen"
            @click.stop="open(row as Article)"
          >
            {{ t('news.editAction') }}
          </el-button>
          <el-dropdown
            trigger="click"
            @command="(cmd: string) => onCommand(cmd, row as Article)"
          >
            <el-button
              link
              size="small"
              :aria-label="t('news.moreActions')"
              @click.stop
            >
              <el-icon><MoreFilled /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                  v-if="canPublish && (row as Article).status !== 'PUBLISHED'"
                  command="publish"
                  :disabled="!(row as Article).coverMediaId"
                >
                  {{ (row as Article).coverMediaId ? t('news.publish') : t('news.publishNeedsCover') }}
                </el-dropdown-item>
                <el-dropdown-item
                  v-if="canPublish && (row as Article).status === 'PUBLISHED'"
                  command="unpublish"
                >
                  {{ t('news.unpublish') }}
                </el-dropdown-item>
                <el-dropdown-item
                  v-if="userStore.hasPerm('nad:article:remove')"
                  command="delete"
                  :disabled="(row as Article).status === 'PUBLISHED'"
                  divided
                >
                  <span :class="{ 'n-danger': (row as Article).status !== 'PUBLISHED' }">
                    {{ (row as Article).status === 'PUBLISHED' ? t('news.deleteNeedsUnpublish') : t('common.delete') }}
                  </span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </template>
    </DataTable>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { EditPen, MoreFilled, Picture, Plus, View } from '@element-plus/icons-vue'
import {
  ARTICLE_LANGUAGES, ARTICLE_STATUSES, deleteArticle, listArticles, publishArticle, unpublishArticle,
  type Article,
} from '@/api/news'
import { assetUrl } from '@/utils/asset'
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

const canPublish = computed(() => userStore.hasPerm('nad:article:publish'))
// covers whose image failed to load fall back to the placeholder instead of showing alt text
const brokenCovers = reactive(new Set<string>())

const columns: DataTableColumn[] = [
  { prop: 'cover', label: t('news.coverColumn'), width: 112, tooltip: false },
  { prop: 'title', label: t('news.articleTitle'), minWidth: 260 },
  { prop: 'language', label: t('news.language'), width: 100 },
  { prop: 'status', label: t('news.status'), width: 150 },
  { prop: 'likeCount', label: t('news.likes'), width: 90 },
  { prop: 'commentCount', label: t('news.comments'), width: 110 },
  { prop: 'publishedAt', label: t('news.publishedAt'), width: 170 },
  { prop: 'actions', label: t('common.actions'), width: 230, align: 'right', tooltip: false },
]

function open(a: Article) {
  router.push(`/news/${a.id}`)
}

/** The editor opens straight into its reader preview. */
function preview(a: Article) {
  router.push({ path: `/news/${a.id}`, query: { preview: '1' } })
}

function onCommand(command: string, a: Article) {
  if (command === 'publish') return void onPublish(a)
  if (command === 'unpublish') return void onUnpublish(a)
  if (command === 'delete') return void onDelete(a)
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
.n-thumb {
  display: block;
  width: 88px;
  height: 56px;
  border-radius: 6px;
  object-fit: cover;
  background: var(--el-fill-color-light);
}
.n-thumb--empty {
  display: grid;
  place-items: center;
  color: var(--el-text-color-placeholder);
  border: 1px dashed var(--el-border-color);
}
.n-actions {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.n-danger {
  color: var(--el-color-danger);
}
</style>
