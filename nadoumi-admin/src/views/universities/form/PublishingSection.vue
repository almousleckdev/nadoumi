<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { PARTNER_STATUSES } from '@/api/university'
import FormSection from '@/components/ui/FormSection.vue'
import { titleCase } from '@/utils/text'
import { useUniversityForm } from '../universityForm'

const { t } = useI18n()
const form = useUniversityForm()

const STATUSES = ['ACTIVE', 'INACTIVE'] as const
const PUBLISH = ['DRAFT', 'PUBLISHED'] as const
</script>

<template>
  <FormSection :title="t('university.secPublication')">
    <div class="row2">
      <el-form-item
        :label="t('university.status')"
        prop="status"
      >
        <el-select
          v-model="form.status"
          style="width: 100%"
        >
          <el-option
            v-for="s in STATUSES"
            :key="s"
            :label="titleCase(s)"
            :value="s"
          />
        </el-select>
      </el-form-item>
      <el-form-item
        :label="t('university.publishStatus')"
        prop="publishStatus"
      >
        <el-select
          v-model="form.publishStatus"
          style="width: 100%"
        >
          <el-option
            v-for="s in PUBLISH"
            :key="s"
            :label="titleCase(s)"
            :value="s"
          />
        </el-select>
      </el-form-item>
    </div>
    <div class="flags">
      <el-checkbox v-model="form.recommended">
        {{ t('university.recommended') }}
      </el-checkbox>
      <el-checkbox v-model="form.featured">
        {{ t('university.featured') }}
      </el-checkbox>
    </div>
    <el-form-item :label="t('university.partnerStatus')">
      <el-radio-group v-model="form.partnerStatus">
        <el-radio-button
          v-for="ps in PARTNER_STATUSES"
          :key="ps"
          :value="ps"
        >
          {{ t(`university.partnerMap.${ps}`) }}
        </el-radio-button>
      </el-radio-group>
      <p class="hint">
        {{ t('university.partnerHint') }}
      </p>
    </el-form-item>
    <el-form-item :label="t('university.publicPartner')">
      <el-switch v-model="form.publicPartner" />
      <p class="hint">
        {{ t('university.publicPartnerHint') }}
      </p>
    </el-form-item>
    <div class="row2">
      <el-form-item :label="t('university.admissionsEmail')">
        <el-input v-model="form.admissionsEmail" />
      </el-form-item>
      <el-form-item :label="t('university.officePhone')">
        <el-input v-model="form.officePhone" />
      </el-form-item>
    </div>
    <el-form-item :label="t('university.remark')">
      <el-input
        v-model="form.remark"
        type="textarea"
        :rows="2"
        maxlength="500"
        show-word-limit
      />
    </el-form-item>
  </FormSection>
</template>

<style scoped src="../universityForm.css" />
