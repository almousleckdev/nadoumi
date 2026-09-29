<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { SysRole } from '@/api/system'
import FormSection from '@/components/ui/FormSection.vue'
import { useEmployeeForm } from '../employeeForm'

defineProps<{ roles: SysRole[], isEdit: boolean }>()

const { t } = useI18n()
const form = useEmployeeForm()
</script>

<template>
  <FormSection :title="t('employees.secAccount')">
    <el-form-item
      v-if="!isEdit"
      :label="t('employees.userName')"
      prop="userName"
    >
      <el-input v-model="form.userName" />
    </el-form-item>
    <el-form-item
      v-if="!isEdit"
      :label="t('employees.password')"
      prop="password"
    >
      <el-input
        v-model="form.password"
        type="password"
        show-password
      />
    </el-form-item>
    <el-form-item
      :label="t('employees.displayName')"
      prop="nickName"
    >
      <el-input v-model="form.nickName" />
    </el-form-item>
    <el-form-item
      :label="t('employees.email')"
      prop="email"
    >
      <el-input v-model="form.email" />
    </el-form-item>
    <el-form-item :label="t('employees.phone')">
      <el-input v-model="form.phone" />
    </el-form-item>
    <el-form-item :label="t('employees.accountActive')">
      <el-switch
        v-model="form.userStatus"
        active-value="0"
        inactive-value="1"
      />
    </el-form-item>
    <el-form-item :label="t('employees.roles')">
      <el-select
        v-model="form.roleIds"
        multiple
        style="width: 100%"
        :placeholder="t('employees.rolesPlaceholder')"
      >
        <el-option
          v-for="r in roles"
          :key="r.roleId"
          :label="r.roleName"
          :value="r.roleId"
        />
      </el-select>
      <div class="nad-field-hint">
        {{ t('employees.rolesHint') }}
      </div>
    </el-form-item>
  </FormSection>
</template>

<style scoped src="../employeeForm.css" />
