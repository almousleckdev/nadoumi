<template>
  <div class="filterbar">
    <div class="filterbar__filters">
      <slot />
    </div>
    <div class="filterbar__actions">
      <el-button
        v-if="dirty"
        text
        :icon="RefreshLeft"
        @click="emit('clear')"
      >
        {{ t('filter.clear') }}
      </el-button>
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { RefreshLeft } from '@element-plus/icons-vue'

withDefaults(defineProps<{ dirty?: boolean }>(), { dirty: false })
const emit = defineEmits<{ clear: [] }>()
const { t } = useI18n()
</script>

<style scoped>
.filterbar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.filterbar__filters {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.filterbar__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}
</style>
