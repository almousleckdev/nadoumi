<script setup lang="ts">
const props = defineProps<{ url: string, title: string }>()

const { t } = useI18n()

const COPIED_MS = 2000
const copied = ref(false)
let copiedTimer: ReturnType<typeof setTimeout> | undefined

const targets = computed(() => shareTargets(props.url, props.title))
// The device's own share sheet (phones, some desktops). Known only in the browser, so no SSR mismatch.
const canNativeShare = ref(false)
onMounted(() => {
  canNativeShare.value = typeof navigator.share === 'function'
})
onBeforeUnmount(() => clearTimeout(copiedTimer))

async function copyLink() {
  try {
    await navigator.clipboard.writeText(props.url)
    copied.value = true
    clearTimeout(copiedTimer)
    copiedTimer = setTimeout(() => { copied.value = false }, COPIED_MS)
  }
  catch {
    // clipboard blocked: the network links below still work
  }
}

async function nativeShare() {
  try {
    await navigator.share({ title: props.title, url: props.url })
  }
  catch {
    // the reader closed the share sheet
  }
}

const itemClass = 'flex w-full items-center px-4 py-2 text-start text-sm text-slate-700 no-underline hover:bg-slate-50 hover:no-underline'
</script>

<template>
  <div class="inline-flex items-center gap-2">
    <span class="text-sm font-medium text-emerald-700" role="status" aria-live="polite">
      <template v-if="copied">{{ t('news.linkCopied') }}</template>
    </span>
    <NDropdown :label="t('news.share')">
      <template #trigger>
        <svg viewBox="0 0 24 24" class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M12 3v12M8 7l4-4 4 4M5 12v7a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-7" />
        </svg>
        <span class="text-sm font-medium text-slate-700">{{ t('news.share') }}</span>
      </template>
      <button v-if="canNativeShare" type="button" role="menuitem" :class="itemClass" @click="nativeShare">
        {{ t('news.shareMore') }}
      </button>
      <button type="button" role="menuitem" :class="itemClass" @click="copyLink">
        {{ t('news.copyLink') }}
      </button>
      <a
        v-for="target in targets"
        :key="target.key"
        :href="target.href"
        target="_blank"
        rel="noopener noreferrer"
        role="menuitem"
        :class="itemClass"
      >
        {{ t(`news.shareOn.${target.key}`) }}
      </a>
    </NDropdown>
  </div>
</template>
