<template>
  <div class="nad-page">
    <el-empty :description="t('page.notBuilt')">
      <template #image>
        <el-icon :size="56" color="#c0c4cc"><Tools /></el-icon>
      </template>
      <div style="color: #909399; font-size: 13px">{{ route.path }}</div>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'

const { t } = useI18n()
const route = useRoute()
</script>
