<script setup lang="ts">
import aiLogo from '~/assets/images/ailogo.jpeg'
import { knockOutFlatBackground } from '~/utils/flatBackground'

const { t } = useI18n()

const SEEN_KEY = 'nad.assistant.seen'

const open = ref(false)
const seen = ref(true)
const tab = ref<'find' | 'track'>('find')

/**
 * ailogo.jpeg has a flat (usually white) baked-in background that looks bad as a
 * bare floating icon. Knock it out client-side; falls back to the raw JPEG when
 * the image is not a logo on a flat field.
 */
const logoSrc = ref(aiLogo)

function keyOutBackground() {
  const img = new Image()
  img.crossOrigin = 'anonymous'
  img.onload = () => {
    try {
      const canvas = document.createElement('canvas')
      canvas.width = img.naturalWidth
      canvas.height = img.naturalHeight
      const ctx = canvas.getContext('2d')
      if (!ctx) return
      ctx.drawImage(img, 0, 0)
      const data = ctx.getImageData(0, 0, canvas.width, canvas.height)
      if (!knockOutFlatBackground(data.data, canvas.width, canvas.height)) return
      ctx.putImageData(data, 0, 0)
      logoSrc.value = canvas.toDataURL('image/png')
    }
    catch { /* keep the raw jpeg */ }
  }
  img.src = aiLogo
}

function onKey(e: KeyboardEvent) {
  if (e.key === 'Escape' && open.value) close()
}

onMounted(() => {
  keyOutBackground()
  try {
    seen.value = localStorage.getItem(SEEN_KEY) === '1'
  }
  catch { seen.value = true }
  window.addEventListener('keydown', onKey)
})
onBeforeUnmount(() => window.removeEventListener('keydown', onKey))

function toggle() {
  open.value = !open.value
  if (open.value && !seen.value) {
    seen.value = true
    try {
      localStorage.setItem(SEEN_KEY, '1')
    }
    catch { /* private mode — pulse just keeps showing */ }
  }
}

function close() {
  open.value = false
}
</script>

<template>
  <div class="ai-assistant">
    <!-- panel -->
    <transition name="ai-pop">
      <section
        v-if="open"
        class="ai-panel"
        role="dialog"
        aria-modal="false"
        :aria-label="t('assistant.title')"
      >
        <header class="ai-panel__head">
          <img :src="logoSrc" alt="" class="ai-panel__avatar">
          <div class="min-w-0 flex-1">
            <p class="font-display text-sm font-semibold text-slate-900">{{ t('assistant.title') }}</p>
            <p class="truncate text-xs text-slate-500">{{ t('assistant.subtitle') }}</p>
          </div>
          <button type="button" class="ai-panel__close" :aria-label="t('assistant.close')" @click="close">
            <svg viewBox="0 0 20 20" class="h-4 w-4" fill="none" aria-hidden="true">
              <path d="M5 5l10 10M15 5L5 15" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" />
            </svg>
          </button>
        </header>

        <div class="ai-panel__body">
          <p class="ai-bubble">{{ t('assistant.greeting') }}</p>

          <div class="ai-tabs">
            <button type="button" :class="['ai-tab', tab === 'find' && 'is-active']" @click="tab = 'find'">
              {{ t('assistant.tabFind') }}
            </button>
            <button type="button" :class="['ai-tab', tab === 'track' && 'is-active']" @click="tab = 'track'">
              {{ t('assistant.tabTrack') }}
            </button>
          </div>

          <AssistantFindPanel v-if="tab === 'find'" @navigate="close" />
          <AssistantTrackPanel v-else @navigate="close" />
        </div>

        <p class="ai-panel__foot">{{ t('assistant.disclaimer') }}</p>
      </section>
    </transition>

    <!-- floating button -->
    <button
      type="button"
      class="ai-fab"
      :class="{ 'is-open': open }"
      :aria-label="t('assistant.fabLabel')"
      :aria-expanded="open ? 'true' : 'false'"
      @click="toggle"
    >
      <span v-if="!seen && !open" class="ai-fab__ping" aria-hidden="true" />
      <img :src="logoSrc" alt="" class="ai-fab__img">
    </button>
  </div>
