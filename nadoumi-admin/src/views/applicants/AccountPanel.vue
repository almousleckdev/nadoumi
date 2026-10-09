<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { listAccess } from '@/api/applicant'
import { changeUserStatus, getUser, type SysUserRow } from '@/api/system'
import { useConfirm } from '@/composables/useConfirm'
import { useUserStore } from '@/stores/user'
import Avatar from '@/components/ui/Avatar.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import StatusReasonDialog from '@/components/ui/StatusReasonDialog.vue'
import StudentContactDialog from './StudentContactDialog.vue'
import { statusChangeCopy, statusLabelKey, statusTone } from './studentStatus'

const STUDENT_USER_TYPE = '10'
const OWNER_ROLE = 'OWNER'
const STATUS_ACTIVE = '0'
const STATUS_SUSPENDED = '1'
const STATUS_BLOCKED = '2'

const props = defineProps<{ id: string, photoUrl?: string | null }>()

const { t } = useI18n()
const { confirm } = useConfirm()
const userStore = useUserStore()

const student = ref<SysUserRow | null>(null)
const contactOpen = ref(false)
const busy = ref(false)
const reasonOpen = ref(false)
const pendingStatus = ref(STATUS_SUSPENDED)

const canChangeStatus = computed(() => userStore.hasPerm('system:user:edit'))
const canContact = computed(() => userStore.hasPerm('nad:conversation:participate'))
const displayName = computed(() => student.value?.nickName || student.value?.userName || '')

// The account is the applicant's active owner. Staff-created applicants have none, so the panel stays hidden.
async function load() {
  try {
    const owner = (await listAccess(props.id))
      .find(g => g.accessRole === OWNER_ROLE && g.status === 'ACTIVE' && g.userId != null)
    if (!owner?.userId) return
    const user = (await getUser(owner.userId)).data
    if (user?.userType === STUDENT_USER_TYPE) student.value = user
  }
  catch {
    // Missing permission or a deleted account: there is simply no account section to show.
  }
}

// Suspending or blocking needs a written reason; activating needs only a confirmation.
async function changeStatus(next: string) {
  const current = student.value
  if (!current) return
  if (next === STATUS_ACTIVE) {
    const copy = statusChangeCopy(next)
    const ok = await confirm({ title: t(copy.title), message: t(copy.confirm, { name: displayName.value }), tone: 'primary' })
    if (ok) await apply(next)
    return
  }
  pendingStatus.value = next
  reasonOpen.value = true
}

async function apply(next: string, reason?: string) {
  const current = student.value
  if (!current) return
  busy.value = true
  try {
    await changeUserStatus(current.userId, next, reason)
    student.value = { ...current, status: next, statusReason: next === STATUS_ACTIVE ? null : reason }
    reasonOpen.value = false
    ElMessage.success(t('students.statusUpdated'))
  }
  catch (e) {
    ElMessage.error((e as Error)?.message || t('students.statusUpdateFailed'))
  }
  finally {
    busy.value = false
  }
}

onMounted(load)
</script>

<template>
  <section
    v-if="student"
    class="account"
    data-test="account-panel"
  >
    <div class="account__who">
      <Avatar
        :name="displayName"
        :src="props.photoUrl || student.avatar || undefined"
        :size="40"
        data-test="account-avatar"
      />
      <div class="account__text">
        <span class="account__name">{{ t('students.accountInfo') }}</span>
        <span class="account__meta">@{{ student.userName }} · {{ student.email || t('students.noEmail') }}</span>
        <span
          v-if="student.statusReason"
          class="account__reason"
          data-test="account-reason"
        >{{ t('students.reasonLabel') }}: {{ student.statusReason }}</span>
      </div>
      <StatusBadge
        :status="statusTone(student.status)"
        :label="t(statusLabelKey(student.status))"
      />
    </div>

    <div class="account__actions">
      <el-button
        v-if="canContact"
        size="small"
        data-test="account-contact"
        @click="contactOpen = true"
      >
        {{ t('students.contactStudent') }}
      </el-button>
      <template v-if="canChangeStatus">
        <el-button
          v-if="student.status === STATUS_ACTIVE"
          size="small"
          :loading="busy"
          data-test="account-suspend"
          @click="changeStatus(STATUS_SUSPENDED)"
        >
          {{ t('students.suspend') }}
        </el-button>
        <el-button
          v-if="student.status !== STATUS_BLOCKED"
          size="small"
          :loading="busy"
          data-test="account-block"
          @click="changeStatus(STATUS_BLOCKED)"
        >
          {{ t('students.block') }}
        </el-button>
        <el-button
          v-if="student.status !== STATUS_ACTIVE"
          size="small"
          type="primary"
          :loading="busy"
          data-test="account-activate"
          @click="changeStatus(STATUS_ACTIVE)"
        >
          {{ t('students.unblock') }}
        </el-button>
      </template>
    </div>

    <StatusReasonDialog
      v-model="reasonOpen"
      :title="t(statusChangeCopy(pendingStatus).title)"
      :message="t(statusChangeCopy(pendingStatus).confirm, { name: displayName })"
      :confirm-text="t(statusChangeCopy(pendingStatus).title)"
      :busy="busy"
      @confirm="(reason: string) => apply(pendingStatus, reason)"
    />

    <StudentContactDialog
      v-model="contactOpen"
      :student="student"
    />
  </section>
</template>

<style scoped>
.account {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
  padding: 12px 16px;
  border: 1px solid var(--nad-line, #e5e7eb);
  border-radius: 10px;
  background: var(--nad-surface, #fff);
}
.account__who {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}
.account__text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.account__name {
  font-size: 13px;
  font-weight: 600;
}
.account__meta {
  font-size: 12px;
  color: var(--nad-ink-soft);
  overflow-wrap: anywhere;
}
.account__reason {
  font-size: 12px;
  color: var(--el-color-danger);
  overflow-wrap: anywhere;
}
.account__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
