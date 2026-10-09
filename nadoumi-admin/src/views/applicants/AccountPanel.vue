<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { deleteStudentAccount, listAccess } from '@/api/applicant'
import { changeUserStatus, getUser, type SysUserRow } from '@/api/system'
import { useConfirm } from '@/composables/useConfirm'
import { useUserStore } from '@/stores/user'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import StudentContactDialog from './StudentContactDialog.vue'
import { statusChangeCopy, statusLabelKey, statusTone } from './studentStatus'

const STUDENT_USER_TYPE = '10'
const OWNER_ROLE = 'OWNER'
const STATUS_ACTIVE = '0'
const STATUS_SUSPENDED = '1'
const STATUS_BLOCKED = '2'

const props = defineProps<{ id: string }>()

const { t } = useI18n()
const router = useRouter()
const { confirm } = useConfirm()
const userStore = useUserStore()

const student = ref<SysUserRow | null>(null)
const contactOpen = ref(false)
const busy = ref(false)

const canChangeStatus = computed(() => userStore.hasPerm('system:user:edit'))
const canContact = computed(() => userStore.hasPerm('nad:conversation:participate'))
const canDelete = computed(() => userStore.hasPerm('nad:student:delete'))
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

async function changeStatus(next: string) {
  const current = student.value
  if (!current) return
  const copy = statusChangeCopy(next)
  const ok = await confirm({
    title: t(copy.title),
    message: t(copy.confirm, { name: displayName.value }),
    tone: copy.tone === 'warning' ? 'danger' : 'primary',
  })
  if (!ok) return
  busy.value = true
  try {
    await changeUserStatus(current.userId, next)
    student.value = { ...current, status: next }
    ElMessage.success(t('students.statusUpdated'))
  }
  catch (e) {
    ElMessage.error((e as Error)?.message || t('students.statusUpdateFailed'))
  }
  finally {
    busy.value = false
  }
}

async function remove() {
  const current = student.value
  if (!current) return
  const ok = await confirm({
    title: t('students.deleteTitle'),
    message: t('students.deleteConfirm', { name: displayName.value }),
    confirmText: t('common.delete'),
    tone: 'danger',
  })
  if (!ok) return
  busy.value = true
  try {
    await deleteStudentAccount(current.userId)
    ElMessage.success(t('students.deleted'))
    router.push('/applicants')
  }
  catch (e) {
    ElMessage.error((e as Error)?.message || t('students.deleteFailed'))
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
      <el-avatar
        :size="40"
        :src="student.avatar || undefined"
        class="account__avatar"
      >
        {{ displayName.slice(0, 1).toUpperCase() }}
      </el-avatar>
      <div class="account__text">
        <span class="account__name">{{ t('students.accountInfo') }}</span>
        <span class="account__meta">@{{ student.userName }} · {{ student.email || t('students.noEmail') }}</span>
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
      <el-button
        v-if="canDelete"
        size="small"
        type="danger"
        plain
        :loading="busy"
        data-test="account-delete"
        @click="remove"
      >
        {{ t('students.deleteStudent') }}
      </el-button>
    </div>

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
.account__avatar {
  background: var(--nad-primary, #1e3a8a);
  color: #fff;
  font-weight: 600;
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
.account__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
