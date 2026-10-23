<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import FormSection from '@/components/ui/FormSection.vue'
import { useUsdHint } from '@/composables/useUsdHint'
import { useProgramForm } from '../programForm'

const { t } = useI18n()
const form = useProgramForm()
const { usdHint } = useUsdHint()
</script>

<template>
  <FormSection
    :title="t('program.admission.title')"
    :description="t('program.admission.hint')"
  >
    <el-form-item :label="t('program.admission.requirements')">
      <el-input
        v-model="form.admissionRequirements"
        type="textarea"
        :rows="3"
        maxlength="4000"
      />
    </el-form-item>
    <div class="row">
      <el-form-item :label="t('program.admission.minHsk')">
        <el-input-number
          v-model="form.minHsk"
          :min="1"
          :max="6"
          controls-position="right"
        />
      </el-form-item>
      <el-form-item :label="t('program.admission.minIelts')">
        <el-input-number
          v-model="form.minIelts"
          :min="0"
          :max="9"
          :step="0.5"
          :precision="1"
          controls-position="right"
        />
      </el-form-item>
      <el-form-item :label="t('program.admission.minToefl')">
        <el-input-number
          v-model="form.minToefl"
          :min="0"
          :max="120"
          controls-position="right"
        />
      </el-form-item>
    </div>
    <div class="row">
      <el-form-item :label="t('program.admission.applicationFee')">
        <el-input-number
          v-model="form.applicationFeeAmount"
          :min="0"
          :precision="2"
          :step="100"
          controls-position="right"
        />
        <span class="tuition-usd">{{ usdHint(form.applicationFeeAmount) }}</span>
      </el-form-item>
    </div>
    <el-form-item :label="t('program.admission.scholarships')">
      <el-input
        v-model="form.scholarshipInfo"
        type="textarea"
        :rows="3"
        maxlength="4000"
      />
    </el-form-item>
    <el-form-item :label="t('program.admission.career')">
      <el-input
        v-model="form.careerProspects"
        type="textarea"
        :rows="3"
        maxlength="4000"
      />
    </el-form-item>
  </FormSection>
</template>

<style scoped src="../programForm.css" />
