<template>
  <el-select
    :model-value="selected"
    filterable
    remote
    allow-create
    default-first-option
    clearable
    reserve-keyword
    :remote-method="search"
    :loading="loading"
    :placeholder="t('program.majorPick')"
    @change="pick"
    @visible-change="(open: boolean) => open && !options.length && search('')"
  >
    <el-option
      v-for="m in options"
      :key="m.id"
      :value="m.id"
      :label="majorLabel(m)"
    />
  </el-select>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { listMajors, type Major } from '@/api/major'
import { majorLabel } from '../majorLabel'

/** One programme major row: pick a catalogue major, or type a name that will be added to the catalogue on save. */
interface MajorRow { majorId?: number | null, name: string, nameCn: string | null }

const SEARCH_LIMIT = 20

const props = defineProps<{ row: MajorRow }>()
const { t } = useI18n()

const loading = ref(false)
const found = ref<Major[]>([])

// the row's own major stays listed so its label shows before any search has run
const options = computed<Major[]>(() => {
  const own = props.row.majorId != null && !found.value.some(m => m.id === props.row.majorId)
    ? [{ id: props.row.majorId, name: props.row.name, nameCn: props.row.nameCn, discipline: null, programCount: 0 }]
    : []
  return [...own, ...found.value]
})

const selected = computed(() => props.row.majorId ?? (props.row.name || undefined))

async function search(query: string) {
  loading.value = true
  try {
    found.value = (await listMajors({ q: query || undefined, size: SEARCH_LIMIT })).content
  }
  finally {
    loading.value = false
  }
}

function pick(value: number | string | undefined) {
  const row = props.row
  if (typeof value === 'number') {
    const major = options.value.find(m => m.id === value)
    row.majorId = value
    row.name = major?.name ?? row.name
    row.nameCn = major?.nameCn ?? null
  }
  else {
    row.majorId = null
    row.name = (value ?? '').trim()
    row.nameCn = null
  }
}
</script>
