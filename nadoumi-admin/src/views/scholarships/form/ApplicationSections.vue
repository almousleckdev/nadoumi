<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { Plus, Delete } from '@element-plus/icons-vue'
import { APPLICATION_CHANNELS, DOC_TYPES } from '@/api/scholarship'
import FormSection from '@/components/ui/FormSection.vue'
import { useScholarshipForm } from '../scholarshipForm'

const { t } = useI18n()
const form = useScholarshipForm()
</script>

<template>
  <FormSection :title="t('scholarship.secApplication')">
    <div class="row">
      <el-form-item :label="t('scholarship.studyDurationMonths')">
        <el-input-number
          v-model="form.studyDurationMonths"
          :min="1"
          :max="120"
          controls-position="right"
        />
      </el-form-item>
      <el-form-item :label="t('scholarship.applicationChannel')">
        <el-select
          v-model="form.applicationChannel"
          clearable
        >
          <el-option
            v-for="ch in APPLICATION_CHANNELS"
            :key="ch"
            :value="ch"
            :label="t(`scholarship.channel.${ch}`)"
          />
        </el-select>
      </el-form-item>
      <el-form-item
        v-if="form.applicationChannel === 'CSC_AGENCY'"
        :label="t('scholarship.agencyNumber')"
      >
        <el-input
          v-model="form.agencyNumber"
          maxlength="64"
        />
      </el-form-item>
    </div>
    <el-form-item>
      <el-checkbox v-model="form.requiresFinancialProof">
        {{ t('scholarship.requiresFinancialProof') }}
      </el-checkbox>
    </el-form-item>
    <el-form-item>
      <el-checkbox v-model="form.requiresFoundationYear">
        {{ t('scholarship.requiresFoundationYear') }}
      </el-checkbox>
    </el-form-item>
  </FormSection>

  <FormSection
    :title="t('scholarship.secDocuments')"
    :description="t('scholarship.documentsHint')"
  >
    <div
      v-for="(d, i) in form.documentRequirements"
      :key="i"
      class="line"
    >
      <el-select
        v-model="d.docType"
        class="w-52"
        filterable
        allow-create
        default-first-option
      >
        <el-option
          v-for="dt in DOC_TYPES"
          :key="dt"
          :value="dt"
          :label="t(`scholarship.doc.${dt}`, dt)"
        />
      </el-select>
      <el-checkbox v-model="d.mandatory">
        {{ t('scholarship.mandatory') }}
      </el-checkbox>
      <el-input
        v-model="d.note"
        :placeholder="t('scholarship.note')"
      />
      <el-button
        link
        type="danger"
        :icon="Delete"
        @click="form.documentRequirements.splice(i, 1)"
      />
    </div>
    <el-button
      :icon="Plus"
      @click="form.documentRequirements.push({ docType: 'PASSPORT', mandatory: true, note: '' })"
    >
      {{ t('scholarship.addDocument') }}
    </el-button>
  </FormSection>
</template>

<style scoped src="../scholarshipForm.css" />
