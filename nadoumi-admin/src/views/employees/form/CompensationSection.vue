<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import FormSection from '@/components/ui/FormSection.vue'
import { PAY_FREQUENCIES, useEmployeeForm } from '../employeeForm'

const { t } = useI18n()
const form = useEmployeeForm()
</script>

<template>
  <FormSection
    :title="t('employees.secCompensation')"
  >
    <el-form-item :label="t('employees.salary')">
      <el-input-number
        v-model="form.salaryAmount"
        :min="0"
        :controls="false"
        style="width: 160px"
      />
      <el-input
        v-model="form.salaryCurrency"
        :placeholder="t('employees.currency')"
        maxlength="3"
        style="width: 90px; margin-left: 8px"
      />
    </el-form-item>
    <el-form-item :label="t('employees.payFrequency')">
      <el-select
        v-model="form.payFrequency"
        style="width: 100%"
      >
        <el-option
          v-for="f in PAY_FREQUENCIES"
          :key="f"
          :label="t(`employees.freqMap.${f}`)"
          :value="f"
        />
      </el-select>
    </el-form-item>
  </FormSection>
</template>

<style scoped src="../employeeForm.css" />
