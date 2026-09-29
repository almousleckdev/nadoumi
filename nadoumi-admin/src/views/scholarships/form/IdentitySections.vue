<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  EDUCATION_LEVELS, NON_DEGREE_DURATIONS, listScholarshipCategories,
  type ScholarshipCategoryOption,
} from '@/api/scholarship'
import FormSection from '@/components/ui/FormSection.vue'
import { useScholarshipForm } from '../scholarshipForm'

const { t } = useI18n()
const form = useScholarshipForm()

const LANGS = ['ENGLISH', 'CHINESE', 'BOTH'] as const
const FUNDING = ['FULLY', 'PARTIAL', 'SELF'] as const
const PARTIAL_EXCLUDED = ['CSC', 'CGS', 'TYPE_A', 'TYPE_B', 'TYPE_C', 'TYPE_D']

const categories = ref<ScholarshipCategoryOption[]>([])
listScholarshipCategories().then(c => categories.value = c).catch(() => {})

const allowedCategories = computed(() => {
  if (form.fundingModel === 'SELF') return []
  if (form.fundingModel === 'PARTIAL') return categories.value.filter(c => !PARTIAL_EXCLUDED.includes(c.code))
  return categories.value
})
watch(() => form.fundingModel, () => {
  const allowed = new Set(allowedCategories.value.map(c => c.code))
  form.categoryCodes = form.categoryCodes.filter(c => allowed.has(c))
})
</script>

<template>
  <FormSection :title="t('scholarship.secIdentity')">
    <el-form-item
      :label="t('scholarship.title')"
      prop="title"
    >
      <el-input
        v-model="form.title"
        maxlength="200"
      />
    </el-form-item>
    <el-form-item :label="t('scholarship.summary')">
      <el-input
        v-model="form.summary"
        type="textarea"
        :rows="2"
        maxlength="2000"
      />
    </el-form-item>
    <div class="row">
      <el-form-item
        :label="t('scholarship.country')"
        prop="country"
        class="w-28"
      >
        <el-input
          v-model="form.country"
          maxlength="2"
          style="text-transform:uppercase"
        />
      </el-form-item>
      <el-form-item :label="t('scholarship.province')">
        <el-input
          v-model="form.province"
          maxlength="120"
        />
      </el-form-item>
      <el-form-item :label="t('scholarship.city')">
        <el-input
          v-model="form.city"
          maxlength="120"
        />
      </el-form-item>
    </div>
    <el-form-item :label="t('scholarship.field')">
      <el-input
        v-model="form.field"
        maxlength="120"
        :placeholder="t('scholarship.fieldHint')"
      />
      <p class="hint">
        {{ t('scholarship.fieldNote') }}
      </p>
    </el-form-item>
  </FormSection>

  <FormSection :title="t('scholarship.secClassification')">
    <div class="row">
      <el-form-item
        :label="t('scholarship.fundingModel')"
        prop="fundingModel"
      >
        <el-select v-model="form.fundingModel">
          <el-option
            v-for="f in FUNDING"
            :key="f"
            :value="f"
            :label="t(`scholarship.funding.${f}`)"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('scholarship.teachingLanguage')">
        <el-select
          v-model="form.teachingLanguage"
          clearable
        >
          <el-option
            v-for="l in LANGS"
            :key="l"
            :value="l"
            :label="t(`scholarship.lang.${l}`)"
          />
        </el-select>
      </el-form-item>
    </div>
    <el-form-item
      v-if="form.fundingModel !== 'SELF'"
      :label="t('scholarship.categories')"
    >
      <el-select
        v-model="form.categoryCodes"
        multiple
        filterable
        :placeholder="t('scholarship.categoriesHint')"
      >
        <el-option
          v-for="c in allowedCategories"
          :key="c.code"
          :value="c.code"
          :label="c.name"
        />
      </el-select>
      <p
        v-if="form.fundingModel === 'PARTIAL'"
        class="hint"
      >
        {{ t('scholarship.categoriesPartialHint') }}
      </p>
    </el-form-item>
    <el-form-item
      :label="t('scholarship.levels')"
      :required="true"
    >
      <el-checkbox-group v-model="form.levels">
        <el-checkbox
          v-for="lv in EDUCATION_LEVELS"
          :key="lv"
          :value="lv"
          :label="t(`scholarship.level.${lv}`)"
        />
      </el-checkbox-group>
    </el-form-item>
    <el-form-item
      v-if="form.levels.includes('NON_DEGREE')"
      :label="t('scholarship.nonDegreeDuration')"
    >
      <el-select
        v-model="form.nonDegreeDuration"
        clearable
        style="width: 220px"
      >
        <el-option
          v-for="d in NON_DEGREE_DURATIONS"
          :key="d"
          :value="d"
          :label="t(`scholarship.nonDegree.${d}`)"
        />
      </el-select>
    </el-form-item>
  </FormSection>
</template>

<style scoped src="../scholarshipForm.css" />
