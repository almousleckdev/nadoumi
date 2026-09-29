<script setup lang="ts">
import { useScholarshipFinder } from '~/composables/useScholarshipFinder'

const emit = defineEmits<{ navigate: [] }>()

const { t } = useI18n()
const localePath = useLocalePath()
const finder = useScholarshipFinder()

const allResultsLink = computed(() => localePath(`/scholarships${finder.resultsQuery.value}`))
</script>

<template>
<form class="ai-form" @submit.prevent="finder.search">
  <input
    v-model="finder.query.value"
    type="search"
    :placeholder="t('assistant.findPlaceholder')"
    class="ai-input"
  >
  <select v-model="finder.funding.value" class="ai-input" :aria-label="t('assistant.anyFunding')">
    <option value="">{{ t('assistant.anyFunding') }}</option>
    <option value="FULLY">{{ t('assistant.fundingFully') }}</option>
    <option value="PARTIAL">{{ t('assistant.fundingPartial') }}</option>
    <option value="SELF">{{ t('assistant.fundingSelf') }}</option>
  </select>
  <button type="submit" class="ai-btn" :disabled="finder.finding.value">
    {{ finder.finding.value ? t('assistant.searching') : t('assistant.search') }}
  </button>

  <div v-if="finder.searched.value && !finder.finding.value" class="ai-results">
    <p v-if="finder.failed.value" class="ai-note ai-note--warn">{{ t('errors.loadSection') }}</p>
    <p v-else-if="!finder.results.value.length" class="ai-note">{{ t('assistant.noResults') }}</p>
    <template v-else>
      <p class="ai-results__title">{{ t('assistant.resultsTitle') }}</p>
      <NuxtLink
        v-for="s in finder.results.value"
        :key="s.id"
        :to="localePath(`/scholarships/${s.slug}`)"
        class="ai-result"
        @click="emit('navigate')"
      >
        <span class="ai-result__title">{{ s.title }}</span>
        <span class="ai-result__meta">
          {{ s.country }} · {{ t(`scholarships.funding.${s.fundingModel}`) }}
          <template v-if="s.deadline"> · {{ t('assistant.deadline') }} {{ s.deadline }}</template>
        </span>
      </NuxtLink>
      <NuxtLink :to="allResultsLink" class="ai-link" @click="emit('navigate')">{{ t('assistant.viewAll') }} →</NuxtLink>
    </template>
  </div>
  <p v-else-if="!finder.searched.value" class="ai-note">{{ t('assistant.findHint') }}</p>
</form>
</template>

<style scoped src="./assistant-form.css" />
