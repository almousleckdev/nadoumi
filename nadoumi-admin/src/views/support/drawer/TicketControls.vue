<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'
import {
  assignTicket, changeTicketCategory, changeTicketPriority, changeTicketStatus,
  TICKET_CATEGORIES, TICKET_PRIORITIES,
  type StaffTicketSummary, type TicketCategory, type TicketPriority,
} from '@/api/support'
import type { SysUserRow } from '@/api/system'
import { allowedNextStatuses } from '../ticketWorkflow'

const props = defineProps<{ ticket: StaffTicketSummary, staff: SysUserRow[], busy: boolean }>()
const emit = defineEmits<{ run: [action: () => Promise<unknown>] }>()

const { t } = useI18n()
const userStore = useUserStore()

const canManage = computed(() => userStore.hasPerm('nad:support:ticket:manage'))
const canAssign = computed(() => userStore.hasPerm('nad:support:ticket:assign'))
const nextStatuses = computed(() => allowedNextStatuses(props.ticket.status))
</script>

<template>
  <div>
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
        @click="emit('run', () => changeTicketStatus(ticket.id, s))"
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
        :model-value="ticket.priority"
        size="small"
        data-test="priority-select"
        :disabled="busy"
        @change="(v: TicketPriority) => emit('run', () => changeTicketPriority(ticket.id, v))"
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
        :model-value="ticket.category"
        size="small"
        data-test="category-select"
        :disabled="busy"
        @change="(v: TicketCategory) => emit('run', () => changeTicketCategory(ticket.id, v))"
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
        :model-value="ticket.assignedStaffId ?? undefined"
        size="small"
        filterable
        :placeholder="t('support.assignTo')"
        data-test="assign-select"
        :disabled="busy"
        @change="(v: number) => emit('run', () => assignTicket(ticket.id, v))"
      >
        <el-option
          v-for="m in staff"
          :key="m.userId"
          :label="`${m.nickName} (${m.userName})`"
          :value="m.userId"
        />
      </el-select>
    </div>
  </div>
</template>

<style scoped>
.st-actions { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin: 0 0 12px; }
.st-fields { display: flex; flex-wrap: wrap; gap: 8px; margin: 0 0 20px; }
.st-muted { color: var(--nad-ink-soft, #64748b); font-size: 13px; }
</style>
