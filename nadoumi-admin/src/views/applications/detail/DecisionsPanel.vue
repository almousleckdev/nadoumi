<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { ApplicationDetail } from '@/api/application'
import { formatDateTime } from '@/utils/date'

defineProps<{ detail: ApplicationDetail }>()
const { t } = useI18n()

const words = (code: string) => {
  const text = code.toLowerCase().replace(/_/g, ' ')
  return text.charAt(0).toUpperCase() + text.slice(1)
}
</script>

<template>
  <div>
    <p class="dc-help">
      {{ t('applications.decisionsHelp') }}
    </p>
    <p
      v-if="!detail.decisions.length"
      class="dc-help"
    >
      {{ t('applications.noDecisions') }}
    </p>
    <ul
      v-else
      class="dc-list"
    >
      <li
        v-for="d in detail.decisions"
        :key="d.id"
      >
        <b>{{ words(d.decisionType) }}: {{ words(d.outcome) }}</b>
        <div class="dc-sub">
          {{ d.rationale }}
        </div>
        <div class="dc-sub">
          {{ d.decidedByName ?? t('applications.system') }} · {{ formatDateTime(d.decidedAt) }}
        </div>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.dc-help { margin: 0 0 12px; font-size: 13px; color: var(--nad-ink-soft, #64748b); }
.dc-list { list-style: none; margin: 0; padding: 0; display: grid; gap: 12px; }
.dc-sub { font-size: 12px; color: var(--nad-ink-soft, #64748b); margin-top: 2px; }
</style>
