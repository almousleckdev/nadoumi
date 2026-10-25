<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'

export interface CatalogRow {
  key: string
  label: string
  count: number | null
  to: string
}

defineProps<{ rows: CatalogRow[] }>()

const { t } = useI18n()
const router = useRouter()
</script>

<template>
  <el-card
    shadow="never"
    class="cat"
  >
    <template #header>
      <span class="cat__title">{{ t('dashboard.catalogTitle') }}</span>
    </template>
    <ul class="cat__list">
      <li
        v-for="r in rows"
        :key="r.key"
        class="cat__row"
        role="link"
        tabindex="0"
        @click="router.push(r.to)"
        @keyup.enter="router.push(r.to)"
      >
        <span>{{ r.label }}</span>
        <strong class="cat__count">{{ r.count ?? '–' }}</strong>
      </li>
    </ul>
  </el-card>
</template>

<style scoped>
.cat { border: 1px solid var(--nad-line); border-radius: var(--nad-radius); }
.cat__title { font-weight: 650; font-size: 14px; color: var(--nad-ink); }
.cat__list { list-style: none; margin: 0; padding: 0; }
.cat__row { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid var(--nad-line, #e5e7eb); font-size: 13px; cursor: pointer; }
.cat__row:last-child { border-bottom: 0; }
.cat__row:hover { color: var(--el-color-primary); }
.cat__count { font-variant-numeric: tabular-nums; }
</style>
