<template>
  <el-config-provider :locale="en">
    <router-view />
  </el-config-provider>
</template>

<script setup lang="ts">
import en from 'element-plus/es/locale/lang/en'
</script>
