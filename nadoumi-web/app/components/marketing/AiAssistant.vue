<script setup lang="ts">
import aiLogo from '~/assets/images/ailogo.jpeg'
import type { Page, ScholarshipCard, ApplicationSummary } from '~/types/catalog'

const { t } = useI18n()
const localePath = useLocalePath()
const { publicGet, studentFetch } = useApi()

const open = ref(false)
const seen = ref(true)
const tab = ref<'find' | 'track'>('find')

/**
 * ailogo.jpeg has a flat (usually white) baked-in background that looks bad as a
 * bare floating icon. Knock it out client-side: load the image, sample the four
 * corners, and if they agree, make every pixel within tolerance of that colour
 * transparent (feathered). Falls back to the raw JPEG if the corners disagree
 * (i.e. it's a photo, not a logo on a flat field).
 */
const logoSrc = ref(aiLogo)

function keyOutBackground() {
  const img = new Image()
  img.crossOrigin = 'anonymous'
  img.onload = () => {
    try {
      const c = document.createElement('canvas')
      c.width = img.naturalWidth
      c.height = img.naturalHeight
      const ctx = c.getContext('2d')
      if (!ctx) return
      ctx.drawImage(img, 0, 0)
      const data = ctx.getImageData(0, 0, c.width, c.height)
      const px = data.data
      const rgbAt = (x: number, y: number): [number, number, number] => {
        const i = (y * c.width + x) * 4
        return [px[i] ?? 0, px[i + 1] ?? 0, px[i + 2] ?? 0]
      }
      const corners: [number, number, number][] = [
        rgbAt(0, 0), rgbAt(c.width - 1, 0), rgbAt(0, c.height - 1), rgbAt(c.width - 1, c.height - 1),
      ]
      const key = corners[0]
      if (!key) return
      const md = (a: [number, number, number], b: [number, number, number]) =>
        Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]) + Math.abs(a[2] - b[2])
      if (!corners.every(cn => md(cn, key) < 40)) return

      const [kr, kg, kb] = key
      const HARD = 45 // fully transparent within this manhattan distance
      const SOFT = 110 // feather out to here
      for (let i = 0; i < px.length; i += 4) {
        const d = Math.abs((px[i] ?? 0) - kr) + Math.abs((px[i + 1] ?? 0) - kg) + Math.abs((px[i + 2] ?? 0) - kb)
        if (d <= HARD) px[i + 3] = 0
        else if (d < SOFT) px[i + 3] = Math.round(((px[i + 3] ?? 255) * (d - HARD)) / (SOFT - HARD))
      }
      ctx.putImageData(data, 0, 0)
      logoSrc.value = c.toDataURL('image/png')
    }
    catch { /* keep the raw jpeg */ }
  }
  img.src = aiLogo
}

onMounted(() => {
  keyOutBackground()
  try {
    seen.value = localStorage.getItem('nad.assistant.seen') === '1'
  }
  catch { seen.value = true }
})

function toggle() {
  open.value = !open.value
  if (open.value && !seen.value) {
    seen.value = true
    try {
      localStorage.setItem('nad.assistant.seen', '1')
    }
    catch { /* private mode — pulse just keeps showing */ }
  }
}
function close() {
  open.value = false
}

// close on Escape
function onKey(e: KeyboardEvent) {
  if (e.key === 'Escape' && open.value) close()
}
onMounted(() => window.addEventListener('keydown', onKey))
onBeforeUnmount(() => window.removeEventListener('keydown', onKey))

/* ---- find scholarships ---- */
const query = ref('')
const funding = ref('')
const finding = ref(false)
const findError = ref(false)
const results = ref<ScholarshipCard[]>([])
const searched = ref(false)

async function runSearch() {
  if (finding.value) return
  finding.value = true
  findError.value = false
  searched.value = true
  try {
    const params: Record<string, unknown> = { size: 5 }
    if (query.value.trim()) params.q = query.value.trim()
    if (funding.value) params.funding = funding.value
    const page = await publicGet<Page<ScholarshipCard>>('scholarships', params)
    results.value = page?.content ?? []
  }
  catch {
    findError.value = true
    results.value = []
  }
  finally {
    finding.value = false
  }
}

const allResultsLink = computed(() => {
  const q = new URLSearchParams()
  if (query.value.trim()) q.set('q', query.value.trim())
  if (funding.value) q.set('funding', funding.value)
  const qs = q.toString()
  return localePath(`/scholarships${qs ? `?${qs}` : ''}`)
})

/* ---- track application ---- */
const appId = ref('')
const tracking = ref(false)
const trackState = ref<'idle' | 'ok' | 'notfound' | 'unavailable'>('idle')
const application = ref<ApplicationSummary | null>(null)

