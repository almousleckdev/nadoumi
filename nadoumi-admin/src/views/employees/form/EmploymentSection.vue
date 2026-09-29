<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { SysPost, SysUserRow } from '@/api/system'
import FormSection from '@/components/ui/FormSection.vue'
import { EMPLOYMENT_STATUSES, EMPLOYMENT_TYPES, useEmployeeForm, type DeptOption } from '../employeeForm'

defineProps<{ positions: SysPost[], managers: SysUserRow[], deptTree: DeptOption[] }>()

const { t } = useI18n()
const form = useEmployeeForm()
</script>

<template>
  <FormSection :title="t('employees.secEmployment')">
    <el-form-item :label="t('employees.position')">
      <el-select
        v-model="form.positionId"
        clearable
        filterable
        style="width: 100%"
      >
        <el-option
          v-for="p in positions"
          :key="p.postId"
          :label="p.postName"
          :value="p.postId"
        />
      </el-select>
    </el-form-item>
    <el-form-item :label="t('employees.positionTitle')">
      <el-input
        v-model="form.positionTitle"
        :placeholder="t('employees.positionTitleHint')"
      />
    </el-form-item>
    <el-form-item :label="t('employees.dept')">
      <el-tree-select
        v-model="form.deptId"
        :data="deptTree"
        :props="{ label: 'label', children: 'children' }"
        value-key="id"
        node-key="id"
        check-strictly
        clearable
        style="width: 100%"
      />
    </el-form-item>
    <el-form-item :label="t('employees.manager')">
      <el-select
        v-model="form.managerUserId"
        clearable
        filterable
        style="width: 100%"
      >
        <el-option
          v-for="m in managers"
          :key="m.userId"
          :label="`${m.nickName} (${m.userName})`"
          :value="m.userId"
        />
      </el-select>
    </el-form-item>
    <el-form-item :label="t('employees.type')">
      <el-select
        v-model="form.employmentType"
        style="width: 100%"
      >
        <el-option
          v-for="ty in EMPLOYMENT_TYPES"
          :key="ty"
          :label="t(`employees.typeMap.${ty}`)"
          :value="ty"
        />
      </el-select>
    </el-form-item>
    <el-form-item :label="t('employees.status')">
      <el-select
        v-model="form.employmentStatus"
        style="width: 100%"
      >
        <el-option
          v-for="s in EMPLOYMENT_STATUSES"
          :key="s"
          :label="t(`employees.statusMap.${s}`)"
          :value="s"
        />
      </el-select>
    </el-form-item>
    <el-form-item
      :label="t('employees.startDate')"
      prop="startDate"
    >
      <el-date-picker
        v-model="form.startDate"
        type="date"
        value-format="YYYY-MM-DD"
        style="width: 100%"
      />
    </el-form-item>
    <el-form-item :label="t('employees.probationEnd')">
      <el-date-picker
        v-model="form.probationEndDate"
        type="date"
        value-format="YYYY-MM-DD"
        style="width: 100%"
      />
    </el-form-item>
    <el-form-item :label="t('employees.endDate')">
      <el-date-picker
        v-model="form.endDate"
        type="date"
        value-format="YYYY-MM-DD"
        style="width: 100%"
      />
    </el-form-item>
    <el-form-item :label="t('employees.workLocation')">
      <el-input v-model="form.workLocation" />
    </el-form-item>
  </FormSection>
</template>

<style scoped src="../employeeForm.css" />
