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
      <MoneyField
        v-model="form.amount"
        :label="t('expenses.amountRmb')"
        prop="amount"
        :note="t('expenses.fxNote')"
      />
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
    </el-form>
  </Drawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import Drawer from '@/components/ui/Drawer.vue'
import MoneyField from '@/components/ui/MoneyField.vue'
import {
  getExpense, createExpense, updateExpense, listExpenseCategories,
  PAYMENT_METHODS, type ExpenseInput, type ExpenseCategory,
} from '@/api/finance'
import { useDrawerForm } from '@/composables/useDrawerForm'
import { todayIso } from '@/utils/date'

const props = defineProps<{ modelValue: boolean, expenseId?: number }>()
const emit = defineEmits<{ 'update:modelValue': [v: boolean], 'saved': [] }>()
const { t } = useI18n()

const isEdit = computed(() => props.expenseId != null)
const categories = ref<ExpenseCategory[]>([])
// number = an existing category id; string = a freshly typed category name
const categoryChoice = ref<number | string | null>(null)

const blank = (): ExpenseInput => ({
  categoryId: null, categoryName: null, title: '', description: '', amount: 0, currency: 'CNY',
  spentOn: todayIso(), vendor: '', paymentMethod: null, notes: '',
})
const rules = {
  title: [{ required: true, trigger: 'blur', message: t('common.required') }],
  amount: [{ required: true, trigger: 'blur', message: t('common.required') }],
  spentOn: [{ required: true, trigger: 'change', message: t('common.required') }],
}

const { formRef, form, loading, saving, save, reset } = useDrawerForm<ExpenseInput>({
  isOpen: () => props.modelValue,
  blank,
  load: async (form) => {
    categories.value = await listExpenseCategories()
    if (!isEdit.value) return
    const e = await getExpense(props.expenseId!)
    Object.assign(form, {
      categoryId: e.categoryId, title: e.title, description: e.description ?? '',
      amount: e.amount, currency: e.currency || 'CNY', spentOn: e.spentOn,
      vendor: e.vendor ?? '', paymentMethod: e.paymentMethod, notes: e.notes ?? '',
    })
    categoryChoice.value = e.categoryId ?? (e.categoryName ?? null)
  },
  submit: async (form) => {
    const choice = categoryChoice.value
    const body: ExpenseInput = {
      ...form,
      currency: 'CNY',
      categoryId: typeof choice === 'number' ? choice : null,
      categoryName: typeof choice === 'string' && choice.trim() ? choice.trim() : null,
    }
    if (isEdit.value) await updateExpense(props.expenseId!, body)
    else await createExpense(body)
  },
  onSaved: () => emit('saved'),
  onReset: () => { categoryChoice.value = null },
})
</script>
