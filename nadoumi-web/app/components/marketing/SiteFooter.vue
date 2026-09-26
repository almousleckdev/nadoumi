<script setup lang="ts">
import logoUrl from '~/assets/images/logo.jpg'
import wechatQrUrl from '~/assets/images/wechat.png'
import whatsappIconUrl from '~/assets/images/whatsapp.png'
import { CONTACT, telHref } from '~/data/contact'
import { GUIDES } from '~/data/guides'

const { t } = useI18n()
const localePath = useLocalePath()
const year = new Date().getFullYear()

/** wa.me needs digits only, no `+` or spaces. */
const whatsappHref = `https://wa.me/${CONTACT.phones[0].replace(/\D/g, '')}`

const explore = [
  { to: '/scholarships', key: 'nav.scholarships' },
  { to: '/universities', key: 'nav.universities' },
]
const company = [
  { to: '/about', key: 'nav.about' },
  { to: '/contact', key: 'nav.contact' },
]
const legal = [
  { to: '/privacy', key: 'footer.privacy' },
  { to: '/terms', key: 'footer.terms' },
]
</script>

<template>
  <footer class="border-t border-slate-200 bg-slate-50">
    <NContainer>
      <div class="grid gap-10 py-14 md:grid-cols-2 lg:grid-cols-[1.5fr_1fr_1fr_1.4fr]">
        <div class="max-w-xs">
          <img :src="logoUrl" alt="Nadoumi" class="h-10 w-auto" width="160" height="40">
          <p class="mt-3 text-sm text-slate-600">{{ t('footer.mission') }}</p>

          <div class="mt-5 flex items-center gap-3">
            <!-- WhatsApp trigger & QR hover popover -->
            <div class="group relative inline-block">
              <a
                :href="whatsappHref"
                target="_blank"
                rel="noopener noreferrer"
                :aria-label="t('footer.whatsapp')"
                class="inline-flex h-9 items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-1 text-xs font-medium text-slate-700 shadow-sm transition hover:border-[#25D366] hover:text-[#25D366] hover:shadow"
              >
                <span class="flex h-5 w-5 items-center justify-center rounded-full bg-[#25D366] text-white">
                  <svg class="h-3 w-3 fill-current" viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M12.04 2C6.58 2 2.13 6.45 2.13 11.91C2.13 13.66 2.59 15.36 3.45 16.86L2.05 22L7.3 20.62C8.75 21.41 10.38 21.83 12.04 21.83C17.5 21.83 21.95 17.38 21.95 11.92C21.95 9.27 20.92 6.78 19.05 4.91C17.18 3.03 14.69 2 12.04 2M12.05 3.67C14.25 3.67 16.31 4.53 17.87 6.09C19.42 7.65 20.28 9.72 20.28 11.92C20.28 16.46 16.58 20.15 12.04 20.15C10.56 20.15 9.11 19.76 7.85 19L7.55 18.83L4.43 19.65L5.26 16.61L5.06 16.29C4.24 15 3.8 13.47 3.8 11.91C3.81 7.37 7.5 3.67 12.05 3.67M9.11 6.84C8.94 6.84 8.68 6.9 8.46 7.15C8.23 7.39 7.6 7.98 7.6 9.19C7.6 10.4 8.48 11.57 8.6 11.73C8.72 11.89 10.32 14.36 12.78 15.42C13.36 15.67 13.82 15.82 14.17 15.93C14.76 16.12 15.3 16.09 15.72 16.03C16.2 15.96 17.18 15.43 17.38 14.86C17.59 14.28 17.59 13.79 17.52 13.69C17.46 13.59 17.31 13.53 17.08 13.41C16.85 13.3 15.74 12.75 15.53 12.67C15.32 12.6 15.17 12.56 15.02 12.79C14.87 13.02 14.44 13.53 14.31 13.68C14.18 13.83 14.05 13.85 13.82 13.73C13.6 13.62 12.87 13.38 12 12.61C11.33 12.01 10.87 11.27 10.74 11.04C10.62 10.82 10.72 10.7 10.84 10.59C10.94 10.49 11.06 10.33 11.17 10.2C11.29 10.07 11.33 9.97 11.4 9.82C11.47 9.68 11.44 9.55 11.38 9.43C11.32 9.32 10.88 8.24 10.7 7.79C10.52 7.36 10.33 7.42 10.19 7.41L9.75 7.4C9.56 7.4 9.29 7.46 9.11 6.84Z"/>
                  </svg>
                </span>
                <span>WhatsApp</span>
              </a>

              <!-- Floating hover card -->
              <div
                class="pointer-events-none absolute bottom-full left-0 z-50 mb-3 w-64 rounded-2xl border border-slate-200/90 bg-white p-4 shadow-xl opacity-0 translate-y-2 transition-all duration-200 ease-out group-hover:pointer-events-auto group-hover:opacity-100 group-hover:translate-y-0 group-focus-within:pointer-events-auto group-focus-within:opacity-100 group-focus-within:translate-y-0"
                data-test="whatsapp-popover"
              >
                <!-- Bridge to prevent flicker -->
                <div class="absolute -bottom-3 left-0 right-0 h-3"></div>

                <div class="mb-2.5 flex items-center justify-between">
                  <div class="flex items-center gap-2">
                    <span class="flex h-5 w-5 items-center justify-center rounded-full bg-[#25D366] text-white">
                      <svg class="h-3 w-3 fill-current" viewBox="0 0 24 24"><path d="M12.04 2C6.58 2 2.13 6.45 2.13 11.91C2.13 13.66 2.59 15.36 3.45 16.86L2.05 22L7.3 20.62C8.75 21.41 10.38 21.83 12.04 21.83C17.5 21.83 21.95 17.38 21.95 11.92C21.95 9.27 20.92 6.78 19.05 4.91C17.18 3.03 14.69 2 12.04 2M12.05 3.67C14.25 3.67 16.31 4.53 17.87 6.09C19.42 7.65 20.28 9.72 20.28 11.92C20.28 16.46 16.58 20.15 12.04 20.15C10.56 20.15 9.11 19.76 7.85 19L7.55 18.83L4.43 19.65L5.26 16.61L5.06 16.29C4.24 15 3.8 13.47 3.8 11.91C3.81 7.37 7.5 3.67 12.05 3.67M9.11 6.84C8.94 6.84 8.68 6.9 8.46 7.15C8.23 7.39 7.6 7.98 7.6 9.19C7.6 10.4 8.48 11.57 8.6 11.73C8.72 11.89 10.32 14.36 12.78 15.42C13.36 15.67 13.82 15.82 14.17 15.93C14.76 16.12 15.3 16.09 15.72 16.03C16.2 15.96 17.18 15.43 17.38 14.86C17.59 14.28 17.59 13.79 17.52 13.69C17.46 13.59 17.31 13.53 17.08 13.41C16.85 13.3 15.74 12.75 15.53 12.67C15.32 12.6 15.17 12.56 15.02 12.79C14.87 13.02 14.44 13.53 14.31 13.68C14.18 13.83 14.05 13.85 13.82 13.73C13.6 13.62 12.87 13.38 12 12.61C11.33 12.01 10.87 11.27 10.74 11.04C10.62 10.82 10.72 10.7 10.84 10.59C10.94 10.49 11.06 10.33 11.17 10.2C11.29 10.07 11.33 9.97 11.4 9.82C11.47 9.68 11.44 9.55 11.38 9.43C11.32 9.32 10.88 8.24 10.7 7.79C10.52 7.36 10.33 7.42 10.19 7.41L9.75 7.4C9.56 7.4 9.29 7.46 9.11 6.84Z"/></svg>
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

                <!-- Tooltip arrow -->
                <div class="absolute -bottom-1.5 left-6 h-3 w-3 rotate-45 border-b border-r border-slate-200/90 bg-white"></div>
              </div>
            </div>

            <!-- WeChat trigger & QR hover popover -->
            <div class="group relative inline-block">
              <button
                type="button"
                :aria-label="t('footer.wechat')"
                class="inline-flex h-9 items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-1 text-xs font-medium text-slate-700 shadow-sm transition hover:border-[#07C160] hover:text-[#07C160] hover:shadow cursor-pointer"
              >
                <span class="flex h-5 w-5 items-center justify-center rounded-full bg-[#07C160] text-white">
                  <svg class="h-3 w-3 fill-current" viewBox="0 0 128 110" aria-hidden="true">
                    <path d="M86.635 33.334c1.467 0 2.917.113 4.358.283C87.078 14.392 67.58.111 45.321.111 20.44.111.055 17.987.055 40.687c0 13.104 6.781 23.863 18.115 32.209l-4.527 14.352 15.82-8.364c5.666 1.182 10.207 2.395 15.858 2.395 1.42 0 2.829-.073 4.227-.189-.886-3.19-1.398-6.53-1.398-9.996 0-20.845 16.98-37.76 38.485-37.76zm-24.34-12.936c3.407 0 5.665 2.363 5.665 5.954 0 3.576-2.258 5.97-5.666 5.97-3.392 0-6.795-2.395-6.795-5.97 0-3.591 3.403-5.954 6.795-5.954zM30.616 32.323c-3.393 0-6.818-2.395-6.818-5.971 0-3.591 3.425-5.954 6.818-5.954 3.392 0 5.65 2.363 5.65 5.954 0 3.576-2.258 5.97-5.65 5.97z"/>
                    <path d="M127.945 70.52c0-19.075-18.108-34.623-38.448-34.623-21.537 0-38.5 15.548-38.5 34.623 0 19.108 16.963 34.622 38.5 34.622 4.508 0 9.058-1.2 13.584-2.395l12.414 7.167-3.404-11.923c9.087-7.184 15.854-16.712 15.854-27.471zm-50.928-5.97c-2.254 0-4.53-2.362-4.53-4.773 0-2.378 2.276-4.771 4.53-4.771 3.422 0 5.665 2.393 5.665 4.771 0 2.41-2.243 4.773-5.665 4.773zm24.897 0c-2.24 0-4.498-2.362-4.498-4.773 0-2.378 2.258-4.771 4.498-4.771 3.392 0 5.665 2.393 5.665 4.771 0 2.41-2.273 4.773-5.665 4.773z"/>
                  </svg>
                </span>
                <span>WeChat</span>
              </button>

              <!-- Floating hover card -->
              <div
                class="pointer-events-none absolute bottom-full left-0 z-50 mb-3 w-64 rounded-2xl border border-slate-200/90 bg-white p-4 shadow-xl opacity-0 translate-y-2 transition-all duration-200 ease-out group-hover:pointer-events-auto group-hover:opacity-100 group-hover:translate-y-0 group-focus-within:pointer-events-auto group-focus-within:opacity-100 group-focus-within:translate-y-0"
                data-test="wechat-popover"
              >
                <!-- Bridge to prevent flicker -->
                <div class="absolute -bottom-3 left-0 right-0 h-3"></div>

                <div class="mb-2.5 flex items-center justify-between">
                  <div class="flex items-center gap-2">
                    <span class="flex h-5 w-5 items-center justify-center rounded-full bg-[#07C160] text-white">
                      <svg class="h-3 w-3 fill-current" viewBox="0 0 128 110"><path d="M86.635 33.334c1.467 0 2.917.113 4.358.283C87.078 14.392 67.58.111 45.321.111 20.44.111.055 17.987.055 40.687c0 13.104 6.781 23.863 18.115 32.209l-4.527 14.352 15.82-8.364c5.666 1.182 10.207 2.395 15.858 2.395 1.42 0 2.829-.073 4.227-.189-.886-3.19-1.398-6.53-1.398-9.996 0-20.845 16.98-37.76 38.485-37.76zm-24.34-12.936c3.407 0 5.665 2.363 5.665 5.954 0 3.576-2.258 5.97-5.666 5.97-3.392 0-6.795-2.395-6.795-5.97 0-3.591 3.403-5.954 6.795-5.954zM30.616 32.323c-3.393 0-6.818-2.395-6.818-5.971 0-3.591 3.425-5.954 6.818-5.954 3.392 0 5.65 2.363 5.65 5.954 0 3.576-2.258 5.97-5.65 5.97z"/><path d="M127.945 70.52c0-19.075-18.108-34.623-38.448-34.623-21.537 0-38.5 15.548-38.5 34.623 0 19.108 16.963 34.622 38.5 34.622 4.508 0 9.058-1.2 13.584-2.395l12.414 7.167-3.404-11.923c9.087-7.184 15.854-16.712 15.854-27.471zm-50.928-5.97c-2.254 0-4.53-2.362-4.53-4.773 0-2.378 2.276-4.771 4.53-4.771 3.422 0 5.665 2.393 5.665 4.771 0 2.41-2.243 4.773-5.665 4.773zm24.897 0c-2.24 0-4.498-2.362-4.498-4.773 0-2.378 2.258-4.771 4.498-4.771 3.392 0 5.665 2.393 5.665 4.771 0 2.41-2.273 4.773-5.665 4.773z"/></svg>
        </span>
        <span class="text-xs font-semibold text-slate-900">WeChat</span>
      </div>
      <span class="rounded bg-emerald-50 px-1.5 py-0.5 text-[10px] font-medium text-emerald-700">Official</span>
    </div>

    <div class="overflow-hidden rounded-xl border border-slate-200/80 bg-white p-2 text-center shadow-inner">
      <img
        :src="wechatQrUrl"
        :alt="t('footer.wechat')"
        class="mx-auto h-56 w-auto rounded-lg object-contain"
        width="200"
        height="280"
      >
    </div>

    <p class="mt-2 text-center text-[11px] leading-tight text-slate-500">
      {{ t('footer.wechatScanHint') }}
    </p>

    <!-- Tooltip arrow -->
    <div class="absolute -bottom-1.5 left-6 h-3 w-3 rotate-45 border-b border-r border-slate-200/90 bg-white"></div>
  </div>
