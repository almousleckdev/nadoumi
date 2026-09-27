<template>
  <el-drawer
    v-model="visible"
    :title="t('students.studentDetails')"
    size="520px"
    destroy-on-close
  >
    <div
      v-if="student"
      class="student-drawer"
    >
      <div class="student-header">
        <el-avatar
          :size="64"
          :src="student.avatar || undefined"
          class="student-avatar"
        >
          {{ (student.nickName || student.userName || 'S').slice(0, 1).toUpperCase() }}
        </el-avatar>
        <div class="student-header__info">
          <h3 class="student-name">
            {{ student.nickName || student.userName }}
          </h3>
          <div class="student-handle">
            @{{ student.userName }}
          </div>
          <div class="student-badge">
            <StatusBadge
              :status="statusTone(student.status)"
              :label="statusLabel(student.status)"
            />
          </div>
        </div>
      </div>

      <el-divider />

      <h4 class="section-title">
        {{ t('students.accountInfo') }}
      </h4>
      <el-descriptions
        :column="1"
        border
        size="small"
      >
        <el-descriptions-item :label="t('students.username')">
          {{ student.userName }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('students.email')">
          {{ student.email || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('students.phone')">
          {{ student.phonenumber || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('students.registered')">
          {{ student.createTime || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('students.lastLogin')">
          {{ student.loginDate || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('user.ip')">
          {{ (student as any).loginIp || '-' }}
        </el-descriptions-item>
      </el-descriptions>

      <div class="drawer-actions">
        <el-button
          type="primary"
          :icon="ChatDotRound"
          @click="emit('contact', student)"
        >
          {{ t('students.contactStudent') }}
        </el-button>

        <el-button
          v-if="student.status === '0'"
          type="warning"
          plain
          @click="emit('change-status', student, '1')"
        >
          {{ t('students.suspend') }}
        </el-button>

        <el-button
          v-if="student.status === '0' || student.status === '1'"
          type="danger"
          plain
          @click="emit('change-status', student, '2')"
        >
          {{ t('students.block') }}
        </el-button>

        <el-button
          v-if="student.status === '1' || student.status === '2'"
          type="success"
          plain
          @click="emit('change-status', student, '0')"
        >
          {{ t('students.unblock') }}
        </el-button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ChatDotRound } from '@element-plus/icons-vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import type { SysUserRow } from '@/api/system'

const props = defineProps<{
  modelValue: boolean
  student?: SysUserRow | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void
  (e: 'contact', student: SysUserRow): void
  (e: 'change-status', student: SysUserRow, newStatus: string): void
}>()

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: val => emit('update:modelValue', val),
})

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
.student-drawer {
  padding: 8px 4px;
}
.student-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 8px;
}
.student-avatar {
  background: var(--nad-primary, #1e3a8a);
  color: #fff;
  font-weight: 600;
  font-size: 20px;
}
.student-header__info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.student-name {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.student-handle {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.student-badge {
  margin-top: 4px;
}
.section-title {
  margin: 16px 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-regular);
}
.drawer-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}
</style>
