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
          {{ t('applicant.registered') }} {{ formatDate(applicant.createdAt) }}
        </template>
        <template #actions>
          <StatusBadge :status="applicant.status" />
          <el-button
            v-if="canDelete"
            type="danger"
            plain
            :loading="deleting"
            data-test="delete-applicant"
            @click="onDelete"
          >
            {{ t('applicant.delete') }}
          </el-button>
        </template>
      </PageHeader>

      <AccountPanel
        v-if="canViewAccess"
        :id="id"
      />

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
        <InterestsTab
          v-else-if="tab === 'interests'"
          :id="id"
        />
        <LocationTab
          v-else-if="tab === 'location'"
          :id="id"
        />
        <WorkTab
          v-else-if="tab === 'work'"
          :id="id"
          @count="n => counts.work = n"
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
import { ElMessage } from 'element-plus'
import { deleteApplicant, getApplicant, type Applicant } from '@/api/applicant'
import { useConfirm } from '@/composables/useConfirm'
import { useUserStore } from '@/stores/user'
import PageHeader from '@/components/PageHeader.vue'
import AppTabs from '@/components/ui/AppTabs.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import LoadingState from '@/components/ui/LoadingState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import type { Tab } from '@/components/ui/types'
import OverviewTab from './tabs/OverviewTab.vue'
import EducationTab from './tabs/EducationTab.vue'
import InterestsTab from './tabs/InterestsTab.vue'
import LocationTab from './tabs/LocationTab.vue'
import WorkTab from './tabs/WorkTab.vue'
import ContactsTab from './tabs/ContactsTab.vue'
import AccessTab from './tabs/AccessTab.vue'
import AccountPanel from './AccountPanel.vue'
import { formatDate } from '@/utils/date'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const id = route.params.id as string
const applicant = ref<Applicant | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)

const userStore = useUserStore()
const { confirm } = useConfirm()
const deleting = ref(false)
const canEdit = computed(() => userStore.hasPerm('nad:applicant:edit'))
const canDelete = computed(() => userStore.hasPerm('nad:applicant:delete'))
const canViewAccess = computed(() => userStore.hasPerm('nad:applicant:access:view'))

const tab = ref<'overview' | 'education' | 'interests' | 'location' | 'work' | 'contacts' | 'access'>('overview')
const counts = reactive<Record<string, number | undefined>>({})

const tabs = computed<Tab[]>(() => {
  const base: Tab[] = [
    { key: 'overview', label: t('applicant.tabOverview') },
    { key: 'education', label: t('applicant.tabEducation'), count: counts.education },
    { key: 'interests', label: t('applicant.tabInterests') },
    { key: 'location', label: t('applicant.tabLocation') },
    { key: 'work', label: t('applicant.tabWork'), count: counts.work },
    { key: 'contacts', label: t('applicant.tabContacts'), count: counts.contacts },
  ]
  if (canViewAccess.value) {
    base.push({ key: 'access', label: t('applicant.tabAccess'), count: counts.access })
  }
  return base
})

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

async function onDelete() {
  const current = applicant.value
  if (!current) return
  const ok = await confirm({
    title: t('applicant.deleteTitle'),
    message: t('applicant.deleteConfirm', { name: `${current.givenName} ${current.familyName}` }),
    confirmText: t('applicant.delete'),
    tone: 'danger',
  })
  if (!ok) return
  deleting.value = true
  try {
    await deleteApplicant(id)
    ElMessage.success(t('applicant.deleted'))
    router.push('/applicants')
  }
  catch (e) {
    ElMessage.error((e as Error)?.message || t('applicant.deleteFailed'))
  }
  finally {
    deleting.value = false
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
