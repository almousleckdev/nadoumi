<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  EDUCATION_LEVELS, NON_DEGREE_DURATIONS, listScholarshipCategories,
  type ScholarshipCategoryOption,
} from '@/api/scholarship'
import FormSection from '@/components/ui/FormSection.vue'
import FieldsEditor from './FieldsEditor.vue'
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
  </FormSection>

  <FormSection :title="t('scholarship.secClassification')">
    <el-form-item
      :label="t('scholarship.websiteSections')"
      prop="fundingModel"
    >
      <el-radio-group v-model="form.fundingModel">
        <el-radio-button
          v-for="f in FUNDING"
          :key="f"
          :value="f"
        >
          {{ t(`scholarship.funding.${f}`) }}
        </el-radio-button>
      </el-radio-group>
      <div class="flags">
        <el-checkbox v-model="form.featured">
          {{ t('scholarship.featured') }}
        </el-checkbox>
        <el-checkbox v-model="form.recommended">
          {{ t('scholarship.recommendedByNadoumi') }}
        </el-checkbox>
        <el-checkbox v-model="form.hot">
          {{ t('scholarship.hot') }}
        </el-checkbox>
      </div>
      <p class="hint">
        {{ t('scholarship.websiteSectionsHint') }}
      </p>
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
    <el-form-item :label="t('scholarship.categories')">
      <el-select
        v-model="form.categoryCodes"
        multiple
        filterable
        :disabled="form.fundingModel === 'SELF'"
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
      <p
        v-else-if="form.fundingModel === 'SELF'"
        class="hint"
      >
        {{ t('scholarship.categoriesSelfHint') }}
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
    <FieldsEditor />
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
