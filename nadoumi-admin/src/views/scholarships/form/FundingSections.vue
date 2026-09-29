<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { Plus, Delete } from '@element-plus/icons-vue'
import { COVERAGE_KINDS, EDUCATION_LEVELS, FEE_KINDS, ROOM_TYPES } from '@/api/scholarship'
import FormSection from '@/components/ui/FormSection.vue'
import { useUsdHint } from '@/composables/useUsdHint'
import { useScholarshipForm } from '../scholarshipForm'

const { t } = useI18n()
const form = useScholarshipForm()
const { usdHint } = useUsdHint()

const FREQ = ['MONTHLY', 'YEARLY', 'ONE_OFF'] as const
</script>

<template>
  <FormSection
    :title="t('scholarship.secFees')"
    :description="t('scholarship.feesHint')"
  >
    <div class="row">
      <el-form-item :label="t('scholarship.applicationFeeRmb')">
        <el-input-number
          v-model="form.applicationFeeAmount"
          :min="0"
          :precision="2"
          :step="100"
          controls-position="right"
          style="width: 100%"
        />
        <span class="usd">{{ usdHint(form.applicationFeeAmount) }}</span>
      </el-form-item>
      <el-form-item :label="t('scholarship.serviceFeeRmb')">
        <el-input-number
          v-model="form.serviceFeeAmount"
          :min="0"
          :precision="2"
          :step="100"
          controls-position="right"
          style="width: 100%"
        />
        <span class="usd">{{ usdHint(form.serviceFeeAmount) }}</span>
      </el-form-item>
    </div>
    <p class="hint">
      {{ t('scholarship.feeLinesHint') }}
    </p>
    <div
      v-for="(f, i) in form.fees"
      :key="i"
      class="line"
    >
      <el-select
        v-model="f.kind"
        class="w-56"
        filterable
      >
        <el-option
          v-for="k in FEE_KINDS"
          :key="k"
          :value="k"
          :label="t(`scholarship.fee.${k}`)"
        />
      </el-select>
      <el-input-number
        v-model="f.amount"
        :min="0"
        :precision="2"
        :step="1000"
        controls-position="right"
      />
      <span class="usd">{{ usdHint(f.amount) }}</span>
      <el-input
        v-model="f.note"
        :placeholder="t('scholarship.note')"
      />
      <el-button
        link
        type="danger"
        :icon="Delete"
        @click="form.fees.splice(i, 1)"
      />
    </div>
    <el-button
      :icon="Plus"
      @click="form.fees.push({ kind: 'TUITION_AFTER', amount: null, currency: 'CNY', note: '' })"
    >
      {{ t('scholarship.addFee') }}
    </el-button>
  </FormSection>

  <FormSection
    :title="t('scholarship.secStipend')"
    :description="t('scholarship.stipendPerLevelHint')"
  >
    <div
      v-for="(st, i) in form.levelStipends"
      :key="i"
      class="line"
    >
      <el-select
        v-model="st.level"
        class="w-44"
      >
        <el-option
          v-for="lv in form.levels.length ? form.levels : EDUCATION_LEVELS"
          :key="lv"
          :value="lv"
          :label="t(`scholarship.level.${lv}`)"
        />
      </el-select>
      <el-input-number
        v-model="st.amount"
        :min="0"
        :precision="2"
        :step="1000"
        controls-position="right"
        :placeholder="t('scholarship.amountRmb')"
      />
      <span class="usd">{{ usdHint(st.amount) }}</span>
      <el-select
        v-model="st.frequency"
        class="w-44"
      >
        <el-option
          v-for="fr in FREQ"
          :key="fr"
          :value="fr"
          :label="t(`scholarship.freq.${fr}`)"
        />
      </el-select>
      <el-input-number
        v-model="st.durationMonths"
        :min="0"
        controls-position="right"
        :placeholder="t('scholarship.durationMonths')"
        class="w-28"
      />
      <el-input
        v-model="st.conditions"
        :placeholder="t('scholarship.stipendConditions')"
      />
      <el-button
        link
        type="danger"
        :icon="Delete"
        @click="form.levelStipends.splice(i, 1)"
      />
    </div>
    <el-button
      :icon="Plus"
      @click="form.levelStipends.push({ level: (form.levels[0] ?? 'MASTER'), amount: null, currency: 'CNY', frequency: 'MONTHLY', durationMonths: null, conditions: '' })"
    >
      {{ t('scholarship.addStipend') }}
    </el-button>
  </FormSection>

  <FormSection
    :title="t('scholarship.secAccommodation')"
    :description="t('scholarship.accommodationHint')"
  >
    <div
      v-for="(a, i) in form.accommodations"
      :key="i"
      class="line"
    >
      <el-select
        v-model="a.roomType"
        class="w-44"
      >
        <el-option
          v-for="rt in ROOM_TYPES"
          :key="rt"
          :value="rt"
          :label="t(`scholarship.room.${rt}`)"
        />
      </el-select>
      <el-input-number
        v-model="a.amount"
        :min="0"
        :precision="2"
        :step="500"
        controls-position="right"
        :placeholder="t('scholarship.amountRmb')"
      />
      <span class="usd">{{ usdHint(a.amount) }}</span>
      <el-input
        v-model="a.note"
        :placeholder="t('scholarship.accommodationNote')"
        maxlength="200"
      />
      <el-button
        link
        type="danger"
        :icon="Delete"
        @click="form.accommodations.splice(i, 1)"
      />
    </div>
    <el-button
      :icon="Plus"
      @click="form.accommodations.push({ roomType: 'SINGLE', amount: null, currency: 'CNY', note: '' })"
    >
      {{ t('scholarship.addAccommodation') }}
    </el-button>
  </FormSection>

  <FormSection
    :title="t('scholarship.secCoverage')"
    :description="t('scholarship.coverageHint')"
  >
    <div
      v-for="(c, i) in form.coverage"
      :key="i"
      class="line"
    >
      <el-select
        v-model="c.kind"
        class="w-52"
      >
        <el-option
          v-for="ck in COVERAGE_KINDS"
          :key="ck"
          :value="ck"
          :label="t(`scholarship.coverageKind.${ck}`)"
        />
      </el-select>
      <el-input
        v-model="c.detail"
        :placeholder="t('scholarship.coverageDetail')"
        maxlength="300"
      />
      <el-button
        link
        type="danger"
        :icon="Delete"
        @click="form.coverage.splice(i, 1)"
      />
    </div>
    <el-button
      :icon="Plus"
      @click="form.coverage.push({ kind: 'TUITION', detail: '' })"
    >
      {{ t('scholarship.addCoverage') }}
    </el-button>
  </FormSection>
</template>

<style scoped src="../scholarshipForm.css" />
