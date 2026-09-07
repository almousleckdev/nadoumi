<template>
  <Drawer
    :model-value="modelValue"
    :title="t(isEdit ? 'expenses.edit' : 'expenses.new')"
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
        :label="t('expenses.expenseTitle')"
        prop="title"
      >
        <el-input
          v-model="form.title"
          maxlength="200"
        />
      </el-form-item>
      <el-form-item :label="t('expenses.category')">
        <el-select
          v-model="categoryChoice"
          filterable
          allow-create
          default-first-option
          clearable
          :placeholder="t('expenses.categoryPlaceholder')"
          style="width: 100%"
        >
          <el-option
            v-for="c in categories"
            :key="c.id"
            :label="c.name"
            :value="c.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item
        :label="t('expenses.amountRmb')"
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
        :label="t('expenses.spentOn')"
        prop="spentOn"
      >
        <el-date-picker
          v-model="form.spentOn"
          type="date"
          value-format="YYYY-MM-DD"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item :label="t('expenses.vendor')">
        <el-input
          v-model="form.vendor"
          maxlength="200"
        />
      </el-form-item>
      <el-form-item :label="t('expenses.paymentMethod')">
        <el-select
          v-model="form.paymentMethod"
          clearable
          style="width: 100%"
        >
          <el-option
            v-for="m in PAYMENT_METHODS"
            :key="m"
            :label="t(`expenses.methodMap.${m}`)"
            :value="m"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('expenses.description')">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="2"
        />
      </el-form-item>
      <el-form-item :label="t('expenses.notes')">
        <el-input
          v-model="form.notes"
          type="textarea"
          :rows="2"
        />
      </el-form-item>
      <p class="fx-note">
        {{ t('expenses.fxNote') }}
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
  getExpense, createExpense, updateExpense, listExpenseCategories,
  PAYMENT_METHODS, type ExpenseInput, type ExpenseCategory,
} from '@/api/finance'
import { cnyToUsdRate } from '@/api/fx'
import { usd } from '@/utils/money'

const props = defineProps<{ modelValue: boolean, expenseId?: number }>()
const emit = defineEmits<{ 'update:modelValue': [v: boolean], 'saved': [] }>()
const { t } = useI18n()

const isEdit = computed(() => props.expenseId != null)
const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)
const categories = ref<ExpenseCategory[]>([])
const fxRate = ref(0.1381)
// number = an existing category id; string = a freshly typed category name
const categoryChoice = ref<number | string | null>(null)

const blank = (): ExpenseInput => ({
  categoryId: null, categoryName: null, title: '', description: '', amount: 0, currency: 'CNY',
  spentOn: new Date().toISOString().slice(0, 10), vendor: '', paymentMethod: null, notes: '',
})
const form = reactive<ExpenseInput>(blank())
const rules = {
  title: [{ required: true, trigger: 'blur', message: t('common.required') }],
  amount: [{ required: true, trigger: 'blur', message: t('common.required') }],
  spentOn: [{ required: true, trigger: 'change', message: t('common.required') }],
}
const usdPreview = computed(() => form.amount ? `≈ ${usd(form.amount * fxRate.value)}` : '')

async function open() {
  Object.assign(form, blank())
  categoryChoice.value = null
  loading.value = true
  try {
    const [cats, rate] = await Promise.all([listExpenseCategories(), cnyToUsdRate()])
    categories.value = cats
    fxRate.value = rate
    if (isEdit.value) {
      const e = await getExpense(props.expenseId!)
      Object.assign(form, {
        categoryId: e.categoryId, title: e.title, description: e.description ?? '',
        amount: e.amount, currency: e.currency || 'CNY', spentOn: e.spentOn,
        vendor: e.vendor ?? '', paymentMethod: e.paymentMethod, notes: e.notes ?? '',
      })
      categoryChoice.value = e.categoryId ?? (e.categoryName ?? null)
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
    const choice = categoryChoice.value
    const body: ExpenseInput = {
      ...form,
      currency: 'CNY',
      categoryId: typeof choice === 'number' ? choice : null,
      categoryName: typeof choice === 'string' && choice.trim() ? choice.trim() : null,
    }
    if (isEdit.value) await updateExpense(props.expenseId!, body)
    else await createExpense(body)
    ElMessage.success(t('common.saved'))
    emit('saved')
  }
  finally {
    saving.value = false
  }
}

function reset() {
  Object.assign(form, blank())
  categoryChoice.value = null
}
watch(() => props.modelValue, (o) => { if (o) open() })
</script>

<style scoped>
.usd-hint { display: block; margin-top: 4px; font-size: 12px; color: var(--nad-ink-soft, #64748b); font-variant-numeric: tabular-nums; }
.fx-note { margin: 4px 0 0; font-size: 12px; color: var(--nad-ink-faint, #9ca3af); }
</style>
