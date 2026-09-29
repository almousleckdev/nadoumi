<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Plus, Delete } from '@element-plus/icons-vue'
import { INTAKE_TERMS, type NationalityScope } from '@/api/scholarship'
import FormSection from '@/components/ui/FormSection.vue'
import { useScholarshipForm } from '../scholarshipForm'

const { t } = useI18n()
const form = useScholarshipForm()

const SCOPES: NationalityScope[] = ['ANY', 'INCLUDE', 'EXCLUDE']

const scopeHint = computed(() => {
  if (form.eligibility.nationalityScope === 'INCLUDE') return t('scholarship.scopeIncludeHint')
  if (form.eligibility.nationalityScope === 'EXCLUDE') return t('scholarship.scopeExcludeHint')
  return t('scholarship.scopeAnyHint')
})
</script>

<template>
  <FormSection
    :title="t('scholarship.secIntakes')"
    :description="t('scholarship.intakesHint')"
  >
    <div
      v-for="(it, i) in form.intakes"
      :key="i"
      class="line"
    >
      <el-select
        v-model="it.term"
        class="w-44"
      >
        <el-option
          v-for="term in INTAKE_TERMS"
          :key="term"
          :value="term"
          :label="t(`scholarship.intake.${term}`)"
        />
      </el-select>
      <el-date-picker
        v-model="it.applicationOpen"
        type="date"
        value-format="YYYY-MM-DD"
        :placeholder="t('scholarship.opens')"
      />
      <el-date-picker
        v-model="it.applicationClose"
        type="date"
        value-format="YYYY-MM-DD"
        :placeholder="t('scholarship.closes')"
      />
      <el-button
        link
        type="danger"
        :icon="Delete"
        @click="form.intakes.splice(i, 1)"
      />
    </div>
    <el-button
      :icon="Plus"
      @click="form.intakes.push({ term: 'AUTUMN_SEPTEMBER', applicationOpen: null, applicationClose: null })"
    >
      {{ t('scholarship.addIntake') }}
    </el-button>
  </FormSection>

  <FormSection :title="t('scholarship.secEligibility')">
    <div class="row">
      <el-form-item
        :label="t('scholarship.ageMin')"
        class="w-28"
      >
        <el-input-number
          v-model="form.eligibility.ageMin"
          :min="0"
          :max="120"
          controls-position="right"
        />
      </el-form-item>
      <el-form-item
        :label="t('scholarship.ageMax')"
        class="w-28"
      >
        <el-input-number
          v-model="form.eligibility.ageMax"
          :min="0"
          :max="120"
          controls-position="right"
        />
      </el-form-item>
      <el-form-item :label="t('scholarship.inChina')">
        <el-select
          v-model="form.eligibility.inChina"
          clearable
          :placeholder="t('scholarship.inChinaEither')"
        >
          <el-option
            :value="true"
            :label="t('scholarship.inChinaYes')"
          />
          <el-option
            :value="false"
            :label="t('scholarship.inChinaNo')"
          />
        </el-select>
      </el-form-item>
    </div>
    <p class="hint">
      {{ t('scholarship.inChinaHint') }}
    </p>
    <el-form-item :label="t('scholarship.nationality')">
      <el-radio-group v-model="form.eligibility.nationalityScope">
        <el-radio
          v-for="sc in SCOPES"
          :key="sc"
          :value="sc"
        >
          {{ t(`scholarship.scope.${sc}`) }}
        </el-radio>
      </el-radio-group>
      <p class="hint">
        {{ scopeHint }}
      </p>
    </el-form-item>
    <el-form-item
      v-if="form.eligibility.nationalityScope !== 'ANY'"
      :label="t('scholarship.countryList')"
    >
      <el-input
        v-model="form.eligibility.acceptedCountries"
        :placeholder="t('scholarship.countryListHint')"
      />
    </el-form-item>
    <div class="row">
      <el-form-item
        label="GPA min"
        class="w-28"
      >
        <el-input-number
          v-model="form.eligibility.gpaMin"
          :min="0"
          :max="5"
          :step="0.1"
          :precision="2"
          controls-position="right"
        />
      </el-form-item>
      <el-form-item
        label="IELTS min"
        class="w-28"
      >
        <el-input-number
          v-model="form.eligibility.ieltsMin"
          :min="0"
          :max="9"
          :step="0.5"
          :precision="1"
          controls-position="right"
        />
      </el-form-item>
      <el-form-item
        label="TOEFL min"
        class="w-28"
      >
        <el-input-number
          v-model="form.eligibility.toeflMin"
          :min="0"
          :max="120"
          controls-position="right"
        />
      </el-form-item>
    </div>
    <div class="row">
      <el-form-item
        label="Duolingo min"
        class="w-28"
      >
        <el-input-number
          v-model="form.eligibility.duolingoMin"
          :min="0"
          :max="160"
          controls-position="right"
        />
      </el-form-item>
      <el-form-item
        label="HSK min"
        class="w-28"
      >
        <el-input-number
          v-model="form.eligibility.hskMin"
          :min="0"
          :max="9"
          controls-position="right"
        />
      </el-form-item>
      <el-form-item
        label="CSCA min"
        class="w-28"
      >
        <el-input-number
          v-model="form.eligibility.cscaMin"
          :min="0"
          controls-position="right"
        />
      </el-form-item>
    </div>
    <el-form-item :label="t('scholarship.eligNotes')">
      <el-input
        v-model="form.eligibility.notes"
        type="textarea"
        :rows="2"
        maxlength="2000"
      />
    </el-form-item>
  </FormSection>
</template>

<style scoped src="../scholarshipForm.css" />
