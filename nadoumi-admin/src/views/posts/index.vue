<template>
  <div class="nad-page">
    <PageHeader
      :title="t('posts.title')"
      :subtitle="t('posts.subtitle')"
    >
      <template #actions>
        <el-button
          v-if="userStore.hasPerm('system:post:add')"
          type="primary"
          :icon="Plus"
          @click="open()"
        >
          {{ t('posts.new') }}
        </el-button>
      </template>
    </PageHeader>

    <FilterBar
      :dirty="dirty"
      @clear="clearFilters"
    >
      <el-input
        v-model="filters.postName"
        :placeholder="t('posts.searchPlaceholder')"
        clearable
        style="width: 240px"
        @keyup.enter="reload"
        @clear="reload"
      />
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
      row-key="postId"
      @retry="load"
    >
      <template #cell-status="{ row }">
        <StatusBadge :status="row.status === '0' ? 'ACTIVE' : 'CLOSED'" />
      </template>
      <template #cell-actions="{ row }">
        <el-button
          link
          type="primary"
          @click="open(row.postId)"
        >
          {{ t('common.edit') }}
        </el-button>
        <el-button
          v-if="userStore.hasPerm('system:post:remove')"
          link
          type="danger"
          @click="doDelete(row as SysPost)"
        >
          {{ t('common.delete') }}
        </el-button>
      </template>
    </DataTable>

    <Pagination
      v-model:page="page"
      v-model:size="size"
      :total="total"
      @change="load"
    />

    <Drawer
      v-model="drawerOpen"
      :title="t(editing ? 'posts.edit' : 'posts.new')"
      :saving="saving"
      @save="save"
    >
      <el-form
        v-if="drawerOpen"
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
      >
        <el-form-item
          :label="t('posts.code')"
          prop="postCode"
        >
          <el-input v-model="form.postCode" />
        </el-form-item>
        <el-form-item
          :label="t('posts.name')"
          prop="postName"
        >
          <el-input v-model="form.postName" />
        </el-form-item>
        <el-form-item :label="t('posts.sort')">
          <el-input-number
            v-model="form.postSort"
            :min="0"
          />
        </el-form-item>
        <el-form-item :label="t('posts.statusLabel')">
          <el-switch
            v-model="form.status"
            active-value="0"
            inactive-value="1"
          />
        </el-form-item>
        <el-form-item :label="t('posts.remark')">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="2"
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
import StatusBadge from '@/components/ui/StatusBadge.vue'
import { useConfirm } from '@/composables/useConfirm'
import { useUserStore } from '@/stores/user'
import {
  listPosts, getPost, createPost, updatePost, deletePosts, type SysPost,
} from '@/api/system'
import { usePagedList } from '@/composables/usePagedList'

const { t } = useI18n()
const { confirm } = useConfirm()
const userStore = useUserStore()

const { rows, total, loading, error, filters, page, size, dirty, load, reload, clearFilters } =
  usePagedList<SysPost, { postName: string }>({
    emptyFilters: () => ({ postName: '' }),
    firstPage: 1,
    size: 10,
    fetch: (f, { page, size }) => listPosts({
      postName: f.postName || undefined,
      pageNum: page,
      pageSize: size,
    }),
  })

const drawerOpen = ref(false)
const saving = ref(false)
const editing = ref<number | undefined>()
const formRef = ref<FormInstance>()
const blank = () => ({ postCode: '', postName: '', postSort: 0, status: '0', remark: '' })
const form = reactive<Partial<SysPost>>(blank())
const rules = {
  postCode: [{ required: true, trigger: 'blur', message: t('common.required') }],
  postName: [{ required: true, trigger: 'blur', message: t('common.required') }],
}

const columns = computed(() => [
  { prop: 'postCode', label: t('posts.code'), minWidth: 140 },
  { prop: 'postName', label: t('posts.name'), minWidth: 160 },
  { prop: 'postSort', label: t('posts.sort'), width: 80, align: 'center' as const },
  { prop: 'status', label: t('posts.statusLabel'), width: 100, align: 'center' as const },
  { prop: 'actions', label: t('common.actions'), width: 150, align: 'right' as const },
])

async function open(id?: number) {
  editing.value = id
  Object.assign(form, blank())
  drawerOpen.value = true
  if (id != null) Object.assign(form, (await getPost(id)).data)
}

async function save() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (editing.value != null) await updatePost(form)
    else await createPost(form)
    ElMessage.success(t('common.saved'))
    drawerOpen.value = false
    load()
  }
  finally {
    saving.value = false
  }
}

async function doDelete(row: SysPost) {
  if (!(await confirm({
    title: t('posts.deleteTitle'),
    message: t('posts.deleteConfirm', { name: row.postName }),
    tone: 'danger',
  }))) return
  await deletePosts([row.postId])
  ElMessage.success(t('common.deleted'))
  load()
}

onMounted(load)
</script>
