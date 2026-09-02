<template>
  <el-menu
    :default-active="activePath"
    background-color="var(--nad-sidebar-bg)"
    text-color="var(--nad-sidebar-fg)"
    active-text-color="#fff"
    router
    unique-opened
  >
    <SidebarItem v-for="route in menu" :key="route.path" :item="route" base-path="/" />
  </el-menu>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { usePermissionStore } from '@/stores/permission'
import SidebarItem from './SidebarItem.vue'

const route = useRoute()
const activePath = computed(() => route.path)

const { menuRoutes } = storeToRefs(usePermissionStore())

// include the static dashboard entry first
const menu = computed(() => [
  { path: '/dashboard', meta: { title: 'nav.dashboard', icon: 'HomeFilled', i18n: true } },
  ...menuRoutes.value,
])
</script>
