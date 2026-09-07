<template>
  <el-pagination
    v-if="total > 0"
    :current-page="page"
    :page-size="size"
    :page-sizes="PAGE_SIZES"
    :total="total"
    layout="total, sizes, prev, pager, next, jumper"
    background
    class="nad-pagination"
    @update:current-page="onPage"
    @update:page-size="onSize"
  />
</template>

<script setup lang="ts">
/** One pagination bar for every list screen. Page is 1-based here. */
const PAGE_SIZES = [10, 20, 50, 100, 150, 200]

defineProps<{ page: number, size: number, total: number }>()
const emit = defineEmits<{
  'update:page': [v: number]
  'update:size': [v: number]
  'change': []
}>()

function onPage(v: number) {
  emit('update:page', v)
  emit('change')
}
function onSize(v: number) {
  emit('update:size', v)
  emit('update:page', 1)
  emit('change')
}
</script>

<style scoped>
.nad-pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
