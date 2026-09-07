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
      <el-form-item
        :label="t('revenue.amountRmb')"
        prop="amount"
      >
        <el-input-number
          v-model="form.amount"
          :min="0"
          :precision="2"
          :step="100"
          controls-position="right"
          style="width: 100%"
        />
        <span class="usd-hint">{{ usdPreview }}</span>
      </el-form-item>
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
      <p class="fx-note">
        {{ t('revenue.fxNote') }}
      </p>
    </el-form>
  </Drawer>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import Drawer from '@/components/ui/Drawer.vue'
import {
  getRevenue, createRevenue, updateRevenue, REVENUE_SOURCES, type RevenueInput,
} from '@/api/finance'
import { cnyToUsdRate } from '@/api/fx'
import { usd } from '@/utils/money'

const props = defineProps<{ modelValue: boolean, revenueId?: number }>()
const emit = defineEmits<{ 'update:modelValue': [v: boolean], 'saved': [] }>()
const { t } = useI18n()

const isEdit = computed(() => props.revenueId != null)
const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)
const fxRate = ref(0.1381)

const blank = (): RevenueInput => ({
  source: 'SERVICE_FEE', title: '', description: '', amount: 0, currency: 'CNY',
  receivedOn: new Date().toISOString().slice(0, 10), reference: '', notes: '',
})
const form = reactive<RevenueInput>(blank())
const rules = {
  title: [{ required: true, trigger: 'blur', message: t('common.required') }],
  source: [{ required: true, trigger: 'change', message: t('common.required') }],
  amount: [{ required: true, trigger: 'blur', message: t('common.required') }],
  receivedOn: [{ required: true, trigger: 'change', message: t('common.required') }],
}
const usdPreview = computed(() => form.amount ? `≈ ${usd(form.amount * fxRate.value)}` : '')

async function open() {
  Object.assign(form, blank())
  loading.value = true
  try {
    fxRate.value = await cnyToUsdRate()
    if (isEdit.value) {
      const r = await getRevenue(props.revenueId!)
      Object.assign(form, {
        source: r.source, title: r.title, description: r.description ?? '',
        amount: r.amount, currency: r.currency || 'CNY', receivedOn: r.receivedOn,
        reference: r.reference ?? '', notes: r.notes ?? '',
      })
    }
  }
  finally {
    loading.value = false
  }
}

async function save() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const body: RevenueInput = { ...form, currency: 'CNY' }
    if (isEdit.value) await updateRevenue(props.revenueId!, body)
    else await createRevenue(body)
    ElMessage.success(t('common.saved'))
    emit('saved')
  }
  finally {
    saving.value = false
  }
}

function reset() { Object.assign(form, blank()) }
watch(() => props.modelValue, (o) => { if (o) open() })
</script>

<style scoped>
.usd-hint { display: block; margin-top: 4px; font-size: 12px; color: var(--nad-ink-soft, #64748b); font-variant-numeric: tabular-nums; }
.fx-note { margin: 4px 0 0; font-size: 12px; color: var(--nad-ink-faint, #9ca3af); }
</style>
