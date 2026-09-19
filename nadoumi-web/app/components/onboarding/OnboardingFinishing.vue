<script setup lang="ts">
import { FINISH_HOLD_MS } from '~/constants/onboarding'

/**
 * The calm hold between Finish and the dashboard. The server has already recorded the onboarding,
 * so this is only a pause; it ends by itself after `FINISH_HOLD_MS`.
 */
const emit = defineEmits<{ done: [] }>()
const { t } = useI18n()

const TICK_MS = 100
const MESSAGE_KEYS = ['onboarding.finishing.profile', 'onboarding.finishing.documents', 'onboarding.finishing.dashboard'] as const

const elapsed = ref(0)
const percent = computed(() => Math.min(100, Math.round((elapsed.value / FINISH_HOLD_MS) * 100)))
const message = computed(() => t(MESSAGE_KEYS[Math.min(MESSAGE_KEYS.length - 1, Math.floor((elapsed.value / FINISH_HOLD_MS) * MESSAGE_KEYS.length))]!))

let timer: ReturnType<typeof setInterval> | undefined
onMounted(() => {
  timer = setInterval(() => {
    elapsed.value += TICK_MS
    if (elapsed.value >= FINISH_HOLD_MS) {
      clearInterval(timer)
      emit('done')
    }
  }, TICK_MS)
})
onBeforeUnmount(() => clearInterval(timer))
</script>

<template>
  <section class="mx-auto grid max-w-md justify-items-center gap-6 py-16 text-center" role="status" aria-live="polite">
    <NSpinner size="md" class="text-brand-600" :label="t('common.loading')" />
    <div>
      <h1 class="font-display text-2xl font-bold text-slate-900">{{ t('onboarding.finishing.title') }}</h1>
      <p class="mt-2 text-sm text-slate-500">{{ message }}</p>
    </div>
    <div class="h-2 w-full overflow-hidden rounded-full bg-slate-100" role="progressbar" :aria-valuenow="percent" aria-valuemin="0" aria-valuemax="100">
      <div class="h-2 origin-left rounded-full bg-brand-500 transition-transform duration-100 ease-linear" :style="{ transform: `scaleX(${percent / 100})` }" />
    </div>
    <p class="text-xs text-slate-400">{{ t('onboarding.finishing.hint') }}</p>
  </section>
</template>
