<script setup lang="ts">
// Global, single-instance overlay (mounted once in app.vue) shown briefly while
// the site switches language — triggered by useLocaleTransition().changeLocale.
// Teleported to <body> so it sits above every layout (marketing, dashboard,
// auth, onboarding) regardless of which one is active.
import logoUrl from '~/assets/images/logo.jpg'

const { t } = useI18n()
const { active } = useLocaleTransition()
</script>

<template>
  <Teleport to="body">
    <Transition name="locale-fade">
      <div
        v-if="active"
        class="fixed inset-0 z-[100] flex items-center justify-center bg-white/95 backdrop-blur-sm"
        role="status"
        aria-live="polite"
      >
        <div class="locale-fade__mark flex flex-col items-center gap-4">
          <img :src="logoUrl" alt="" class="h-14 w-auto" width="224" height="56">
          <span class="sr-only">{{ t('common.switchingLanguage') }}</span>
          <span class="locale-fade__dot h-1.5 w-1.5 rounded-full bg-brand-600" aria-hidden="true" />
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.locale-fade-enter-active,
.locale-fade-leave-active {
  transition: opacity 280ms ease;
}
.locale-fade-enter-from,
.locale-fade-leave-to {
  opacity: 0;
}

.locale-fade__mark {
  animation: locale-mark-in 320ms ease-out;
}

.locale-fade__dot {
  animation: locale-dot-pulse 900ms ease-in-out infinite;
}

@keyframes locale-mark-in {
  from {
    opacity: 0;
    transform: scale(0.92);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes locale-dot-pulse {
  0%,
  100% {
    opacity: 0.35;
    transform: scale(0.85);
  }
  50% {
    opacity: 1;
    transform: scale(1.15);
  }
}

@media (prefers-reduced-motion: reduce) {
  .locale-fade__mark,
  .locale-fade__dot {
    animation: none;
  }
}
</style>