async function runTrack() {
  const id = appId.value.trim()
  if (!id || tracking.value) return
  tracking.value = true
  trackState.value = 'idle'
  application.value = null
  try {
    application.value = await studentFetch<ApplicationSummary>(`applications/${encodeURIComponent(id)}`)
    trackState.value = 'ok'
  }
  catch (e) {
    const status = (e as { statusCode?: number; status?: number })?.statusCode
      ?? (e as { status?: number })?.status
    // A real 404 from a present endpoint means "no such application"; anything
    // else (route not deployed yet, 5xx) falls back to the "coming soon" guidance.
    trackState.value = status === 404 ? 'notfound' : 'unavailable'
  }
  finally {
    tracking.value = false
  }
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

          <!-- find scholarships -->
          <form v-if="tab === 'find'" class="ai-form" @submit.prevent="runSearch">
            <input
              v-model="query"
              type="search"
              :placeholder="t('assistant.findPlaceholder')"
              class="ai-input"
            >
            <select v-model="funding" class="ai-input" :aria-label="t('assistant.anyFunding')">
              <option value="">{{ t('assistant.anyFunding') }}</option>
              <option value="FULLY">{{ t('assistant.fundingFully') }}</option>
              <option value="PARTIAL">{{ t('assistant.fundingPartial') }}</option>
              <option value="SELF">{{ t('assistant.fundingSelf') }}</option>
            </select>
            <button type="submit" class="ai-btn" :disabled="finding">
              {{ finding ? t('assistant.searching') : t('assistant.search') }}
            </button>

            <div v-if="searched && !finding" class="ai-results">
              <p v-if="findError" class="ai-note ai-note--warn">{{ t('errors.loadSection') }}</p>
              <p v-else-if="!results.length" class="ai-note">{{ t('assistant.noResults') }}</p>
              <template v-else>
                <p class="ai-results__title">{{ t('assistant.resultsTitle') }}</p>
                <NuxtLink
                  v-for="s in results"
                  :key="s.id"
                  :to="localePath(`/scholarships/${s.slug}`)"
                  class="ai-result"
                  @click="close"
                >
                  <span class="ai-result__title">{{ s.title }}</span>
                  <span class="ai-result__meta">
                    {{ s.country }} · {{ t(`scholarships.funding.${s.fundingModel}`) }}
                    <template v-if="s.deadline"> · {{ t('assistant.deadline') }} {{ s.deadline }}</template>
                  </span>
                </NuxtLink>
                <NuxtLink :to="allResultsLink" class="ai-link" @click="close">{{ t('assistant.viewAll') }} →</NuxtLink>
              </template>
            </div>
            <p v-else-if="!searched" class="ai-note">{{ t('assistant.findHint') }}</p>
          </form>

          <!-- track application -->
          <form v-else class="ai-form" @submit.prevent="runTrack">
            <input
              v-model="appId"
              type="text"
              :placeholder="t('assistant.trackPlaceholder')"
              class="ai-input"
              autocomplete="off"
            >
            <button type="submit" class="ai-btn" :disabled="tracking || !appId.trim()">
              {{ tracking ? t('assistant.trackChecking') : t('assistant.trackCta') }}
            </button>

            <div v-if="trackState === 'ok' && application" class="ai-track">
              <p class="ai-result__title">{{ application.opportunityTitle || `#${application.id}` }}</p>
              <dl class="ai-track__grid">
                <div><dt>{{ t('assistant.trackStatus') }}</dt><dd>{{ application.status }}</dd></div>
                <div v-if="application.stage"><dt>{{ t('assistant.trackStage') }}</dt><dd>{{ application.stage }}</dd></div>
                <div v-if="application.updatedAt"><dt>{{ t('assistant.trackUpdated') }}</dt><dd>{{ application.updatedAt }}</dd></div>
              </dl>
            </div>
            <p v-else-if="trackState === 'notfound'" class="ai-note ai-note--warn">{{ t('assistant.trackNotFound') }}</p>
            <template v-else-if="trackState === 'unavailable'">
              <p class="ai-note">{{ t('assistant.trackComingSoon') }}</p>
              <NuxtLink :to="localePath('/dashboard')" class="ai-link" @click="close">{{ t('assistant.trackGoDashboard') }} →</NuxtLink>
            </template>
            <p v-else class="ai-note">{{ t('assistant.trackHint') }}</p>
          </form>
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
.ai-form {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.ai-input {
  width: 100%;
  border: 1px solid rgb(203 213 225);
  border-radius: 0.5rem;
  padding: 0.5rem 0.625rem;
  font-size: 0.8125rem;
  outline: none;
  background: #fff;
}
.ai-input:focus-visible { border-color: rgb(99 102 241); }
.ai-btn {
  align-self: flex-start;
  border-radius: 0.5rem;
  background: rgb(79 70 229);
  color: #fff;
  font-size: 0.8125rem;
  font-weight: 600;
  padding: 0.5rem 0.875rem;
}
.ai-btn:disabled { opacity: 0.6; }
.ai-results,
.ai-track {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
  margin-top: 0.25rem;
}
.ai-results__title,
.ai-result__title {
  font-size: 0.8125rem;
  font-weight: 600;
  color: rgb(15 23 42);
}
.ai-result {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  border: 1px solid rgb(226 232 240);
  border-radius: 0.5rem;
  padding: 0.5rem 0.625rem;
  text-decoration: none;
  transition: background 0.12s ease;
}
.ai-result:hover { background: rgb(248 250 252); }
.ai-result__meta {
  font-size: 0.6875rem;
  color: rgb(100 116 139);
}
.ai-track__grid {
  display: grid;
  gap: 0.25rem;
  font-size: 0.75rem;
}
.ai-track__grid dt { color: rgb(148 163 184); display: inline; }
.ai-track__grid dd { color: rgb(15 23 42); display: inline; margin-left: 0.25rem; font-weight: 600; }
.ai-note {
  font-size: 0.75rem;
  color: rgb(100 116 139);
}
.ai-note--warn { color: rgb(190 24 93); }
.ai-link {
  font-size: 0.75rem;
  font-weight: 600;
  color: rgb(67 56 202);
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
