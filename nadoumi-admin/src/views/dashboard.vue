
<template>
  <div class="nad-page">
    <el-alert
      v-if="userStore.mustChangePassword"
      :title="t('profile.initialWarning')"
      type="warning"
      show-icon
      :closable="false"
      style="margin-bottom: 16px"
    >
      <el-button size="small" type="primary" @click="$router.push('/profile')">
        {{ t('profile.changePassword') }}
      </el-button>
    </el-alert>

    <el-row :gutter="16">
      <el-col :span="8">
        <el-card>
          <div class="stat">
            <div class="stat__label">Signed in as</div>
            <div class="stat__value">{{ userStore.nickName || userStore.name }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <div class="stat">
            <div class="stat__label">Roles</div>
            <div class="stat__value">{{ userStore.roles.join(', ') || '—' }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <div class="stat">
            <div class="stat__label">Permissions</div>
            <div class="stat__value">{{ userStore.permissions.length }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 16px">
      <p>
        This is the Nadoumi internal admin console. The sidebar is built live from the backend
        (<code>/getRouters</code>). Screens not yet implemented in this console show a placeholder.
      </p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'

const { t } = useI18n()
const userStore = useUserStore()
</script>

<style scoped>
.stat__label {
  color: #909399;
  font-size: 13px;
}
.stat__value {
  font-size: 18px;
  font-weight: 600;
  margin-top: 4px;
  word-break: break-word;
}
</style>
