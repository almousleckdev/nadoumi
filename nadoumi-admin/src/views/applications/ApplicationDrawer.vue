<template>
  <el-drawer
    :model-value="modelValue"
    :title="detail ? `#${detail.application.id} · ${typeLabel(detail.application.applicationType)}` : t('applications.title')"
    size="640"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div
      v-if="loading"
      v-loading="true"
      class="ad-loading"
    />
    <el-alert
      v-else-if="error"
      type="error"
      :closable="false"
      :title="error"
      show-icon
    >
      <el-button
        size="small"
        @click="load"
      >
        {{ t('state.retry') }}
      </el-button>
    </el-alert>

    <template v-else-if="detail">
      <div class="ad-badges">
        <StatusBadge
          :status="detail.application.currentStatus"
          :map="STATUS_TONES"
          :label="statusLabel(detail.application.currentStatus)"
        />
      </div>

      <dl class="ad-meta">
        <div>
          <dt>{{ t('applications.stage') }}</dt>
          <dd>{{ detail.application.currentStageName || '' }}</dd>
        </div>
        <div>
          <dt>{{ t('applications.applicant') }}</dt>
          <dd>#{{ detail.application.applicantId }}</dd>
        </div>
        <div>
          <dt>{{ t('applications.assignee') }}</dt>
          <dd>{{ detail.application.assigneeUserId ?? t('applications.unassigned') }}</dd>
        </div>
        <div>
          <dt>{{ t('applications.submitted') }}</dt>
          <dd>{{ detail.application.submittedAt || '' }}</dd>
        </div>
      </dl>

      <div class="ad-actions">
        <el-button
          v-if="canClaim"
          size="small"
          type="primary"
          data-test="claim"
          @click="claim"
        >
          {{ t('applications.claim') }}
        </el-button>
        <el-button
          v-if="canAssign"
          size="small"
          data-test="assign"
          @click="assign"
        >
          {{ t('applications.assign') }}
        </el-button>
        <el-button
          v-if="canTransition && !closed"
          size="small"
          type="success"
          data-test="transition"
          @click="transitionOpen = true"
        >
          {{ t('applications.transition') }}
        </el-button>
        <el-button
          v-if="canDecide && !closed"
          size="small"
          data-test="decide"
          @click="decisionOpen = true"
        >
          {{ t('applications.recordDecision') }}
        </el-button>
      </div>
      <ApplicationTabs
        v-model:tab="tab"
        :detail="detail"
        :can-transition="canTransition"
        @complete="completeTask"
        @skip="skipTask"
      />
    </template>

    <TransitionDialog
      v-model="transitionOpen"
      :saving="saving"
      @submit="submitTransition"
    />
    <DecisionDialog
      v-model="decisionOpen"
      :saving="saving"
      @submit="submitDecision"
    />
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import { useUserStore } from '@/stores/user'
import {
  assignApplication, claimApplication, completeApplicationTask, decideApplication, getApplication,
  skipApplicationTask, transitionApplication, type ApplicationDetail,
} from '@/api/application'
import { isClosedStatus, STATUS_TONES } from './vocabulary'
import ApplicationTabs from './drawer/ApplicationTabs.vue'
import TransitionDialog from './drawer/TransitionDialog.vue'
import DecisionDialog from './drawer/DecisionDialog.vue'
import { useApplicationLabels } from './drawer/useApplicationLabels'

const props = defineProps<{ modelValue: boolean, applicationId?: number }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean], changed: [] }>()

const { t } = useI18n()
const userStore = useUserStore()
const { typeLabel, statusLabel } = useApplicationLabels()

const detail = ref<ApplicationDetail | null>(null)
const loading = ref(false)
const error = ref('')
const saving = ref(false)
const tab = ref('tasks')

// Permission checks only decide which controls to show; every action is authorised by the backend.
const canClaim = computed(() => userStore.hasPerm('nad:application:claim') && detail.value?.application.assigneeUserId == null)
const canAssign = computed(() => userStore.hasPerm('nad:application:assign'))
const canTransition = computed(() => userStore.hasPerm('nad:application:transition'))
const canDecide = computed(() => userStore.hasPerm('nad:application:decide'))
const closed = computed(() => isClosedStatus(detail.value?.application.currentStatus ?? null))

async function load() {
  if (props.applicationId === undefined) return
  loading.value = true
  error.value = ''
  try {
    detail.value = await getApplication(props.applicationId)
  }
  catch (e) {
    detail.value = null
    error.value = (e as Error)?.message || t('state.errorTitle')
  }
  finally {
    loading.value = false
  }
}

watch(() => [props.modelValue, props.applicationId] as const, ([open, id]) => {
  if (open && id !== undefined) {
    tab.value = 'tasks'
    load()
  }
}, { immediate: true })

async function changed() {
  ElMessage.success(t('common.saved'))
  await load()
  emit('changed')
}

async function run(fn: () => Promise<unknown>): Promise<boolean> {
  saving.value = true
  try {
    await fn()
    await changed()
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

const id = () => detail.value!.application.id

const claim = () => run(() => claimApplication(id()))

async function assign() {
  let value: string
  try {
    value = (await ElMessageBox.prompt(t('applications.assignPrompt'), t('applications.assign'), {
      inputPattern: /^[1-9]\d*$/, inputErrorMessage: t('applications.assignInvalid'),
    })).value
  }
  catch { return }
  await run(() => assignApplication(id(), Number(value)))
}

const completeTask = (taskId: number) => run(() => completeApplicationTask(id(), taskId))

async function skipTask(taskId: number) {
  let reason: string
  try {
    reason = (await ElMessageBox.prompt(t('applications.skipPrompt'), t('applications.skipTask'), {
      inputPattern: /\S/, inputErrorMessage: t('applications.reasonRequired'),
    })).value.trim()
  }
  catch { return }
  await run(() => skipApplicationTask(id(), taskId, reason))
}

const transitionOpen = ref(false)
const decisionOpen = ref(false)

async function submitTransition(payload: { code: string, reason: string }) {
  const ok = await run(() => transitionApplication(id(), payload.code, {
    reason: payload.reason || undefined,
    version: detail.value!.application.version,
  }))
  if (ok) transitionOpen.value = false
}

async function submitDecision(payload: { decisionType: string, outcome: string, rationale: string }) {
  const ok = await run(() => decideApplication(id(), payload))
  if (ok) decisionOpen.value = false
}
</script>

<style scoped>
.ad-loading { min-height: 160px; }
.ad-badges { display: flex; gap: 8px; margin-bottom: 12px; }
.ad-meta { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; margin: 0 0 16px; }
.ad-meta dt { font-size: 12px; color: var(--nad-ink-soft, #64748b); }
.ad-meta dd { margin: 2px 0 0; font-size: 14px; }
.ad-actions { display: flex; flex-wrap: wrap; gap: 8px; margin: 0 0 16px; }
</style>
