<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { University } from '@/api/university'

const props = defineProps<{ highlights: University['highlights'] }>()

const { t } = useI18n()

function ofKind(kind: 'HIGHLIGHT' | 'ADVANTAGE') {
  return props.highlights.filter(h => h.kind === kind)
}
</script>

<template>
  <div class="nad-card sec">
    <h3 class="sec__title">
      {{ t('university.secHighlights') }}
    </h3>
    <div class="hl">
      <div v-if="ofKind('HIGHLIGHT').length">
        <p class="hl__cap">
          {{ t('university.kindHighlight') }}
        </p>
        <ul>
          <li
            v-for="h in ofKind('HIGHLIGHT')"
            :key="h.id"
          >
            {{ h.text }}
          </li>
        </ul>
      </div>
      <div v-if="ofKind('ADVANTAGE').length">
        <p class="hl__cap">
          {{ t('university.kindAdvantage') }}
        </p>
        <ul>
          <li
            v-for="h in ofKind('ADVANTAGE')"
            :key="h.id"
          >
            {{ h.text }}
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<style scoped src="./detail.css" />
<style scoped>
.hl {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
}
.hl__cap {
  margin: 0 0 4px;
  font-size: 12px;
  font-weight: 600;
  color: var(--nad-ink-faint);
}
.hl ul {
  margin: 0;
  padding-left: 18px;
  font-size: 14px;
  line-height: 1.7;
  color: var(--nad-ink);
}
</style>
