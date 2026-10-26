<template>
  <div class="nad-page">
    <button
      class="back"
      type="button"
      @click="router.push('/applications')"
    >
      <el-icon><ArrowLeft /></el-icon>{{ t('applications.back') }}
    </button>

    <LoadingState
      v-if="loading && !detail"
      :rows="5"
    />
    <ErrorState
      v-else-if="error"
      :message="error"
      @retry="load"
    />

    <template v-else-if="detail">
      <PageHeader
        :title="detail.application.reference ?? `#${detail.application.id}`"
        :subtitle="detail.opportunity.title ?? ''"
      >
        <template #actions>
          <StatusBadge
            :status="detail.application.currentStatus"
            :map="STATUS_TONES"
            :label="statusLabel(detail.application.currentStatus)"
          />
          <el-button
            v-if="canClaim"
            type="primary"
            data-test="claim"
            @click="claim"
          >
            {{ t('applications.claim') }}
          </el-button>
          <el-button
            v-if="canAssign"
            data-test="assign"
            @click="assignOpen = true"
          >
            {{ t('applications.assign') }}
          </el-button>
          <el-button
            v-if="canDelete"
            type="danger"
            plain
            data-test="delete"
            @click="deleteOpen = true"
          >
            {{ t('common.delete') }}
          </el-button>
          <el-button
            v-if="canChange && !closed"
            type="success"
            data-test="change-status"
            @click="statusOpen = true"
          >
            {{ t('applications.changeStatus') }}
          </el-button>
        </template>
      </PageHeader>

      <DescriptionList
        class="facts"
        :items="facts"
      >
        <template #applicant>
          <el-link
            type="primary"
            @click="router.push(`/applicants/${detail.applicant.publicId}`)"
          >
            {{ detail.applicant.name }}
          </el-link>
        </template>
      </DescriptionList>

      <AppTabs
        v-model="tab"
        :tabs="tabs"
      >
        <FormPanel
          v-if="tab === 'form'"
          :application-id="detail.application.id"
          :reference="detail.application.reference"
        />
        <DocumentsPanel
          v-else-if="tab === 'documents'"
          :applicant-id="detail.application.applicantId"
        />
        <PaymentPanel
          v-else-if="tab === 'payment'"
          :application-id="detail.application.id"
        />
        <TasksPanel
          v-else-if="tab === 'tasks'"
          :detail="detail"
          :can-work="canChange && !closed"
          @complete="completeTask"
          @skip="skipTask"
        />
        <ActivityPanel
          v-else-if="tab === 'activity'"
          :detail="detail"
        />
        <DecisionsPanel
          v-else-if="tab === 'decisions'"
          :detail="detail"
        />
        <InterviewPanel
          v-else-if="tab === 'interview'"
          :application-id="detail.application.id"
          :can-schedule="canChange && detail.application.currentStatus === INTERVIEW_STATUS"
          @changed="load"
        />
        <LettersPanel
          v-else-if="tab === 'letters'"
          :application-id="detail.application.id"
          :can-upload="canChange && !!detail.application.currentStatus && ADMISSION_DOCUMENT_STATUSES.includes(detail.application.currentStatus)"
          @changed="load"
        />
      </AppTabs>
    </template>

    <DeleteWithReasonDialog
      v-model="deleteOpen"
      :title="t('applications.deleteTitle')"
      :message="t('applications.deleteBody', { ref: detail?.application.reference ?? '' })"
      :busy="saving"
      @confirm="doDelete"
    />
    <AssignDialog
      v-model="assignOpen"
      :saving="saving"
      :current="detail?.application.assigneeUserId ?? null"
      @submit="assign"
    />
    <StatusDialog
      v-if="detail"
      v-model="statusOpen"
      :saving="saving"
      :application-id="detail.application.id"
      @submit="changeStatus"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import AppTabs from '@/components/ui/AppTabs.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import LoadingState from '@/components/ui/LoadingState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import DescriptionList from '@/components/ui/DescriptionList.vue'
import type { Tab } from '@/components/ui/types'
import { useUserStore } from '@/stores/user'
import { formatDateTime } from '@/utils/date'
import {
  advanceApplication, assignApplication, claimApplication, completeApplicationTask, deleteApplication, getApplication,
  skipApplicationTask, type ApplicationDetail,
} from '@/api/application'
import { ADMISSION_DOCUMENT_STATUSES, INTERVIEW_STATUS, isClosedStatus, STATUS_TONES } from './vocabulary'
import { useApplicationLabels } from './useApplicationLabels'
import AssignDialog from './detail/AssignDialog.vue'
import DeleteWithReasonDialog from '@/components/ui/DeleteWithReasonDialog.vue'
import StatusDialog from './detail/StatusDialog.vue'
import FormPanel from './detail/FormPanel.vue'
import DocumentsPanel from './detail/DocumentsPanel.vue'
import PaymentPanel from './detail/PaymentPanel.vue'
import TasksPanel from './detail/TasksPanel.vue'
import ActivityPanel from './detail/ActivityPanel.vue'
import DecisionsPanel from './detail/DecisionsPanel.vue'
import InterviewPanel from './detail/InterviewPanel.vue'
import LettersPanel from './detail/LettersPanel.vue'

type TabKey = 'form' | 'documents' | 'payment' | 'tasks' | 'activity' | 'decisions' | 'interview' | 'letters'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { typeLabel, statusLabel } = useApplicationLabels()
const id = Number(route.params.id)