</template>

<style scoped>
.ai-assistant {
  position: fixed;
  right: max(1rem, env(safe-area-inset-right));
  bottom: max(5rem, calc(env(safe-area-inset-bottom) + 4rem));
  z-index: 60;
}
.ai-fab {
  position: relative;
  display: grid;
  place-items: center;
  height: 4.5rem;
  width: 4.5rem;
  border: 0;
  background: transparent;
  filter: drop-shadow(0 8px 16px rgba(15, 23, 42, 0.3));
  transition: transform 0.18s ease, filter 0.18s ease;
  -webkit-tap-highlight-color: transparent;
}
.ai-fab:hover { transform: translateY(-2px) scale(1.06); }
.ai-fab:active,
.ai-fab.is-open { transform: scale(0.92); }
.ai-fab__img {
  height: 100%;
  width: 100%;
  object-fit: contain;
  display: block;
}
.ai-fab__ping {
  position: absolute;
  inset: 8%;
  border-radius: 9999px;
  border: 2px solid #6366f1;
  animation: ai-ping 1.8s cubic-bezier(0, 0, 0.2, 1) infinite;
}
@keyframes ai-ping {
  0% { transform: scale(1); opacity: 0.7; }
  75%, 100% { transform: scale(1.6); opacity: 0; }
}
.ai-panel {
  position: absolute;
  right: 0;
  bottom: 5rem;
  width: min(22rem, calc(100vw - 2rem));
  max-height: min(30rem, calc(100vh - 11rem));
  display: flex;
  flex-direction: column;
  border-radius: 1rem;
  background: #fff;
  border: 1px solid rgb(226 232 240);
  box-shadow: 0 24px 60px -12px rgba(15, 23, 42, 0.35);
  overflow: hidden;
}
.ai-panel__head {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.75rem 0.875rem;
  border-bottom: 1px solid rgb(241 245 249);
  background: linear-gradient(180deg, rgb(248 250 252), #fff);
}
.ai-panel__avatar {
  height: 2.25rem;
  width: 2.25rem;
  object-fit: contain;
  flex-shrink: 0;
}
.ai-panel__close {
  display: grid;
  place-items: center;
  height: 1.75rem;
  width: 1.75rem;
  border-radius: 0.5rem;
  color: rgb(100 116 139);
}
.ai-panel__close:hover { background: rgb(241 245 249); }
.ai-panel__body {
  padding: 0.875rem;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.ai-bubble {
  background: rgb(248 250 252);
  border: 1px solid rgb(241 245 249);
  border-radius: 0.75rem;
  padding: 0.625rem 0.75rem;
  font-size: 0.8125rem;
  color: rgb(51 65 85);
}
.ai-tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.375rem;
  background: rgb(241 245 249);
  border-radius: 0.625rem;
  padding: 0.25rem;
}
.ai-tab {
  padding: 0.375rem 0.5rem;
  border-radius: 0.5rem;
  font-size: 0.8125rem;
  font-weight: 600;
  color: rgb(71 85 105);
}
.ai-tab.is-active {
  background: #fff;
  color: rgb(15 23 42);
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.12);
}
.ai-panel__foot {
  padding: 0.5rem 0.875rem;
  border-top: 1px solid rgb(241 245 249);
  font-size: 0.6875rem;
  color: rgb(148 163 184);
}
.ai-pop-enter-active,
.ai-pop-leave-active { transition: opacity 0.16s ease, transform 0.16s ease; }
.ai-pop-enter-from,
.ai-pop-leave-to { opacity: 0; transform: translateY(8px) scale(0.98); }
@media (prefers-reduced-motion: reduce) {
  .ai-fab, .ai-fab__ping, .ai-pop-enter-active, .ai-pop-leave-active { transition: none; animation: none; }
}
</style>
