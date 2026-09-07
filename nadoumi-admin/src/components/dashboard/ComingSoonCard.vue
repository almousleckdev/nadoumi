<template>
  <el-card
    class="cs-card"
    :body-style="{ padding: '18px 20px' }"
    shadow="never"
  >
    <div class="cs-card__row">
      <span class="cs-card__label">{{ label }}</span>
      <el-tag
        size="small"
        type="info"
        effect="plain"
        disable-transitions
      >
        {{ t('dashboard.comingSoon') }}
      </el-tag>
    </div>
    <div class="cs-card__value">
      N/A
    </div>
    <div class="cs-card__note">
      {{ note || t('dashboard.comingSoonNote', { domain }) }}
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

withDefaults(defineProps<{
  label: string
  /** the bounded context that will provide this metric (docs/ADMIN_ARCHITECTURE.md §7) */
  domain: string
  note?: string
}>(), { note: undefined })

const { t } = useI18n()
</script>

<style scoped>
.cs-card {
  border: 1px dashed var(--el-border-color);
  border-radius: 10px;
  background: var(--el-fill-color-blank);
}
.cs-card__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.cs-card__label {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  font-weight: 500;
}
.cs-card__value {
  margin-top: 6px;
  font-size: 26px;
  font-weight: 700;
  color: var(--el-text-color-placeholder);
}
.cs-card__note {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
</style>
