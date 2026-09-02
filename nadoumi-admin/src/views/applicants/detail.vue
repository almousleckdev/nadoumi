<template>
  <div class="nad-page">
    <button
      class="back"
      type="button"
      @click="router.push('/applicants')"
    >
      <el-icon><ArrowLeft /></el-icon>{{ t('applicant.backToList') }}
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

    <template v-else-if="applicant">
      <PageHeader :title="`${applicant.givenName} ${applicant.familyName}`">
        <template #subtitle>
          {{ t('applicant.idLabel', { id: applicant.id }) }} ·
          {{ t('applicant.registered') }} {{ fmtDate(applicant.createdAt) }}
        </template>
        <template #actions>
          <StatusBadge :status="applicant.status" />
        </template>
      </PageHeader>

      <AppTabs
        v-model="tab"
        :tabs="tabs"
      >
        <OverviewTab
          v-if="tab === 'overview'"
          :applicant="applicant"
          :can-edit="canEdit"
          @updated="load"
        />
        <EducationTab
          v-else-if="tab === 'education'"
          :id="id"
          :can-edit="canEdit"
          @count="n => counts.education = n"
        />
        <ScoresTab
          v-else-if="tab === 'scores'"
          :id="id"
          :can-edit="canEdit"
          @count="n => counts.scores = n"
        />
        <ContactsTab
          v-else-if="tab === 'contacts'"
          :id="id"
          :can-edit="canEdit"
          @count="n => counts.contacts = n"
        />
        <AccessTab
          v-else-if="tab === 'access'"
          :id="id"
          @count="n => counts.access = n"
        />
      </AppTabs>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getApplicant, type Applicant } from '@/api/applicant'
import { useUserStore } from '@/stores/user'
import PageHeader from '@/components/PageHeader.vue'
import AppTabs from '@/components/ui/AppTabs.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import LoadingState from '@/components/ui/LoadingState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import type { Tab } from '@/components/ui/types'
import OverviewTab from './tabs/OverviewTab.vue'
import EducationTab from './tabs/EducationTab.vue'
import ScoresTab from './tabs/ScoresTab.vue'
import ContactsTab from './tabs/ContactsTab.vue'
import AccessTab from './tabs/AccessTab.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const id = route.params.id as string
const applicant = ref<Applicant | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)

const canEdit = computed(() => userStore.hasPerm('nad:applicant:edit'))
const canViewAccess = computed(() => userStore.hasPerm('nad:applicant:access:view'))

const tab = ref<'overview' | 'education' | 'scores' | 'contacts' | 'access'>('overview')
const counts = reactive<Record<string, number | undefined>>({})

const tabs = computed<Tab[]>(() => {
  const base: Tab[] = [
    { key: 'overview', label: t('applicant.tabOverview') },
    { key: 'education', label: t('applicant.tabEducation'), count: counts.education },
    { key: 'scores', label: t('applicant.tabScores'), count: counts.scores },
    { key: 'contacts', label: t('applicant.tabContacts'), count: counts.contacts },
  ]
  if (canViewAccess.value) {
    base.push({ key: 'access', label: t('applicant.tabAccess'), count: counts.access })
  }
  return base
})

function fmtDate(v: string | null): string {
  if (!v) return '—'
  const d = new Date(v.replace(' ', 'T'))
  return Number.isNaN(d.getTime()) ? v : d.toLocaleDateString()
}

async function load() {
  loading.value = true
  error.value = null
  try {
    applicant.value = await getApplicant(id)
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
</style>
