<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import FormSection from '@/components/ui/FormSection.vue'
import { useEmployeeForm } from '../employeeForm'

const { t } = useI18n()
const form = useEmployeeForm()
</script>

<template>
  <FormSection :title="t('employees.secOther')">
    <div class="row2">
      <el-form-item :label="t('employees.emergencyContactName')">
        <el-input
          v-model="form.emergencyContact"
          maxlength="200"
        />
      </el-form-item>
      <el-form-item :label="t('employees.emergencyContactRelationship')">
        <el-input
          v-model="form.emergencyContactRelationship"
          maxlength="60"
          :placeholder="t('employees.emergencyContactRelationshipHint')"
        />
      </el-form-item>
    </div>
    <div class="row2">
      <el-form-item :label="t('employees.emergencyContactPhone')">
        <el-input
          v-model="form.emergencyContactPhone"
          maxlength="32"
        />
      </el-form-item>
      <el-form-item :label="t('employees.emergencyContactEmail')">
        <el-input
          v-model="form.emergencyContactEmail"
          maxlength="120"
          :placeholder="t('common.optional')"
        />
      </el-form-item>
    </div>
    <el-form-item :label="t('employees.notes')">
      <el-input
        v-model="form.notes"
        type="textarea"
        :rows="3"
      />
    </el-form-item>
  </FormSection>
</template>

<style scoped src="../employeeForm.css" />