const detail = ref<ApplicationDetail | null>(null)
const loading = ref(false)
const error = ref('')
const saving = ref(false)
const tab = ref<TabKey>('form')
const assignOpen = ref(false)
const statusOpen = ref(false)

// Permission checks only decide which controls to show; every action is authorised by the backend.
const canClaim = computed(() => userStore.hasPerm('nad:application:claim') && detail.value?.application.assigneeUserId == null)
const canAssign = computed(() => userStore.hasPerm('nad:application:assign'))
const canDelete = computed(() => userStore.hasPerm('nad:application:remove'))
const deleteOpen = ref(false)
const canChange = computed(() => userStore.hasPerm('nad:application:transition'))
const closed = computed(() => isClosedStatus(detail.value?.application.currentStatus ?? null))

// the interview and the admission documents only matter from the status that asks for them on
const hasInterview = computed(() => detail.value?.application.currentStatus === INTERVIEW_STATUS
  || detail.value?.events.some(e => e.eventType.startsWith('INTERVIEW_')) === true)
const hasLetters = computed(() => {
  const status = detail.value?.application.currentStatus
  return !!status && ADMISSION_DOCUMENT_STATUSES.includes(status)
})

const tabs = computed<Tab[]>(() => [
  { key: 'form', label: t('applications.tabForm') },
  { key: 'documents', label: t('applications.tabDocuments') },
  { key: 'payment', label: t('applications.tabPayment') },
  ...(hasInterview.value ? [{ key: 'interview', label: t('applications.tabInterview') }] : []),
  ...(hasLetters.value ? [{ key: 'letters', label: t('applications.tabLetters') }] : []),
  { key: 'tasks', label: t('applications.tabTasks'), count: detail.value?.tasks.filter(x => x.status === 'OPEN').length },
  { key: 'activity', label: t('applications.tabActivity'), count: detail.value?.events.length },
  { key: 'decisions', label: t('applications.tabDecisions'), count: detail.value?.decisions.length },
])

const facts = computed(() => {
  const d = detail.value
  if (!d) return []
  return [
    { label: t('applications.applicant'), value: d.applicant.name, slot: 'applicant' },
    { label: t('applications.email'), value: d.applicant.email },
    { label: t('applications.phone'), value: d.applicant.phone },
    { label: t('applications.nationality'), value: d.applicant.nationality },
    { label: t('applications.type'), value: typeLabel(d.application.applicationType) },
    { label: t('applications.university'), value: d.opportunity.universityName },
    { label: t('applications.intake'), value: words(d.opportunity.intake) },
    { label: t('applications.stage'), value: d.application.currentStageName },
    { label: t('applications.assignee'), value: d.assigneeName ?? t('applications.unassigned') },
    { label: t('applications.submitted'), value: formatDateTime(d.application.submittedAt) },
  ]
})

/** AUTUMN_SEPTEMBER becomes "Autumn september"; the engine's codes never reach the screen. */
function words(code: string | null): string | null {
  if (!code) return null
  const text = code.toLowerCase().replace(/_/g, ' ')
  return text.charAt(0).toUpperCase() + text.slice(1)
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    detail.value = await getApplication(id)
  }
  catch (e) {
    error.value = (e as Error)?.message || t('state.errorTitle')
  }
  finally {
    loading.value = false
  }
}

async function run(fn: () => Promise<unknown>): Promise<boolean> {
  saving.value = true
  try {
    await fn()
    ElMessage.success(t('common.saved'))
    await load()
    return true
  }
  catch {
    // the shared request client already toasts the server's message
    return false
  }
  finally {
    saving.value = false
  }
}

const claim = () => run(() => claimApplication(id))

async function doDelete(reason: string) {
  saving.value = true
  try {
    await deleteApplication(id, reason)
    ElMessage.success(t('applications.deleted'))
    deleteOpen.value = false
    router.push('/applications')
  }
  catch {
    // the shared request client already toasts the server's message
  }
  finally {
    saving.value = false
  }
}

async function assign(userId: number) {
  if (await run(() => assignApplication(id, userId))) assignOpen.value = false
}

async function changeStatus(payload: { code: string, note: string }) {
  const ok = await run(() => advanceApplication(id, {
    code: payload.code,
    note: payload.note || undefined,
    version: detail.value!.application.version,
  }))
  if (ok) statusOpen.value = false
}

const completeTask = (taskId: number) => run(() => completeApplicationTask(id, taskId))

async function skipTask(taskId: number) {
  let reason: string
  try {
    reason = (await ElMessageBox.prompt(t('applications.skipPrompt'), t('applications.skipTask'), {
      inputPattern: /\S/, inputErrorMessage: t('applications.reasonRequired'),
    })).value.trim()
  }
  catch { return }
  await run(() => skipApplicationTask(id, taskId, reason))
}

onMounted(load)
</script>

<style scoped>
.back {
  display: inline-flex; align-items: center; gap: 5px; border: none; background: transparent;
  color: var(--nad-ink-soft, #64748b); cursor: pointer; font-size: 13px; padding: 0; margin-bottom: 8px;
}
.facts { margin: 0 0 20px; padding: 16px; border: 1px solid var(--nad-line, #e2e8f0); border-radius: 8px; }
</style>
