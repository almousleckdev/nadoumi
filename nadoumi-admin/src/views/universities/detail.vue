<template>
  <div class="nad-page">
    <button
      class="back"
      type="button"
      @click="router.push('/universities')"
    >
      <el-icon><ArrowLeft /></el-icon>{{ t('university.backToList') }}
    </button>

    <LoadingState
      v-if="loading"
      :rows="5"
    />
    <ErrorState
      v-else-if="error"
      :message="error"
      @retry="load"
    />

    <template v-else-if="university">
      <PageHeader :title="university.name">
        <template #subtitle>
          {{ university.country }}<span v-if="university.city"> · {{ university.city }}</span>
        </template>
        <template #actions>
          <StatusBadge :status="university.status" />
          <el-button
            v-if="userStore.hasPerm('nad:university:edit')"
            size="small"
            :icon="Edit"
            @click="drawerOpen = true"
          >
            {{ t('common.edit') }}
          </el-button>
        </template>
      </PageHeader>

      <div class="nad-card detail__card">
        <DescriptionList :items="items">
          <template #website="{ value }">
            <a
              v-if="value"
              :href="String(value)"
              target="_blank"
              rel="noopener"
              class="link"
            >{{ value }}</a>
            <span
              v-else
              class="muted"
            >—</span>
          </template>
        </DescriptionList>
      </div>

      <UniversityDrawer
        v-model="drawerOpen"
        :university="university"
        @saved="load"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Edit } from '@element-plus/icons-vue'
import { getUniversity, type University } from '@/api/university'
import { useUserStore } from '@/stores/user'
import PageHeader from '@/components/PageHeader.vue'
import DescriptionList from '@/components/ui/DescriptionList.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import LoadingState from '@/components/ui/LoadingState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import type { DescriptionItem } from '@/components/ui/types'
import UniversityDrawer from './UniversityDrawer.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const id = route.params.id as string
const university = ref<University | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)
const drawerOpen = ref(false)

function fmtDate(v: string | null): string {
  if (!v) return '—'
  const d = new Date(v.replace(' ', 'T'))
  return Number.isNaN(d.getTime()) ? v : d.toLocaleString()
}

const items = computed<DescriptionItem[]>(() => {
  const u = university.value
  if (!u) return []
  return [
    { label: t('university.name'), value: u.name },
    { label: t('university.country'), value: u.country },
    { label: t('university.city'), value: u.city },
    { label: t('university.website'), value: u.website, slot: 'website' },
    { label: t('university.rankingTier'), value: u.rankingTier },
    { label: t('university.status'), value: u.status.charAt(0) + u.status.slice(1).toLowerCase() },
    { label: t('university.created'), value: fmtDate(u.createdAt) },
    { label: t('university.updated'), value: fmtDate(u.updatedAt) },
  ]
})

async function load() {
  loading.value = true
  error.value = null
  try {
    university.value = await getUniversity(id)
  }
  catch (e) {
    error.value = (e as Error)?.message || t('state.errorTitle')
  }
  finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.back {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: none;
  background: transparent;
  color: var(--nad-ink-soft);
  font-size: 13px;
  font-weight: 550;
  cursor: pointer;
  padding: 0;
  margin-bottom: 14px;
}
.back:hover {
  color: var(--nad-brand-700);
}
.detail__card {
  padding: 8px 20px;
}
.link {
  color: var(--nad-brand-700);
}
.muted {
  color: var(--nad-ink-faint);
}
</style>
