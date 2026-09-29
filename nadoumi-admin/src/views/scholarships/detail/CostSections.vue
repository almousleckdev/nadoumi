<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ScholarshipView } from '@/api/scholarship'
import FormSection from '@/components/ui/FormSection.vue'

const props = defineProps<{ view: ScholarshipView }>()

const { t } = useI18n()

function dual(rmb: number | null | undefined, usd: number | null | undefined): string {
  if (rmb == null && usd == null) return ''
  return `¥${(rmb ?? 0).toLocaleString('en')} · $${(usd ?? 0).toLocaleString('en')}`
}

const feeRows = computed(() => {
  const v = props.view
  const rows = v.fees.map(f => ({ label: t(`scholarship.fee.${f.kind}`), amount: dual(f.amountRmb, f.amountUsd) }))
  if (v.applicationFee) rows.unshift({ label: t('scholarship.fee.APPLICATION'), amount: dual(v.applicationFee.amountRmb, v.applicationFee.amountUsd) })
  if (v.serviceFee) rows.push({ label: t('scholarship.fee.NADOUMI_SERVICE'), amount: dual(v.serviceFee.amountRmb, v.serviceFee.amountUsd) })
  return rows
})
</script>

<template>
  <FormSection
    v-if="view.fees.length || view.applicationFee || view.serviceFee"
    :title="t('scholarship.secFees')"
  >
    <el-table
      :data="feeRows"
      size="small"
    >
      <el-table-column
        prop="label"
        :label="t('scholarship.feeKind')"
      />
      <el-table-column
        prop="amount"
        :label="t('scholarship.amount')"
        align="right"
      />
    </el-table>
  </FormSection>

  <FormSection
    v-if="view.stipends.length"
    :title="t('scholarship.secStipend')"
  >
    <el-table
      :data="view.stipends"
      size="small"
    >
      <el-table-column
        :label="t('scholarship.level')"
        width="120"
      >
        <template #default="{ row }">
          {{ t(`scholarship.level.${row.level}`) }}
        </template>
      </el-table-column>
      <el-table-column
        :label="t('scholarship.amount')"
        align="right"
      >
        <template #default="{ row }">
          {{ dual(row.amountRmb, row.amountUsd) }} / {{ t(`scholarship.freq.${row.frequency}`) }}
        </template>
      </el-table-column>
      <el-table-column
        :label="t('scholarship.durationMonths')"
        width="120"
        align="right"
      >
        <template #default="{ row }">
          {{ row.durationMonths ?? '' }}
        </template>
      </el-table-column>
      <el-table-column
        :label="t('scholarship.stipendConditions')"
        prop="conditions"
      />
    </el-table>
  </FormSection>

  <FormSection
    v-if="view.accommodation.length"
    :title="t('scholarship.secAccommodation')"
  >
    <el-table
      :data="view.accommodation"
      size="small"
    >
      <el-table-column
        :label="t('scholarship.accommodationNote')"
        prop="note"
      >
        <template #default="{ row }">
          <strong>{{ t(`scholarship.room.${row.roomType}`) }}</strong>
          <span
            v-if="row.note"
            class="muted"
          > · {{ row.note }}</span>
        </template>
      </el-table-column>
      <el-table-column
        :label="t('scholarship.amount')"
        align="right"
        width="200"
      >
        <template #default="{ row }">
          {{ dual(row.amountRmb, row.amountUsd) }}
        </template>
      </el-table-column>
    </el-table>
  </FormSection>
</template>

<style scoped src="./detail.css" />
