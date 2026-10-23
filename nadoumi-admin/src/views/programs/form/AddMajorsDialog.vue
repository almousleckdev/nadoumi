<template>
  <el-dialog
    :model-value="modelValue"
    :title="t('program.addSeveralTitle')"
    width="560px"
    @update:model-value="(v: boolean) => emit('update:modelValue', v)"
  >
    <p class="hint">
      {{ t('program.addSeveralHint') }}
    </p>
    <el-select
      v-model="chosen"
      multiple
      filterable
      remote
      allow-create
      default-first-option
      reserve-keyword
      :remote-method="search"
      :loading="loading"
      :placeholder="t('program.majorPick')"
      style="width: 100%"
      @visible-change="(open: boolean) => open && !found.length && search('')"
    >
      <el-option
        v-for="m in found"
        :key="m.id"
        :value="m.id"
        :label="majorLabel(m)"
      />
    </el-select>

    <el-input
      v-model="pasted"
      type="textarea"
      :rows="5"
      :placeholder="t('program.addSeveralPaste')"
      class="paste"
    />

    <div class="where">
      <el-checkbox-group
        v-model="levels"
        class="where__levels"
      >
        <el-checkbox
          v-for="lv in programLevels"
          :key="lv"
          :value="lv"
        >
          {{ t(`program.levelMap.${lv}`) }}
        </el-checkbox>
      </el-checkbox-group>
      <el-select
        v-model="departmentId"
        clearable
        filterable
        :placeholder="t('program.majorDepartment')"
        class="where__department"
      >
        <el-option
          v-for="d in departments"
          :key="d.id"
          :value="d.id"
          :label="d.name"
        />
      </el-select>
    </div>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">
        {{ t('common.cancel') }}
      </el-button>
      <el-button
        type="primary"
        :disabled="!hasAnything"
        @click="add"
      >
        {{ t('program.addSeveralAdd') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { listMajors, type Major } from '@/api/major'
import type { DegreeLevel } from '@/api/program'
import type { Department } from '@/api/university'
import { majorLabel } from '../majorLabel'
import { parseMajorLines } from '../parseMajorLines'

export interface NewMajorRow {
  majorId: number | null
  name: string
  nameCn: string | null
  departmentId: number | null
  level: DegreeLevel | null
}

const SEARCH_LIMIT = 30

const props = defineProps<{
  modelValue: boolean
  programLevels: DegreeLevel[]
  departments: Department[]
}>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean], add: [rows: NewMajorRow[]] }>()

const { t } = useI18n()
const loading = ref(false)
const found = ref<Major[]>([])
const chosen = ref<(number | string)[]>([])
const pasted = ref('')
const levels = ref<DegreeLevel[]>([])
const departmentId = ref<number | null>(null)

const hasAnything = computed(() => chosen.value.length > 0 || pasted.value.trim().length > 0)

watch(() => props.modelValue, (open) => {
  if (!open) return
  chosen.value = []
  pasted.value = ''
  departmentId.value = null
  levels.value = [...props.programLevels]
})

async function search(query: string) {
  loading.value = true
  try {
    found.value = (await listMajors({ q: query || undefined, size: SEARCH_LIMIT })).content
  }
  finally {
    loading.value = false
  }
}

/** Picked majors keep their catalogue id; typed or pasted names are matched or added on the server when saved. */
function add() {
  const picked = chosen.value.map((value) => {
    const major = typeof value === 'number' ? found.value.find(m => m.id === value) : undefined
    return major
      ? { majorId: major.id, name: major.name, nameCn: major.nameCn }
      : { majorId: null, name: String(value).trim(), nameCn: null }
  })
  const typed = parseMajorLines(pasted.value).map(l => ({ majorId: null, name: l.name, nameCn: l.nameCn }))
  const targetLevels: (DegreeLevel | null)[] = levels.value.length ? levels.value : [null]
  const rows = [...picked, ...typed].flatMap(major =>
    targetLevels.map(level => ({ ...major, departmentId: departmentId.value, level })))
  emit('add', rows)
  emit('update:modelValue', false)
}
</script>

<style scoped>
.hint { margin: 0 0 10px; color: var(--el-text-color-secondary); font-size: 13px; }
.paste { margin-top: 12px; }
.where { margin-top: 12px; display: flex; gap: 16px; align-items: center; flex-wrap: wrap; }
.where__department { width: 220px; }
</style>
