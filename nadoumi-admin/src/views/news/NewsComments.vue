<template>
  <div class="nc">
    <p
      v-if="!rows.length && !loading"
      class="nc__empty"
    >
      {{ t('news.noComments') }}
    </p>
    <ul
      v-else
      v-loading="loading"
      class="nc__list"
    >
      <li
        v-for="row in rows"
        :key="row.comment.id"
        class="nc__item"
        :style="{ marginInlineStart: `${Math.min(row.depth, MAX_INDENT) * 24}px` }"
      >
        <div class="nc__head">
          <strong>{{ row.comment.authorName || t('news.unknownUser') }}</strong>
          <span class="nc__time">{{ formatDateTime(row.comment.createTime) }}</span>
          <el-tag
            v-if="row.comment.status === 'DELETED'"
            size="small"
            type="info"
            effect="plain"
            disable-transitions
          >
            {{ t('news.commentDeleted') }}
          </el-tag>
          <el-button
            v-else-if="userStore.hasPerm('nad:article:comment:remove')"
            link
            size="small"
            type="danger"
            @click="onDelete(row.comment)"
          >
            {{ t('common.delete') }}
          </el-button>
        </div>
        <p
          class="nc__body"
          :class="{ 'nc__body--deleted': row.comment.status === 'DELETED' }"
        >
          {{ row.comment.body }}
        </p>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { deleteArticleComment, listArticleComments, type ArticleComment } from '@/api/news'
import { formatDateTime } from '@/utils/date'
import { useUserStore } from '@/stores/user'
import { useConfirm } from '@/composables/useConfirm'
import { threadRows } from './commentThread'

const props = defineProps<{ articleId: string }>()
const emit = defineEmits<{ changed: [] }>()

// Deeper replies stop indenting so long threads stay inside the panel.
const MAX_INDENT = 5

const { t } = useI18n()
const userStore = useUserStore()
const { confirm } = useConfirm()

const loading = ref(false)
const comments = ref<ArticleComment[]>([])
const rows = computed(() => threadRows(comments.value))

async function load() {
  loading.value = true
  try {
    comments.value = await listArticleComments(props.articleId)
  }
  finally {
    loading.value = false
  }
}

async function onDelete(c: ArticleComment) {
  const ok = await confirm({
    title: t('news.deleteCommentTitle'),
    message: t('news.deleteCommentConfirm', { name: c.authorName ?? '' }),
    confirmText: t('common.delete'),
    tone: 'danger',
  })
  if (!ok) return
  await deleteArticleComment(c.id)
  ElMessage.success(t('common.deleted'))
  await load()
  emit('changed')
}

onMounted(load)
</script>

<style scoped>
.nc__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.nc__item {
  border-inline-start: 2px solid var(--el-border-color-light, #e5e7eb);
  padding-inline-start: 12px;
}
.nc__head {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}
.nc__time {
  color: var(--nad-ink-soft, #64748b);
}
.nc__body {
  margin: 4px 0 0;
  white-space: pre-line;
  overflow-wrap: anywhere;
}
.nc__body--deleted {
  color: var(--nad-ink-soft, #94a3b8);
  text-decoration: line-through;
}
.nc__empty {
  color: var(--nad-ink-soft, #64748b);
}
</style>
