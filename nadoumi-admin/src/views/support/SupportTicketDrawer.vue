<template>
  <el-drawer
    :model-value="modelValue"
    :title="detail ? `#${detail.ticket.id} · ${detail.ticket.subject}` : t('support.title')"
    size="560"
    @update:model-value="(v: boolean) => emit('update:modelValue', v)"
  >
    <LoadingState v-if="loading" />
    <ErrorState
      v-else-if="error"
      :message="error"
      @retry="load"
    />

    <template v-else-if="detail">
      <div class="st-badges">
        <StatusBadge
          :status="statusTone(detail.ticket.status)"
          :label="t(`support.statusMap.${detail.ticket.status}`)"
        />
        <StatusBadge
          :status="priorityTone(detail.ticket.priority)"
          :label="t(`support.priorityMap.${detail.ticket.priority}`)"
        />
      </div>

      <dl class="st-meta">
        <div>
          <dt>{{ t('support.category') }}</dt>
          <dd>{{ t(`support.categoryMap.${detail.ticket.category}`) }}</dd>
        </div>
        <div>
          <dt>{{ t('support.openedBy') }}</dt>
          <dd>{{ detail.ticket.openedByUserId }}</dd>
        </div>
        <div>
          <dt>{{ t('support.assignee') }}</dt>
          <dd>{{ assigneeLabel }}</dd>
        </div>
        <div>
          <dt>{{ t('support.created') }}</dt>
          <dd>{{ detail.ticket.createTime }}</dd>
        </div>
      </dl>

      <div
        v-if="canManage"
        class="st-actions"
        data-test="status-actions"
      >
        <el-button
          v-for="s in nextStatuses"
          :key="s"
          size="small"
          :type="s === 'RESOLVED' ? 'success' : 'primary'"
          :loading="busy"
          @click="runAction(() => changeTicketStatus(detail!.ticket.id, s))"
        >
          {{ t(`support.action.${s}`) }}
        </el-button>
        <span
          v-if="!nextStatuses.length"
          class="st-muted"
        >{{ t('support.noMoreMoves') }}</span>
      </div>

      <div class="st-fields">
        <el-select
          v-if="canManage"
          :model-value="detail.ticket.priority"
          size="small"
          data-test="priority-select"
          :disabled="busy"
          @change="(v: TicketPriority) => runAction(() => changeTicketPriority(detail!.ticket.id, v))"
        >
          <el-option
            v-for="p in TICKET_PRIORITIES"
            :key="p"
            :label="t(`support.priorityMap.${p}`)"
            :value="p"
          />
        </el-select>
        <el-select
          v-if="canManage"
          :model-value="detail.ticket.category"
          size="small"
          data-test="category-select"
          :disabled="busy"
          @change="(v: TicketCategory) => runAction(() => changeTicketCategory(detail!.ticket.id, v))"
        >
          <el-option
            v-for="c in TICKET_CATEGORIES"
            :key="c"
            :label="t(`support.categoryMap.${c}`)"
            :value="c"
          />
        </el-select>
        <el-select
          v-if="canAssign"
          :model-value="detail.ticket.assignedStaffId ?? undefined"
          size="small"
          filterable
          :placeholder="t('support.assignTo')"
          data-test="assign-select"
          :disabled="busy"
          @change="(v: number) => runAction(() => assignTicket(detail!.ticket.id, v))"
        >
          <el-option
            v-for="m in staff"
            :key="m.userId"
            :label="`${m.nickName} (${m.userName})`"
            :value="m.userId"
          />
        </el-select>
      </div>

      <h4 class="st-h4">
        {{ t('support.conversation') }}
      </h4>
      <p
        v-if="!detail.messages.length"
        class="st-muted"
      >
        {{ t('support.noMessages') }}
      </p>
      <ul
        v-else
        class="st-thread"
        data-test="thread"
      >
        <li
          v-for="m in detail.messages"
          :key="m.id"
          class="st-msg"
          :class="{ 'st-msg--student': m.senderUserId === detail.ticket.openedByUserId }"
        >
          <div class="st-msg-meta">
            {{ m.senderName || m.senderUserId }} · {{ m.createdAt }}
          </div>
          <div class="st-msg-body">
            {{ m.body }}
          </div>
        </li>
      </ul>

      <div
        v-if="canManage && canStaffReply(detail.ticket.status)"
        class="st-reply"
        data-test="reply"
      >
        <el-input
          v-model="draft"
          type="textarea"
          :rows="3"
          maxlength="4000"
          :placeholder="t('support.replyPlaceholder')"
        />
        <el-button
          type="primary"
          size="small"
          :disabled="!draft.trim()"
          :loading="busy"
          data-test="send-reply"
          @click="sendReply"
        >
          {{ t('support.send') }}
        </el-button>
      </div>

      <h4 class="st-h4">
        {{ t('support.history') }}
      </h4>
      <el-timeline
        v-if="detail.events.length"
        data-test="events"
      >
        <el-timeline-item
          v-for="(e, i) in detail.events"
          :key="i"
          :timestamp="e.createdAt"
        >
          {{ eventText(e) }}
        </el-timeline-item>
      </el-timeline>
      <p
        v-else
        class="st-muted"
      >
        {{ t('support.noHistory') }}
      </p>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import LoadingState from '@/components/ui/LoadingState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import { useUserStore } from '@/stores/user'
