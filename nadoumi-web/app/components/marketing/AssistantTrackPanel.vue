<script setup lang="ts">
import { useApplicationTracker } from '~/composables/useApplicationTracker'

const emit = defineEmits<{ navigate: [] }>()

const { t } = useI18n()
const localePath = useLocalePath()
const tracker = useApplicationTracker()
</script>

<template>
<form class="ai-form" @submit.prevent="tracker.track">
  <input
    v-model="tracker.appId.value"
    type="text"
    :placeholder="t('assistant.trackPlaceholder')"
    class="ai-input"
    autocomplete="off"
  >
  <button type="submit" class="ai-btn" :disabled="tracker.tracking.value || !tracker.appId.value.trim()">
    {{ tracker.tracking.value ? t('assistant.trackChecking') : t('assistant.trackCta') }}
  </button>

  <div v-if="tracker.state.value === 'ok' && tracker.application.value" class="ai-track">
    <p class="ai-result__title">{{ tracker.application.value.opportunityTitle || `#${tracker.application.value.id}` }}</p>
    <dl class="ai-track__grid">
      <div><dt>{{ t('assistant.trackStatus') }}</dt><dd>{{ tracker.application.value.status }}</dd></div>
      <div v-if="tracker.application.value.stage"><dt>{{ t('assistant.trackStage') }}</dt><dd>{{ tracker.application.value.stage }}</dd></div>
      <div v-if="tracker.application.value.updatedAt"><dt>{{ t('assistant.trackUpdated') }}</dt><dd>{{ tracker.application.value.updatedAt }}</dd></div>
    </dl>
  </div>
  <p v-else-if="tracker.state.value === 'notfound'" class="ai-note ai-note--warn">{{ t('assistant.trackNotFound') }}</p>
  <template v-else-if="tracker.state.value === 'unavailable'">
    <p class="ai-note">{{ t('assistant.trackComingSoon') }}</p>
    <NuxtLink :to="localePath('/dashboard')" class="ai-link" @click="emit('navigate')">{{ t('assistant.trackGoDashboard') }} →</NuxtLink>
  </template>
  <p v-else class="ai-note">{{ t('assistant.trackHint') }}</p>
</form>
</template>

<style scoped src="./assistant-form.css" />
