<template>
  <el-container class="layout">
    <el-aside width="220px" class="layout__aside">
      <div class="layout__brand">Nadoumi</div>
      <Sidebar />
    </el-aside>
    <el-container>
      <el-header class="layout__header">
        <Navbar />
      </el-header>
      <el-main class="layout__main">
        <router-view v-slot="{ Component }">
          <component :is="Component" />
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import Sidebar from './components/Sidebar.vue'
import Navbar from './components/Navbar.vue'
</script>

<style scoped>
.layout {
  height: 100%;
}
.layout__aside {
  background: var(--nad-sidebar-bg);
  color: var(--nad-sidebar-fg);
  overflow-y: auto;
}
.layout__brand {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  font-weight: 700;
  font-size: 18px;
  color: #fff;
}
.layout__header {
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
}
.layout__main {
  background: #f5f7fa;
}
</style>
