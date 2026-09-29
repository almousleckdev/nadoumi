<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { University } from '@/api/university'

defineProps<{ rankings: University['rankings'] }>()

const { t } = useI18n()
</script>

<template>
  <div class="nad-card sec">
    <h3 class="sec__title">
      {{ t('university.secRankings') }}
    </h3>
    <el-table :data="rankings">
      <el-table-column
        :label="t('university.rankSource')"
        prop="source"
        width="140"
      />
      <el-table-column
        :label="t('university.rankPosition')"
        width="120"
      >
        <template #default="{ row }">
          #{{ row.rankPosition }}
        </template>
      </el-table-column>
      <el-table-column
        :label="t('university.rankYear')"
        prop="rankYear"
        width="100"
      />
      <el-table-column
        :label="t('university.rankNote')"
        prop="note"
      />
    </el-table>
  </div>
</template>

<style scoped src="./detail.css" />
