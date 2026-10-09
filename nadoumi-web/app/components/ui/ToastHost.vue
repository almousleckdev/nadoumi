<script setup lang="ts">
import { useToast, type ToastKind } from '~/composables/useToast'

/** Renders the app's toasts: top centre on phones, top right from `sm`. Mounted once per layout. */
const { toasts, dismiss } = useToast()
const { t } = useI18n()

const styles: Record<ToastKind, string> = {
  success: 'border-emerald-200 text-slate-900',
  error: 'border-red-200 text-slate-900',
  info: 'border-slate-200 text-slate-900',
}
const icons: Record<ToastKind, string> = {
  success: 'bg-emerald-100 text-emerald-700',
  error: 'bg-red-100 text-red-700',
  info: 'bg-slate-100 text-slate-600',
}
</script>

<template>
  <div
    class="pointer-events-none fixed inset-x-0 top-4 z-[70] flex flex-col items-center gap-2 px-4 sm:inset-x-auto sm:end-5 sm:top-5 sm:items-end"
    role="region"
    aria-live="polite"
    :aria-label="t('common.notifications')"
    data-test="toasts"
  >
    <TransitionGroup name="toast">
      <div
        v-for="toast in toasts"
        :key="toast.id"
        class="pointer-events-auto flex w-full max-w-sm items-center gap-3 rounded-xl border bg-white py-3 ps-3 pe-2 shadow-lg ring-1 ring-black/5"
        :class="styles[toast.kind]"
        :role="toast.kind === 'error' ? 'alert' : 'status'"
        data-test="toast"
        :data-kind="toast.kind"
      >
        <span class="grid h-7 w-7 shrink-0 place-items-center rounded-full" :class="icons[toast.kind]" aria-hidden="true">
          <svg v-if="toast.kind === 'success'" viewBox="0 0 24 24" class="h-4 w-4" fill="none" stroke="currentColor" stroke-width="2.6"><path d="m5 12 4.5 4.5L19 7" stroke-linecap="round" stroke-linejoin="round" /></svg>
          <svg v-else-if="toast.kind === 'error'" viewBox="0 0 24 24" class="h-4 w-4" fill="none" stroke="currentColor" stroke-width="2.4"><path d="M12 8v5m0 3.5h.01" stroke-linecap="round" /></svg>
          <svg v-else viewBox="0 0 24 24" class="h-4 w-4" fill="none" stroke="currentColor" stroke-width="2.4"><path d="M12 11v5m0-8.5h.01" stroke-linecap="round" /></svg>
        </span>
        <p class="min-w-0 flex-1 text-sm font-medium">{{ toast.message }}</p>
        <button
          type="button"
          class="grid h-7 w-7 shrink-0 place-items-center rounded-full text-slate-500 hover:bg-slate-100 focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand-500"
          :aria-label="t('common.dismiss')"
          @click="dismiss(toast.id)"
        >
          ×
        </button>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.toast-enter-active, .toast-leave-active { transition: transform 0.22s ease, opacity 0.22s ease; }
.toast-enter-from { transform: translateY(-12px) scale(0.98); opacity: 0; }
.toast-leave-to { transform: translateX(16px); opacity: 0; }
.toast-leave-active { position: absolute; }
@media (prefers-reduced-motion: reduce) {
  .toast-enter-active, .toast-leave-active { transition: opacity 0.01s; }
  .toast-enter-from, .toast-leave-to { transform: none; }
}
</style>
