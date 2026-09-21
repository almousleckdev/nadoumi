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
          @click="openTransition"
        >
          {{ t('applications.transition') }}
        </el-button>
        <el-button
          v-if="canDecide && !closed"
          size="small"
          data-test="decide"
          @click="openDecision"
        >
          {{ t('applications.recordDecision') }}
        </el-button>
      </div>

      <el-tabs v-model="tab">
        <el-tab-pane
          :label="t('applications.tasks')"
          name="tasks"
        >
          <p
            v-if="!detail.tasks.length"
            class="ad-empty"
          >
            {{ t('applications.noTasks') }}
          </p>
          <ul
            v-else
            class="ad-list"
          >
            <li
              v-for="task in detail.tasks"
              :key="task.id"
            >
              <div class="ad-row">
                <span>
                  {{ task.title }}
                  <el-tag
                    v-if="task.mandatory"
                    size="small"
                    type="warning"
                  >{{ t('applications.mandatory') }}</el-tag>
                </span>
                <StatusBadge
                  :status="task.status"
                  :label="taskStatusLabel(task.status)"
                  :map="TASK_TONES"
                />
              </div>
              <div
                v-if="task.skipReason"
                class="ad-meta-line"
              >
                {{ t('applications.skipReason', { reason: task.skipReason }) }}
              </div>
              <div
                v-if="canTransition && task.status === 'OPEN'"
                class="ad-task-actions"
              >
                <el-button
                  size="small"
                  data-test="task-complete"
                  @click="completeTask(task.id)"
                >
                  {{ t('applications.completeTask') }}
                </el-button>
                <el-button
                  v-if="!task.mandatory"
                  size="small"
                  plain
                  data-test="task-skip"
                  @click="skipTask(task.id)"
                >
                  {{ t('applications.skipTask') }}
                </el-button>
              </div>
            </li>
          </ul>
        </el-tab-pane>

        <el-tab-pane
          :label="t('applications.history')"
          name="history"
        >
          <p
            v-if="!detail.history.length"
            class="ad-empty"
          >
            {{ t('applications.noHistory') }}
          </p>
          <el-timeline v-else>
            <el-timeline-item
              v-for="h in detail.history"
              :key="h.id"
              :timestamp="h.changedAt || ''"
              placement="top"
            >
              <b>{{ h.transitionCode }}</b>
              <div class="ad-meta-line">
                {{ t('applications.byUser', { id: h.changedBy ?? '-' }) }}<span v-if="h.reason"> · {{ h.reason }}</span>
              </div>
            </el-timeline-item>
          </el-timeline>
        </el-tab-pane>

        <el-tab-pane
          :label="t('applications.decisions')"
          name="decisions"
        >
          <p
            v-if="!detail.decisions.length"
            class="ad-empty"
          >
            {{ t('applications.noDecisions') }}
          </p>
          <ul
            v-else
            class="ad-list"
          >
            <li
              v-for="d in detail.decisions"
              :key="d.id"
            >
              <b>{{ d.decisionType }}: {{ d.outcome }}</b>
              <div class="ad-meta-line">
                {{ d.rationale }}
              </div>
              <div class="ad-meta-line">
                {{ t('applications.byUser', { id: d.decidedBy ?? '-' }) }} · {{ d.decidedAt || '' }}
              </div>
            </li>
          </ul>
        </el-tab-pane>

        <el-tab-pane
          :label="t('applications.events')"
          name="events"
        >
          <p
            v-if="!detail.events.length"
            class="ad-empty"
          >
            {{ t('applications.noEvents') }}
          </p>
          <el-timeline v-else>
            <el-timeline-item
              v-for="ev in detail.events"
              :key="ev.id"
              :timestamp="ev.at || ''"
              placement="top"
            >
              <b>{{ ev.eventType }}</b>
              <div class="ad-meta-line">
                {{ t('applications.byUser', { id: ev.actorUserId ?? '-' }) }}
              </div>
            </el-timeline-item>
          </el-timeline>
        </el-tab-pane>
      </el-tabs>
    </template>

    <el-dialog
      v-model="transitionOpen"
      :title="t('applications.transition')"
      width="440px"
      append-to-body
    >
      <el-form
        label-position="top"
        @submit.prevent="submitTransition"
      >
        <el-form-item
          :label="t('applications.transitionCode')"
          :error="transitionError"
        >
          <el-input
            v-model="transitionForm.code"
            data-test="transition-code"
            :placeholder="t('applications.transitionCodeHint')"
          />
        </el-form-item>
        <el-form-item :label="t('applications.reason')">
          <el-input
            v-model="transitionForm.reason"
            type="textarea"
            :rows="3"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transitionOpen = false">
          {{ t('common.cancel') }}
        </el-button>
        <el-button
          type="primary"
          :loading="saving"
          data-test="transition-submit"
          @click="submitTransition"
        >
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="decisionOpen"
      :title="t('applications.recordDecision')"
      width="440px"
      append-to-body
    >
      <el-form
        label-position="top"
        @submit.prevent="submitDecision"
      >
        <el-form-item :label="t('applications.decisionType')">
          <el-select
            v-model="decisionForm.decisionType"
            filterable
            allow-create
            style="width: 100%"
          >
            <el-option
              v-for="ty in DECISION_TYPES"
              :key="ty"
              :label="ty"
              :value="ty"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('applications.outcome')">
          <el-input
            v-model="decisionForm.outcome"
            data-test="decision-outcome"
          />
        </el-form-item>
        <el-form-item
          :label="t('applications.rationale')"
          :error="decisionError"
        >
          <el-input
            v-model="decisionForm.rationale"
            type="textarea"
            :rows="3"
            data-test="decision-rationale"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="decisionOpen = false">
          {{ t('common.cancel') }}
        </el-button>
        <el-button
          type="primary"
          :loading="saving"
          data-test="decision-submit"
          @click="submitDecision"
        >
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import { useUserStore } from '@/stores/user'
import {
  assignApplication, claimApplication, completeApplicationTask, decideApplication, getApplication,
  skipApplicationTask, transitionApplication, type ApplicationDetail,
} from '@/api/application'
import { DECISION_TYPES, isClosedStatus, STATUS_TONES } from './vocabulary'

