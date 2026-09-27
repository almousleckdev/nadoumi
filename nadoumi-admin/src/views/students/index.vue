<template>
  <div class="nad-page">
    <PageHeader
      :title="t('students.title')"
      :subtitle="t('students.subtitle')"
    />

    <FilterBar
      :dirty="dirty"
      @clear="clearFilters"
    >
      <el-input
        v-model="filters.userName"
        :placeholder="t('students.searchPlaceholder')"
        clearable
        style="width: 280px"
        @keyup.enter="reload"
        @clear="reload"
      />
      <el-select
        v-model="filters.status"
        :placeholder="t('students.status')"
        clearable
        style="width: 160px"
        @change="reload"
      >
        <el-option
          :label="t('students.statusActive')"
          value="0"
        />
        <el-option
          :label="t('students.statusSuspended')"
          value="1"
        />
        <el-option
          :label="t('students.statusBlocked')"
          value="2"
        />
      </el-select>
      <el-button
        :icon="Search"
        @click="reload"
      >
        {{ t('common.search') }}
      </el-button>
    </FilterBar>

    <DataTable
      :rows="rows"
      :columns="columns"
      :loading="loading"
      :error="error"
      row-key="userId"
      :empty-title="t('students.noStudents')"
      @retry="reload"
    >
      <template #cell-student="{ row }">
        <div class="student-cell">
          <el-avatar
            :size="36"
            :src="(row as SysUserRow).avatar || undefined"
            class="student-cell__avatar"
          >
            {{ ((row as SysUserRow).nickName || (row as SysUserRow).userName || 'S').slice(0, 1).toUpperCase() }}
          </el-avatar>
          <div class="student-cell__text">
            <span class="student-cell__name">{{ (row as SysUserRow).nickName || (row as SysUserRow).userName }}</span>
            <span class="student-cell__handle">@{{ (row as SysUserRow).userName }}</span>
          </div>
        </div>
      </template>

      <template #cell-email="{ row }">
        <span>{{ (row as SysUserRow).email || '-' }}</span>
      </template>

      <template #cell-phonenumber="{ row }">
        <span>{{ (row as SysUserRow).phonenumber || '-' }}</span>
      </template>

      <template #cell-createTime="{ row }">
        <span>{{ (row as SysUserRow).createTime || '-' }}</span>
      </template>

      <template #cell-loginDate="{ row }">
        <span>{{ (row as SysUserRow).loginDate || '-' }}</span>
      </template>

      <template #cell-status="{ row }">
        <StatusBadge
          :status="statusTone((row as SysUserRow).status)"
          :label="statusLabel((row as SysUserRow).status)"
        />
      </template>

      <template #cell-actions="{ row }">
        <div class="table-actions">
          <el-button
            link
            type="primary"
            @click="openDetails(row as SysUserRow)"
          >
            {{ t('students.viewDetails') }}
          </el-button>

          <el-button
            link
            type="primary"
            @click="openContact(row as SysUserRow)"
          >
            {{ t('students.contactStudent') }}
          </el-button>

          <el-dropdown
            trigger="click"
            @command="(cmd: string) => handleStatusChange(row as SysUserRow, cmd)"
          >
            <el-button
              link
              type="info"
            >
              {{ t('common.actions') }}
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                  v-if="(row as SysUserRow).status === '0'"
                  command="1"
                >
                  <span class="text-warning">{{ t('students.suspend') }}</span>
                </el-dropdown-item>
                <el-dropdown-item
                  v-if="(row as SysUserRow).status === '0' || (row as SysUserRow).status === '1'"
                  command="2"
                >
                  <span class="text-danger">{{ t('students.block') }}</span>
                </el-dropdown-item>
                <el-dropdown-item
                  v-if="(row as SysUserRow).status === '1' || (row as SysUserRow).status === '2'"
                  command="0"
                >
                  <span class="text-success">{{ t('students.unblock') }}</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </template>
    </DataTable>

    <Pagination
      v-model:page="page"
      v-model:size="size"
      :total="total"
      @change="load"
    />

    <StudentDrawer
      v-model="drawerOpen"
      :student="selectedStudent"
      @contact="openContact"
      @change-status="handleStatusChange"
    />

    <!-- Contact Student Modal -->
    <el-dialog
      v-model="contactOpen"
      :title="t('students.sendMessageModalTitle', { name: contactTarget?.nickName || contactTarget?.userName })"
      width="540px"
      destroy-on-close
    >
      <el-form
        label-position="top"
        class="contact-form"
      >
        <el-form-item :label="t('students.messageSubject')">
          <el-input
            v-model="contactForm.subject"
            placeholder="e.g. Update regarding your profile / application"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item
          :label="t('students.messageBody')"
          required
        >
          <el-input
            v-model="contactForm.body"
            type="textarea"
            :rows="5"
            placeholder="Type your message to the student here…"
            maxlength="4000"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="contactOpen = false">
          {{ t('common.cancel') }}
        </el-button>
        <el-button
          type="primary"
          :loading="sendingMessage"
          :disabled="!contactForm.body.trim()"
          @click="submitContact"
        >
          {{ t('students.sendDirectMessage') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, ArrowDown } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/ui/FilterBar.vue'
import DataTable from '@/components/ui/DataTable.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import Pagination from '@/components/ui/Pagination.vue'
import StudentDrawer from './StudentDrawer.vue'
import { listUsers, changeUserStatus, type SysUserRow } from '@/api/system'
import { createConversation } from '@/api/conversation'
import { usePagedList } from '@/composables/usePagedList'

const { t } = useI18n()
const router = useRouter()

const emptyFilters = () => ({ userName: '', status: '' })

const { rows, total, loading, error, filters, page, size, dirty, load, reload, clearFilters } =
  usePagedList<SysUserRow, ReturnType<typeof emptyFilters>>({
    emptyFilters,
    firstPage: 1,
    size: 20,
    fetch: async (f, { index, size }) => {
      const res = await listUsers({
        userType: '10', // Strictly registered Nadoumi students
        userName: f.userName || undefined,
        status: f.status || undefined,
        pageNum: index,
        pageSize: size,
      })
      return {
        rows: res.rows || [],
        total: res.total || 0,
      }
    },
  })

const columns = computed(() => [
  { prop: 'student', label: t('students.name'), minWidth: 200 },
  { prop: 'email', label: t('students.email'), minWidth: 180 },
  { prop: 'phonenumber', label: t('students.phone'), minWidth: 140 },
  { prop: 'createTime', label: t('students.registered'), width: 170 },
  { prop: 'loginDate', label: t('students.lastLogin'), width: 170 },
  { prop: 'status', label: t('students.status'), width: 120, align: 'center' as const },
  { prop: 'actions', label: t('common.actions'), width: 220, align: 'right' as const },
])

const drawerOpen = ref(false)
const selectedStudent = ref<SysUserRow | null>(null)

function openDetails(student: SysUserRow) {
  selectedStudent.value = student
  drawerOpen.value = true
}

// Contact student modal state
const contactOpen = ref(false)
const contactTarget = ref<SysUserRow | null>(null)
const sendingMessage = ref(false)
const contactForm = reactive({
  subject: '',
  body: '',
})

function openContact(student: SysUserRow) {
  contactTarget.value = student
  contactForm.subject = `Message from Nadoumi Administration`
  contactForm.body = ''
  contactOpen.value = true
}

async function submitContact() {
  if (!contactTarget.value || !contactForm.body.trim()) return
  sendingMessage.value = true
  try {
    await createConversation({
      studentUserId: contactTarget.value.userId,
      subject: contactForm.subject.trim() || 'Message from Nadoumi Administration',
      body: contactForm.body.trim(),
    })
    ElMessage.success(t('students.messageSent'))
    contactOpen.value = false
    ElMessageBox.confirm(
      'Conversation opened. Would you like to view it in Conversations now?',
      'Message Sent',
      {
        confirmButtonText: 'Go to Conversations',
        cancelButtonText: 'Stay Here',
        type: 'success',
      },
    ).then(() => {
      router.push('/conversations')
    }).catch(() => {})
  }
  catch (e) {
    ElMessage.error((e as Error)?.message || 'Failed to send message')
  }
  finally {
    sendingMessage.value = false
  }
}

// Status Management: Suspend ('1'), Block ('2'), Unblock ('0')
async function handleStatusChange(student: SysUserRow, newStatus: string) {
  const name = student.nickName || student.userName
  let confirmMessage = ''
  let confirmTitle = ''

  if (newStatus === '1') {
    confirmTitle = t('students.suspend')
    confirmMessage = t('students.suspendConfirm', { name })
  } else if (newStatus === '2') {
    confirmTitle = t('students.block')
    confirmMessage = t('students.blockConfirm', { name })
  } else {
    confirmTitle = t('students.unblock')
    confirmMessage = t('students.unblockConfirm', { name })
  }

  try {
    await ElMessageBox.confirm(confirmMessage, confirmTitle, {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      type: newStatus === '0' ? 'info' : 'warning',
    })

    await changeUserStatus(student.userId, newStatus)
    ElMessage.success(t('students.statusUpdated'))
    student.status = newStatus
    if (selectedStudent.value?.userId === student.userId) {
      selectedStudent.value.status = newStatus
    }
    load()
  }
  catch (e) {
    if (e !== 'cancel') {
      ElMessage.error((e as Error)?.message || 'Failed to update status')
    }
  }
}

function statusTone(s: string): string {
  if (s === '0') return 'ACTIVE'
  if (s === '1') return 'PENDING'
  return 'FAILED'
}

function statusLabel(s: string): string {
  if (s === '0') return t('students.statusActive')
  if (s === '1') return t('students.statusSuspended')
  return t('students.statusBlocked')
}
</script>

<style scoped>
.student-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}
.student-cell__avatar {
  background: var(--nad-primary, #1e3a8a);
  color: #fff;
  font-weight: 600;
  font-size: 14px;
}
.student-cell__text {
  display: flex;
  flex-direction: column;
}
.student-cell__name {
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.student-cell__handle {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.table-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}
.text-warning {
  color: var(--el-color-warning);
}
.text-danger {
  color: var(--el-color-danger);
}
.text-success {
  color: var(--el-color-success);
}
.contact-form {
  padding-top: 8px;
}
</style>
