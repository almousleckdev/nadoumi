<template>
  <template v-if="!hidden">
    <!-- leaf -->
    <el-menu-item v-if="isLeaf" :index="resolvePath(single.path)">
      <el-icon v-if="iconOf(single)"><component :is="iconOf(single)" /></el-icon>
      <span>{{ titleOf(single) }}</span>
    </el-menu-item>

    <!-- group -->
    <el-sub-menu v-else :index="resolvePath(item.path)">
      <template #title>
        <el-icon v-if="iconOf(item)"><component :is="iconOf(item)" /></el-icon>
        <span>{{ titleOf(item) }}</span>
      </template>
      <SidebarItem
        v-for="child in visibleChildren"
        :key="child.path"
        :item="child"
        :base-path="resolvePath(item.path)"
      />
    </el-sub-menu>
  </template>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{ item: any; basePath: string }>()
const { t } = useI18n()

const hidden = computed(() => props.item?.meta?.hidden)
const children = computed<any[]>(() => props.item.children || [])
const visibleChildren = computed(() => children.value.filter((c) => !c.meta?.hidden))

const isLeaf = computed(() => visibleChildren.value.length === 0)
const single = computed(() =>
  visibleChildren.value.length === 1 && !props.item.alwaysShow ? visibleChildren.value[0] : props.item,
)

function titleOf(r: any) {
  const title = r?.meta?.title
  if (!title) return r?.path
  return r?.meta?.i18n ? t(title) : title
}
function iconOf(r: any) {
  return r?.meta?.icon
}
function resolvePath(p: string) {
  if (/^https?:\/\//.test(p)) return p
  if (p.startsWith('/')) return p
  return `${props.basePath.replace(/\/$/, '')}/${p}`
}
</script>
