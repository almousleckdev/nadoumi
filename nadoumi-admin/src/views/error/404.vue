<template>
  <div class="nad-page">
    <el-result
      icon="warning"
      :title="t('page.notFound')"
    >
      <template #extra>
        <el-button
          type="primary"
          @click="$router.push('/')"
        >
          {{ t('nav.home') }}
        </el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
const { t } = useI18n()
</script>
