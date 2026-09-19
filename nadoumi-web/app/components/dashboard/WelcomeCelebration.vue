<script setup lang="ts">
/** Shown once, the first time a student reaches the dashboard after onboarding. */
defineProps<{ name: string }>()
defineEmits<{ dismiss: [] }>()
const { t } = useI18n()

const PIECES = 28
const COLOURS = ['bg-brand-500', 'bg-amber-400', 'bg-emerald-500', 'bg-sky-400', 'bg-rose-400']
// deterministic so the server and client render the same markup
const pieces = Array.from({ length: PIECES }, (_, i) => ({
  left: `${(i * 37) % 100}%`,
  delay: `${(i % 7) * 0.18}s`,
  duration: `${2.4 + (i % 5) * 0.4}s`,
  colour: COLOURS[i % COLOURS.length],
}))
</script>

<template>
  <div class="fixed inset-0 z-50 grid place-items-center bg-slate-900/50 p-4" role="dialog" aria-modal="true" :aria-label="t('welcome.title')">
    <div class="pointer-events-none absolute inset-0 overflow-hidden motion-reduce:hidden" aria-hidden="true">
      <span
        v-for="(piece, i) in pieces"
        :key="i"
        class="confetti absolute -top-4 h-3 w-2 rounded-sm"
        :class="piece.colour"
        :style="{ left: piece.left, animationDelay: piece.delay, animationDuration: piece.duration }"
      />
    </div>
    <div class="relative w-full max-w-md rounded-2xl bg-white p-8 text-center shadow-lg">
      <p class="text-4xl" aria-hidden="true">🎉</p>
      <h2 class="mt-3 font-display text-2xl font-bold text-slate-900">{{ t('welcome.title', { name }) }}</h2>
      <p class="mt-2 text-sm text-slate-600">{{ t('welcome.body') }}</p>
      <NButton class="mt-6" block @click="$emit('dismiss')">{{ t('welcome.cta') }}</NButton>
    </div>
  </div>
</template>

<style scoped>
.confetti { animation-name: confetti-fall; animation-timing-function: ease-in; animation-iteration-count: infinite; }
@keyframes confetti-fall {
  from { transform: translateY(0) rotate(0deg); opacity: 1; }
  to { transform: translateY(105vh) rotate(540deg); opacity: 0.8; }
}
</style>
