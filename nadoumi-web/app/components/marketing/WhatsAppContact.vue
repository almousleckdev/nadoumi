<script setup lang="ts">
import whatsappIconUrl from '~/assets/images/whatsapp.png'
import { CONTACT } from '~/data/contact'

const { t } = useI18n()

/** wa.me needs digits only, no `+` or spaces. */
const whatsappHref = `https://wa.me/${CONTACT.phones[0].replace(/\D/g, '')}`
</script>

<template>
  <div class="group relative inline-block">
    <a
      :href="whatsappHref"
      target="_blank"
      rel="noopener noreferrer"
      :aria-label="t('footer.whatsapp')"
      class="inline-flex h-9 items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-1 text-xs font-medium text-slate-700 shadow-sm transition hover:border-[#25D366] hover:text-[#25D366] hover:shadow"
    >
      <span class="flex h-5 w-5 items-center justify-center rounded-full bg-[#25D366] text-white">
        <WhatsAppIcon />
      </span>
      <span>WhatsApp</span>
    </a>

    <div
      class="pointer-events-none absolute bottom-full left-0 z-50 mb-3 w-64 rounded-2xl border border-slate-200/90 bg-white p-4 shadow-xl opacity-0 translate-y-2 transition-all duration-200 ease-out group-hover:pointer-events-auto group-hover:opacity-100 group-hover:translate-y-0 group-focus-within:pointer-events-auto group-focus-within:opacity-100 group-focus-within:translate-y-0"
      data-test="whatsapp-popover"
    >
      <div class="absolute -bottom-3 left-0 right-0 h-3" />

      <div class="mb-2.5 flex items-center justify-between">
        <div class="flex items-center gap-2">
          <span class="flex h-5 w-5 items-center justify-center rounded-full bg-[#25D366] text-white">
            <WhatsAppIcon />
          </span>
          <span class="text-xs font-semibold text-slate-900">WhatsApp</span>
        </div>
        <span class="rounded bg-emerald-50 px-1.5 py-0.5 text-[10px] font-medium text-emerald-700">Official</span>
      </div>

      <div class="overflow-hidden rounded-xl border border-slate-200/80 bg-black p-2 text-center shadow-inner">
        <img
          :src="whatsappIconUrl"
          :alt="t('footer.whatsapp')"
          class="mx-auto aspect-square w-full rounded-lg object-contain"
          width="200"
          height="200"
        >
      </div>

      <p class="mt-2 text-center text-[11px] leading-tight text-slate-500">
        {{ t('footer.whatsappScanHint') }}
      </p>

      <a
        :href="whatsappHref"
        target="_blank"
        rel="noopener noreferrer"
        class="mt-2.5 block w-full rounded-lg bg-[#25D366] py-1.5 text-center text-xs font-medium text-white transition hover:bg-[#20bd5a]"
      >
        {{ t('footer.whatsappOpenChat') }}
      </a>

      <div class="absolute -bottom-1.5 left-6 h-3 w-3 rotate-45 border-b border-r border-slate-200/90 bg-white" />
    </div>
  </div>
</template>
