<template>
  <Drawer
    :model-value="modelValue"
    :title="university ? t('university.edit') : t('university.new')"
    :saving="saving"
    @update:model-value="v => emit('update:modelValue', v)"
    @save="save"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
    >
      <el-form-item
        :label="t('university.name')"
        prop="name"
      >
        <el-input v-model="form.name" />
      </el-form-item>
      <div class="row2">
        <el-form-item
          :label="t('university.country')"
          prop="country"
        >
          <el-input
            v-model="form.country"
            maxlength="2"
            placeholder="ISO alpha-2 (e.g. CN)"
          />
        </el-form-item>
        <el-form-item :label="t('university.city')">
          <el-input v-model="form.city" />
        </el-form-item>
      </div>
      <el-form-item :label="t('university.website')">
        <el-input
          v-model="form.website"
          placeholder="https://…"
        />
      </el-form-item>
      <div class="row2">
        <el-form-item :label="t('university.rankingTier')">
          <el-input
            v-model="form.rankingTier"
            placeholder="e.g. Top 100"
          />
        </el-form-item>
        <el-form-item
          :label="t('university.status')"
          prop="status"
        >
          <el-select
            v-model="form.status"
            style="width: 100%"
          >
            <el-option
              v-for="s in STATUSES"
              :key="s"
              :label="titleCase(s)"
              :value="s"
            />
          </el-select>
        </el-form-item>
      </div>
      <el-form-item :label="t('university.remark')">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="2"
        />
      </el-form-item>
    </el-form>
  </Drawer>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import {
  createUniversity, updateUniversity,
  type University, type UniversityInput, type UniversityStatus,
} from '@/api/university'
import Drawer from '@/components/ui/Drawer.vue'

const props = defineProps<{ modelValue: boolean, university: University | null }>()
const emit = defineEmits<{ 'update:modelValue': [v: boolean], 'saved': [u: University] }>()

const { t } = useI18n()
const STATUSES: UniversityStatus[] = ['ACTIVE', 'INACTIVE']

function titleCase(s: string) {
  return s.charAt(0) + s.slice(1).toLowerCase()
}

const formRef = ref<FormInstance>()
const saving = ref(false)
const blank = { name: '', country: '', city: '', website: '', rankingTier: '', status: 'ACTIVE' as UniversityStatus, remark: '' }
const form = reactive({ ...blank })
const rules = {
  name: [{ required: true, trigger: 'blur', message: t('university.required') }],
  country: [
    { required: true, trigger: 'blur', message: t('university.required') },
    { pattern: /^[A-Za-z]{2}$/, trigger: 'blur', message: t('university.countryFormat') },
  ],
  status: [{ required: true, message: t('university.required') }],
}

watch(() => props.modelValue, (open) => {
  if (!open) return
  const u = props.university
  Object.assign(form, u
    ? {
        name: u.name, country: u.country, city: u.city ?? '', website: u.website ?? '',
        rankingTier: u.rankingTier ?? '', status: u.status, remark: '',
      }
    : blank)
}, { immediate: true })

function payload(): UniversityInput {
  return {
    name: form.name.trim(),
    country: form.country.trim().toUpperCase(),
    city: form.city || null,
    website: form.website || null,
    rankingTier: form.rankingTier || null,
    status: form.status,
    remark: form.remark || null,
  }
}

async function save() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const saved = props.university
      ? await updateUniversity(props.university.id, payload())
      : await createUniversity(payload())
    ElMessage.success(t('common.saved'))
    emit('update:modelValue', false)
    emit('saved', saved)
  }
  finally {
    saving.value = false
  }
}
</script>

<style scoped>
.row2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
</style>