</div>
          </div>
        </div>

        <div class="text-sm">
          <p class="font-semibold text-slate-900">{{ t('footer.explore') }}</p>
          <ul class="mt-3 space-y-2">
            <li v-for="l in explore" :key="l.to">
              <NuxtLink :to="localePath(l.to)" class="text-slate-600 no-underline hover:text-slate-900">{{ t(l.key) }}</NuxtLink>
            </li>
          </ul>
          <p class="mt-6 font-semibold text-slate-900">{{ t('footer.company') }}</p>
          <ul class="mt-3 space-y-2">
            <li v-for="l in company" :key="l.to">
              <NuxtLink :to="localePath(l.to)" class="text-slate-600 no-underline hover:text-slate-900">{{ t(l.key) }}</NuxtLink>
            </li>
          </ul>
        </div>

        <div class="text-sm">
          <p class="font-semibold text-slate-900">{{ t('footer.guides') }}</p>
          <ul class="mt-3 space-y-2">
            <li v-for="g in GUIDES" :key="g.slug">
              <NuxtLink :to="localePath(`/guides/${g.slug}`)" class="text-slate-600 no-underline hover:text-slate-900">{{ t(`guides.meta.${g.slug}.title`) }}</NuxtLink>
            </li>
          </ul>
        </div>

        <div class="text-sm">
          <p class="font-semibold text-slate-900">{{ t('footer.contactTitle') }}</p>
          <address class="mt-3 space-y-3 not-italic text-slate-600">
            <p>{{ CONTACT.officeEn }}</p>
            <p>{{ CONTACT.hours }}</p>
            <ul class="space-y-1">
              <li v-for="e in CONTACT.emails" :key="e">
                <a :href="`mailto:${e}`" class="no-underline hover:text-slate-900">{{ e }}</a>
              </li>
            </ul>
            <ul class="space-y-1">
              <li v-for="p in CONTACT.phones" :key="p">
                <a :href="telHref(p)" class="no-underline hover:text-slate-900">{{ p }}</a>
              </li>
            </ul>
          </address>
        </div>
      </div>

      <div class="flex flex-col gap-3 border-t border-slate-200 py-6 text-xs text-slate-500 sm:flex-row sm:items-center sm:justify-between">
        <p>© {{ year }} {{ t('footer.rights') }}</p>
        <div class="flex gap-4">
          <NuxtLink v-for="l in legal" :key="l.to" :to="localePath(l.to)" class="no-underline hover:text-slate-700">{{ t(l.key) }}</NuxtLink>
        </div>
      </div>
    </NContainer>
  </footer>
</template>
