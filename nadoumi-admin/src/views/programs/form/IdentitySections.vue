<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  PROGRAM_TYPES, PROGRAM_LANGUAGES, TERM_LENGTHS, DEGREE_LEVELS,
  isDegree as isDegreeKind,
} from '@/api/program'
import FormSection from '@/components/ui/FormSection.vue'
import { useUsdHint } from '@/composables/useUsdHint'
import { useProgramForm } from '../programForm'

defineProps<{
  universities: { id: number, name: string }[]
  lockedUniversity?: { id: number, name: string } | null
}>()
const emit = defineEmits<{ universityChange: [] }>()

const { t } = useI18n()
const form = useProgramForm()
const { usdHint } = useUsdHint()

const isDegree = computed(() => isDegreeKind(form.programType))
const tuitionUsdPreview = computed(() => usdHint(form.tuitionAmount))
</script>

<template>
  <FormSection :title="t('program.secIdentity')">
    <el-form-item
      :label="t('program.university')"
      prop="universityId"
    >
      <el-input
        v-if="lockedUniversity"
        :model-value="lockedUniversity.name"
        disabled
      />
      <el-select
        v-else
        v-model="form.universityId"
        filterable
        :placeholder="t('program.universityPlaceholder')"
        style="width: 100%"
        @change="emit('universityChange')"
      >
        <el-option
          v-for="u in universities"
          :key="u.id"
          :label="u.name"
          :value="u.id"
        />
      </el-select>
    </el-form-item>
    <div class="row">
      <el-form-item
        :label="t('program.name')"
        prop="name"
      >
        <el-input
          v-model="form.name"
          maxlength="200"
        />
      </el-form-item>
      <el-form-item :label="t('program.nameCn')">
        <el-input
          v-model="form.nameCn"
          maxlength="200"
        />
      </el-form-item>
    </div>
    <el-form-item :label="t('program.summary')">
      <el-input
        v-model="form.summary"
        type="textarea"
        :rows="2"
        maxlength="4000"
      />
    </el-form-item>
  </FormSection>

  <FormSection :title="t('program.secClassification')">
    <div class="row">
      <el-form-item
        :label="t('program.type')"
        prop="programType"
      >
        <el-select
          v-model="form.programType"
          style="width: 100%"
        >
          <el-option
            v-for="pt in PROGRAM_TYPES"
            :key="pt"
            :value="pt"
            :label="t(`program.typeMap.${pt}`)"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('program.language')">
        <el-select
          v-model="form.teachingLanguage"
          clearable
          style="width: 100%"
        >
          <el-option
            v-for="l in PROGRAM_LANGUAGES"
            :key="l"
            :value="l"
            :label="t(`program.langMap.${l}`)"
          />
        </el-select>
      </el-form-item>
    </div>
    <el-form-item
      v-if="isDegree"
      :label="t('program.levels')"
    >
      <el-checkbox-group v-model="form.levels">
        <el-checkbox
          v-for="lv in DEGREE_LEVELS"
          :key="lv"
          :value="lv"
          :label="t(`program.levelMap.${lv}`)"
        />
      </el-checkbox-group>
      <p class="fx-note">
        {{ t('program.levelsHint') }}
      </p>
    </el-form-item>
    <div class="row">
      <el-form-item :label="t('program.durationMonths')">
        <el-input-number
          v-model="form.durationMonths"
          :min="1"
          :max="120"
          controls-position="right"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item
        v-if="!isDegree"
        :label="t('program.termLength')"
      >
        <el-select
          v-model="form.termLength"
          clearable
          style="width: 100%"
          :placeholder="t('program.termLengthPlaceholder')"
        >
          <el-option
            v-for="tl in TERM_LENGTHS"
            :key="tl"
            :value="tl"
            :label="t(`program.termLengthMap.${tl}`)"
          />
        </el-select>
      </el-form-item>
    </div>
    <div class="row">
      <el-form-item
        :label="t('program.tuitionAmountRmb')"
        class="w-40"
      >
        <el-input-number
          v-model="form.tuitionAmount"
          :min="0"
          :precision="2"
          :step="1000"
          controls-position="right"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item :label="t('program.tuitionUsd')">
        <span class="tuition-usd">{{ tuitionUsdPreview }}</span>
      </el-form-item>
    </div>
    <p class="fx-note">
      {{ t('program.tuitionFxNote') }}
    </p>
  </FormSection>
</template>

<style scoped src="../programForm.css" />
