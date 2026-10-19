<script setup lang="ts">
import { whatsappHref } from '~/data/contact'

const { t } = useI18n()
const href = whatsappHref()
</script>

<template>
  <a
    :href="href"
    target="_blank"
    rel="noopener noreferrer"
    :aria-label="t('footer.whatsapp')"
    :title="t('footer.whatsapp')"
    data-test="floating-whatsapp"
    class="fixed bottom-5 end-5 z-40 inline-flex h-14 w-14 items-center justify-center rounded-full bg-[#25D366] text-white shadow-lg ring-4 ring-white/70 transition hover:scale-105 hover:bg-[#20bd5a] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#25D366] motion-reduce:transition-none sm:bottom-6 sm:end-6"
  >
    <span class="absolute inset-0 -z-10 animate-ping rounded-full bg-[#25D366]/40 motion-reduce:hidden" aria-hidden="true" />
    <WhatsAppIcon class="!h-8 !w-8" />
  </a>
</template>
