<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { listScholarshipCategories, type ScholarshipCategoryOption } from '@/api/scholarship'
import FormSection from '@/components/ui/FormSection.vue'
import { useUsdHint } from '@/composables/useUsdHint'
import { useProgramForm } from '../programForm'

const { t } = useI18n()
const form = useProgramForm()
const { usdHint } = useUsdHint()

// some programmes have scholarships and some do not; "none" is a real choice, not an empty box
const offersScholarships = ref(form.scholarshipCategories.length > 0 || form.scholarshipInfo.trim().length > 0)
const categories = ref<ScholarshipCategoryOption[]>([])
listScholarshipCategories().then(c => categories.value = c).catch(() => undefined)

const needsTypes = computed(() => offersScholarships.value && form.scholarshipCategories.length === 0)

function onOffersChange(offers: string | number | boolean | undefined) {
  if (offers === false) {
    form.scholarshipCategories = []
    form.scholarshipInfo = ''
  }
}
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
      <el-form-item :label="t('program.admission.serviceFee')">
        <el-input-number
          v-model="form.serviceFeeAmount"
          :min="0"
          :precision="2"
          :step="100"
          controls-position="right"
        />
        <span class="tuition-usd">{{ usdHint(form.serviceFeeAmount) }}</span>
      </el-form-item>
    </div>
    <el-form-item :label="t('program.admission.scholarships')">
      <el-radio-group
        v-model="offersScholarships"
        @change="onOffersChange"
      >
        <el-radio :value="false">
          {{ t('program.admission.scholarshipsNone') }}
        </el-radio>
        <el-radio :value="true">
          {{ t('program.admission.scholarshipsAvailable') }}
        </el-radio>
      </el-radio-group>
    </el-form-item>
    <template v-if="offersScholarships">
      <el-form-item :label="t('program.admission.scholarshipTypes')">
        <el-select
          v-model="form.scholarshipCategories"
          multiple
          filterable
          :placeholder="t('program.admission.scholarshipTypesPlaceholder')"
          style="width: 100%"
        >
          <el-option
            v-for="c in categories"
            :key="c.code"
            :value="c.code"
            :label="c.name"
          />
        </el-select>
        <p
          v-if="needsTypes"
          class="field-hint"
        >
          {{ t('program.admission.scholarshipTypesNeeded') }}
        </p>
      </el-form-item>
      <el-form-item :label="t('program.admission.scholarshipDetails')">
        <el-input
          v-model="form.scholarshipInfo"
          type="textarea"
          :rows="3"
          maxlength="4000"
        />
      </el-form-item>
    </template>
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
