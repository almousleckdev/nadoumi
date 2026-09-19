<template>
  <Drawer
    :model-value="modelValue"
    :title="t(isEdit ? 'revenue.edit' : 'revenue.new')"
    :saving="saving"
    :size="520"
    @update:model-value="(v: boolean) => emit('update:modelValue', v)"
    @save="save"
    @closed="reset"
  >
    <el-form
      v-if="modelValue"
      ref="formRef"
      v-loading="loading"
      :model="form"
      :rules="rules"
      label-position="top"
    >
      <el-form-item
        :label="t('revenue.revenueTitle')"
        prop="title"
      >
        <el-input
          v-model="form.title"
          maxlength="200"
        />
      </el-form-item>
      <el-form-item
        :label="t('revenue.source')"
        prop="source"
      >
        <el-select
          v-model="form.source"
          filterable
          allow-create
          default-first-option
          :placeholder="t('revenue.sourcePlaceholder')"
          style="width: 100%"
        >
          <el-option
            v-for="s in REVENUE_SOURCES"
            :key="s"
            :label="t(`revenue.sourceMap.${s}`)"
            :value="s"
          />
        </el-select>
      </el-form-item>
      <MoneyField
        v-model="form.amount"
        :label="t('revenue.amountRmb')"
        prop="amount"
        :note="t('revenue.fxNote')"
      />
      <el-form-item
        :label="t('revenue.receivedOn')"
        prop="receivedOn"
      >
        <el-date-picker
          v-model="form.receivedOn"
          type="date"
          value-format="YYYY-MM-DD"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item :label="t('revenue.reference')">
        <el-input
          v-model="form.reference"
          maxlength="120"
          :placeholder="t('revenue.referenceHint')"
        />
      </el-form-item>
      <el-form-item :label="t('revenue.description')">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="2"
        />
      </el-form-item>
      <el-form-item :label="t('revenue.notes')">
        <el-input
          v-model="form.notes"
          type="textarea"
          :rows="2"
        />
      </el-form-item>
    </el-form>
  </Drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import Drawer from '@/components/ui/Drawer.vue'
import MoneyField from '@/components/ui/MoneyField.vue'
import {
  getRevenue, createRevenue, updateRevenue, REVENUE_SOURCES, type RevenueInput,
} from '@/api/finance'
import { useDrawerForm } from '@/composables/useDrawerForm'
import { todayIso } from '@/utils/date'

const props = defineProps<{ modelValue: boolean, revenueId?: number }>()
const emit = defineEmits<{ 'update:modelValue': [v: boolean], 'saved': [] }>()
const { t } = useI18n()

const isEdit = computed(() => props.revenueId != null)

const blank = (): RevenueInput => ({
  source: 'SERVICE_FEE', title: '', description: '', amount: 0, currency: 'CNY',
  receivedOn: todayIso(), reference: '', notes: '',
})
const rules = {
  source: [{ required: true, trigger: 'change', message: t('common.required') }],
  title: [{ required: true, trigger: 'blur', message: t('common.required') }],
  amount: [{ required: true, trigger: 'blur', message: t('common.required') }],
  receivedOn: [{ required: true, trigger: 'change', message: t('common.required') }],
}

const { formRef, form, loading, saving, save, reset } = useDrawerForm<RevenueInput>({
  isOpen: () => props.modelValue,
  blank,
  load: async (form) => {
    if (!isEdit.value) return
    const r = await getRevenue(props.revenueId!)
    Object.assign(form, {
      source: r.source, title: r.title, description: r.description ?? '',
      amount: r.amount, currency: r.currency || 'CNY', receivedOn: r.receivedOn,
      reference: r.reference ?? '', notes: r.notes ?? '',
    })
  },
  submit: async (form) => {
    const body: RevenueInput = { ...form, currency: 'CNY' }
    if (isEdit.value) await updateRevenue(props.revenueId!, body)
    else await createRevenue(body)
  },
  onSaved: () => emit('saved'),
})
</script>
