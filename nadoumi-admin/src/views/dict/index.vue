<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import PageHeader from '@/components/PageHeader.vue'
import type { SysDictType } from '@/api/system'
import DictTypesPanel from './DictTypesPanel.vue'
import DictDataPanel from './DictDataPanel.vue'

const { t } = useI18n()

const selected = ref<SysDictType | null>(null)
</script>

<template>
  <div class="nad-page">
    <PageHeader
      :title="t('dict.title')"
      :subtitle="t('dict.subtitle')"
    />

    <div class="dict__split">
      <DictTypesPanel
        :selected-id="selected?.dictId ?? null"
        @select="selected = $event"
      />
      <DictDataPanel :type="selected" />
    </div>
  </div>
</template>

<style scoped>
.dict__split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}
@media (max-width: 1000px) {
  .dict__split {
    grid-template-columns: 1fr;
  }
}
</style>
