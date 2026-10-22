<script setup lang="ts">
import { computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Plus, Delete } from '@element-plus/icons-vue'
import {
  ACCOMMODATION_PERIODS, DURATION_OPTIONS, MIN_DURATION_MONTHS, TUITION_PERIODS,
  isDegree as isDegreeKind, type DegreeLevel,
} from '@/api/program'
import FormSection from '@/components/ui/FormSection.vue'
import { useUsdHint } from '@/composables/useUsdHint'
import { blankOffering, offeringProblems, syncOfferingsToLevels, useProgramForm, type OfferingRow } from '../programForm'

const { t } = useI18n()
const form = useProgramForm()
const { usdHint } = useUsdHint()

const isDegree = computed(() => isDegreeKind(form.programType))
const problems = computed(() => offeringProblems(form))

// every ticked level gets a row; unticking a level removes its rows
watch(() => [form.programType, [...form.levels]] as const, () => syncOfferingsToLevels(form), { immediate: true })

function durationOptions(level: DegreeLevel | null): number[] {
  return level ? DURATION_OPTIONS[level] : DURATION_OPTIONS.OTHER
}

function durationLabel(months: number): string {
  const years = months / 12
  return Number.isInteger(years)
    ? t('program.offerings.yearsAndMonths', { years, months })
    : t('program.offerings.months', { months })
}

function minFor(level: DegreeLevel | null): number {
  return level ? MIN_DURATION_MONTHS[level] : 1
}

function problemText(p: ReturnType<typeof offeringProblems>[number]): string {
  const level = p.level ? t(`program.levelMap.${p.level}`) : t('program.offerings.programme')
  if (p.kind === 'TOO_SHORT') return t('program.offerings.tooShort', { level, min: p.min })
  if (p.kind === 'DUPLICATE') return t('program.offerings.duplicate', { level })
  return t('program.offerings.missingDuration', { level })
}

function addRow() {
  form.offerings.push(blankOffering(isDegree.value ? form.levels[0] ?? null : null))
}

function onLevelChange(row: OfferingRow) {
  const min = minFor(row.level)
  if (row.durationMonths == null || row.durationMonths < min) row.durationMonths = durationOptions(row.level)[0] ?? min
}
</script>

<template>
  <FormSection
    :title="t('program.offerings.title')"
    :description="t(isDegree ? 'program.offerings.hintDegree' : 'program.offerings.hintOther')"
  >
    <p
      v-if="isDegree && !form.levels.length"
      class="fx-note"
    >
      {{ t('program.offerings.pickLevels') }}
    </p>

    <div
      v-for="(o, i) in form.offerings"
      :key="i"
      class="offering"
    >
      <div class="offering__head">
        <el-select
          v-if="isDegree"
          v-model="o.level"
          class="w-32"
          @change="onLevelChange(o)"
        >
          <el-option
            v-for="lv in form.levels"
            :key="lv"
            :value="lv"
            :label="t(`program.levelMap.${lv}`)"
          />
        </el-select>
        <el-select
          v-model="o.durationMonths"
          filterable
          allow-create
          default-first-option
          class="w-44"
          :placeholder="t('program.offerings.duration')"
        >
          <el-option
            v-for="m in durationOptions(o.level)"
            :key="m"
            :value="m"
            :label="durationLabel(m)"
          />
        </el-select>
        <el-button
          link
          type="danger"
          :icon="Delete"
          @click="form.offerings.splice(i, 1)"
        />
      </div>
      <div class="offering__money">
        <el-form-item :label="t('program.offerings.tuition')">
          <el-input-number
            v-model="o.tuitionAmount"
            :min="0"
            :precision="2"
            :step="1000"
            controls-position="right"
          />
          <el-select
            v-model="o.tuitionPeriod"
            class="w-32"
          >
            <el-option
              v-for="p in TUITION_PERIODS"
              :key="p"
              :value="p"
              :label="t(`program.offerings.period.${p}`)"
            />
          </el-select>
          <span class="tuition-usd">{{ usdHint(o.tuitionAmount) }}</span>
        </el-form-item>
        <el-form-item :label="t('program.offerings.accommodation')">
          <el-input-number
            v-model="o.accommodationAmount"
            :min="0"
            :precision="2"
            :step="100"
            controls-position="right"
          />
          <el-select
            v-model="o.accommodationPeriod"
            class="w-32"
          >
            <el-option
              v-for="p in ACCOMMODATION_PERIODS"
              :key="p"
              :value="p"
              :label="t(`program.offerings.period.${p}`)"
            />
          </el-select>
          <span class="tuition-usd">{{ usdHint(o.accommodationAmount) }}</span>
        </el-form-item>
      </div>
      <el-input
        v-model="o.note"
        :placeholder="t('program.offerings.note')"
        maxlength="300"
      />
    </div>

    <el-button
      :icon="Plus"
      :disabled="isDegree && !form.levels.length"
      @click="addRow"
    >
      {{ t('program.offerings.add') }}
    </el-button>

    <el-alert
      v-for="(p, i) in problems"
      :key="i"
      class="offering__problem"
      type="warning"
      :closable="false"
      show-icon
      :title="problemText(p)"
    />
    <p class="fx-note">
      {{ t('program.tuitionFxNote') }}
    </p>
  </FormSection>
</template>

<style scoped src="../programForm.css" />