import {
  assignTicket, changeTicketCategory, changeTicketPriority, changeTicketStatus, getTicket, replyToTicket,
  TICKET_CATEGORIES, TICKET_PRIORITIES,
  type StaffTicketDetail, type StaffTicketSummary, type TicketCategory, type TicketEvent, type TicketPriority,
} from '@/api/support'
import type { SysUserRow } from '@/api/system'
import { allowedNextStatuses, canStaffReply, priorityTone, statusTone } from './ticketWorkflow'

const props = defineProps<{ modelValue: boolean, ticketId: number | null, staff: SysUserRow[] }>()
const emit = defineEmits<{ 'update:modelValue': [v: boolean], 'changed': [] }>()

const { t } = useI18n()
const userStore = useUserStore()

const canManage = computed(() => userStore.hasPerm('nad:support:ticket:manage'))
const canAssign = computed(() => userStore.hasPerm('nad:support:ticket:assign'))

const detail = ref<StaffTicketDetail | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)
const busy = ref(false)
const draft = ref('')

const nextStatuses = computed(() => (detail.value ? allowedNextStatuses(detail.value.ticket.status) : []))

const assigneeLabel = computed(() => {
  const id = detail.value?.ticket.assignedStaffId
  if (id == null) return t('support.unassigned')
  const m = props.staff.find(s => s.userId === id)
  return m ? m.nickName : String(id)
})

function eventText(e: TicketEvent): string {
  const key = `support.event.${e.eventType}`
  return t(key, { from: e.oldValue ?? '', to: e.newValue ?? '', actor: e.actorUserId })
}

async function load() {
  if (props.ticketId == null) return
  loading.value = true
  error.value = null
  try {
    detail.value = await getTicket(props.ticketId)
  }
  catch (e) {
    error.value = (e as Error)?.message || t('state.errorTitle')
  }
  finally {
    loading.value = false
  }
}

watch(() => [props.modelValue, props.ticketId] as const, ([open, id]) => {
  if (open && id != null) {
    draft.value = ''
    load()
  }
}, { immediate: true })

/** Runs a mutation, then reloads the full detail so the event history and allowed moves stay truthful. */
async function runAction(fn: () => Promise<StaffTicketSummary | unknown>): Promise<boolean> {
  busy.value = true
  try {
    await fn()
    ElMessage.success(t('common.saved'))
    await load()
    emit('changed')
    return true
  }
  catch (e) {
    // The backend is authoritative on transitions and permissions; show what it said.
    ElMessage.error((e as Error)?.message || t('state.errorTitle'))
    return false
  }
  finally {
    busy.value = false
  }
}

async function sendReply() {
  if (!detail.value || !draft.value.trim()) return
  const id = detail.value.ticket.id
  const body = draft.value.trim()
  if (await runAction(() => replyToTicket(id, body))) draft.value = ''
}
</script>

<style scoped>
.st-badges { display: flex; gap: 8px; margin-bottom: 12px; }
.st-meta { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin: 0 0 16px; }
.st-meta dt { font-size: 12px; color: var(--nad-ink-soft, #64748b); }
.st-meta dd { margin: 2px 0 0; font-size: 14px; }
.st-actions { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin: 0 0 12px; }
.st-fields { display: flex; flex-wrap: wrap; gap: 8px; margin: 0 0 20px; }
.st-muted { color: var(--nad-ink-soft, #64748b); font-size: 13px; }
.st-h4 { margin: 0 0 12px; font-size: 13px; }
.st-thread { list-style: none; margin: 0 0 16px; padding: 0; display: grid; gap: 10px; }
.st-msg { border: 1px solid var(--el-border-color-light); border-radius: 8px; padding: 10px 12px; background: #fff; }
.st-msg--student { background: var(--el-fill-color-lighter); }
.st-msg-meta { font-size: 12px; color: var(--nad-ink-soft, #64748b); margin-bottom: 4px; }
.st-msg-body { white-space: pre-wrap; word-break: break-word; font-size: 14px; }
.st-reply { display: grid; gap: 8px; justify-items: start; margin: 0 0 20px; }
.st-reply :deep(.el-textarea) { width: 100%; }
</style>