const props = defineProps<{ modelValue: boolean, applicationId?: number }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean], changed: [] }>()

const { t, te } = useI18n()
const userStore = useUserStore()

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

const TASK_TONES = { OPEN: 'warning', DONE: 'success', SKIPPED: 'neutral', CANCELLED: 'neutral' } as const

const typeLabel = (type: string | null) =>
  type && te(`applications.typeMap.${type}`) ? t(`applications.typeMap.${type}`) : (type ?? '')
const statusLabel = (status: string | null) =>
  status && te(`applications.statusMap.${status}`) ? t(`applications.statusMap.${status}`) : (status ?? '')
const taskStatusLabel = (status: string | null) =>
  status && te(`applications.taskStatusMap.${status}`) ? t(`applications.taskStatusMap.${status}`) : (status ?? '')

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
const transitionForm = reactive({ code: '', reason: '' })
const transitionError = ref('')
function openTransition() {
  transitionForm.code = ''
  transitionForm.reason = ''
  transitionError.value = ''
  transitionOpen.value = true
}
async function submitTransition() {
  const code = transitionForm.code.trim()
  transitionError.value = code ? '' : t('applications.transitionCodeRequired')
  if (!code) return
  const ok = await run(() => transitionApplication(id(), code, {
    reason: transitionForm.reason.trim() || undefined,
    version: detail.value!.application.version,
  }))
  if (ok) transitionOpen.value = false
}

const decisionOpen = ref(false)
const decisionForm = reactive({ decisionType: '', outcome: '', rationale: '' })
const decisionError = ref('')
function openDecision() {
  decisionForm.decisionType = DECISION_TYPES[0]
  decisionForm.outcome = ''
  decisionForm.rationale = ''
  decisionError.value = ''
  decisionOpen.value = true
}
async function submitDecision() {
  const body = {
    decisionType: decisionForm.decisionType.trim(),
    outcome: decisionForm.outcome.trim(),
    rationale: decisionForm.rationale.trim(),
  }
  decisionError.value = body.decisionType && body.outcome && body.rationale ? '' : t('applications.decisionRequired')
  if (decisionError.value) return
  const ok = await run(() => decideApplication(id(), body))
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
.ad-empty { color: var(--nad-ink-soft, #64748b); font-size: 13px; }
.ad-list { list-style: none; margin: 0; padding: 0; display: grid; gap: 12px; }
.ad-row { display: flex; justify-content: space-between; gap: 8px; align-items: center; }
.ad-meta-line { font-size: 12px; color: var(--nad-ink-soft, #64748b); margin-top: 2px; }
.ad-task-actions { display: flex; gap: 8px; margin-top: 6px; }
</style>
