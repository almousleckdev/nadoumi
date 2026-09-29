<template>
  <div class="nad-page">
    <button
      class="back"
      type="button"
      @click="router.push('/scholarships')"
    >
      <el-icon><ArrowLeft /></el-icon>{{ t('scholarship.backToList') }}
    </button>

    <LoadingState
      v-if="loading"
      :rows="6"
    />
    <ErrorState
      v-else-if="error"
      :message="error"
      @retry="load"
    />

    <template v-else-if="s">
      <PageHeader :title="s.view.title">
        <template #subtitle>
          {{ [s.view.city, s.view.province, s.view.country].filter(Boolean).join(' · ') }}
          · {{ t(`scholarship.funding.${s.view.fundingModel}`) }}
        </template>
        <template #actions>
          <StatusBadge :status="s.status" />
          <StatusBadge
            :status="s.publishStatus"
            :map="{ PUBLISHED: 'success', DRAFT: 'neutral' }"
          />
          <el-button
            v-if="userStore.hasPerm('nad:scholarship:edit')"
            size="small"
            :icon="Edit"
            @click="drawerOpen = true"
          >
            {{ t('common.edit') }}
          </el-button>
        </template>
      </PageHeader>

      <OverviewSections :view="s.view" />
      <CostSections :view="s.view" />
      <ConditionsSections :view="s.view" />
      <InternalPanel
        ref="internalPanel"
        :scholarship-id="id"
      />
    </template>

    <ScholarshipDrawer
      v-model="drawerOpen"
      :scholarship="s"
      @saved="onSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Edit } from '@element-plus/icons-vue'
import { getScholarship, type Scholarship } from '@/api/scholarship'
import { useUserStore } from '@/stores/user'
import PageHeader from '@/components/PageHeader.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import LoadingState from '@/components/ui/LoadingState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import ScholarshipDrawer from './ScholarshipDrawer.vue'
import OverviewSections from './detail/OverviewSections.vue'
import CostSections from './detail/CostSections.vue'
import ConditionsSections from './detail/ConditionsSections.vue'
import InternalPanel from './detail/InternalPanel.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const id = computed(() => String(route.params.id))
const loading = ref(true)
const error = ref<string | null>(null)
const s = ref<Scholarship | null>(null)
const drawerOpen = ref(false)
const internalPanel = ref<InstanceType<typeof InternalPanel>>()

async function load() {
  loading.value = true
  error.value = null
  try {
    s.value = await getScholarship(id.value)
    await internalPanel.value?.reload()
  }
  catch (e) {
    error.value = (e as Error)?.message || t('state.errorTitle')
  }
  finally {
    loading.value = false
  }
}
function onSaved() { load() }

onMounted(load)
</script>

<style scoped>
.back { display: inline-flex; align-items: center; gap: 4px; margin-bottom: 12px; background: none; border: 0; cursor: pointer; color: var(--nad-ink-soft); font-size: 13px; }
</style>
