<template>
  <el-input
    :model-value="modelValue"
    :placeholder="placeholder"
    :prefix-icon="Search"
    clearable
    class="search"
    :style="{ width: `${width}px` }"
    @update:model-value="onInput"
    @keyup.enter="flush"
    @clear="flush"
  />
</template>

<script setup lang="ts">
import { onBeforeUnmount } from 'vue'
import { Search } from '@element-plus/icons-vue'

const props = withDefaults(defineProps<{
  modelValue: string
  placeholder?: string
  /** debounce before emitting `search`; 0 = only on Enter/clear */
  debounce?: number
  width?: number
}>(), { placeholder: 'Search…', debounce: 300, width: 260 })

const emit = defineEmits<{ 'update:modelValue': [v: string], 'search': [v: string] }>()

let timer: ReturnType<typeof setTimeout> | null = null

function onInput(v: string) {
  emit('update:modelValue', v)
  if (props.debounce <= 0) return
  if (timer) clearTimeout(timer)
  timer = setTimeout(() => emit('search', v), props.debounce)
}

function flush() {
  if (timer) clearTimeout(timer)
  emit('search', props.modelValue)
}

onBeforeUnmount(() => {
  if (timer) clearTimeout(timer)
})
</script>
