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

      <TicketControls
        :ticket="detail.ticket"
        :staff="staff"
        :busy="busy"
        @run="runAction"
      />

      <TicketThread
        v-model:draft="draft"
        :messages="detail.messages"
        :opened-by-user-id="detail.ticket.openedByUserId"
        :can-reply="canManage && canStaffReply(detail.ticket.status)"
        :busy="busy"
        @send="sendReply"
      />

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

    <template
      v-if="canDelete && detail"
      #footer
    >
      <el-button
        type="danger"
        plain
        :loading="busy"
        data-test="delete-ticket"
        @click="removeTicket"
      >
        {{ t('support.deleteTicket') }}
      </el-button>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { useConfirm } from '@/composables/useConfirm'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import LoadingState from '@/components/ui/LoadingState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import { useUserStore } from '@/stores/user'
import TicketControls from './drawer/TicketControls.vue'
import TicketThread from './drawer/TicketThread.vue'
import {
  deleteTicket, getTicket, replyToTicket,
  type StaffTicketDetail, type TicketEvent,
} from '@/api/support'
import type { SysUserRow } from '@/api/system'
import { canStaffReply, priorityTone, statusTone } from './ticketWorkflow'

const props = defineProps<{ modelValue: boolean, ticketId: number | null, staff: SysUserRow[] }>()
const emit = defineEmits<{ 'update:modelValue': [v: boolean], 'changed': [] }>()

const { t } = useI18n()
const userStore = useUserStore()
const { confirm } = useConfirm()

const canManage = computed(() => userStore.hasPerm('nad:support:ticket:manage'))
const canDelete = computed(() => userStore.hasPerm('nad:support:ticket:delete'))

const detail = ref<StaffTicketDetail | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)
const busy = ref(false)
const draft = ref('')

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
async function runAction(fn: () => Promise<unknown>): Promise<boolean> {
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

async function removeTicket() {
  if (!detail.value) return
  const ticket = detail.value.ticket
  const ok = await confirm({
    title: t('support.deleteTicket'),
    message: t('support.deleteTicketConfirm', { id: ticket.id }),
    confirmText: t('common.delete'),
    tone: 'danger',
  })
  if (!ok) return
  busy.value = true
  try {
    await deleteTicket(ticket.id)
    ElMessage.success(t('support.ticketDeleted'))
    emit('update:modelValue', false)
    emit('changed')
  }
  catch (e) {
    ElMessage.error((e as Error)?.message || t('state.errorTitle'))
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
.st-muted { color: var(--nad-ink-soft, #64748b); font-size: 13px; }
.st-h4 { margin: 0 0 12px; font-size: 13px; }
</style>
