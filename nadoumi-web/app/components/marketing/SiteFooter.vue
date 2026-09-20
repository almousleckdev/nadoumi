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

          <div class="mt-5 flex items-end gap-4">
            <a
              :href="whatsappHref"
              target="_blank"
              rel="noopener noreferrer"
              :aria-label="t('footer.whatsapp')"
              class="group block"
            >
              <div class="overflow-hidden rounded-lg border border-slate-200 bg-white p-1.5 shadow-xs transition-all duration-200 ease-out group-hover:-translate-y-1 group-hover:border-green-300 group-hover:shadow-md">
                <img :src="whatsappIconUrl" alt="" width="96" height="102" class="h-28 w-auto object-contain">
              </div>
              <p class="mt-1.5 text-center text-xs font-medium text-slate-500 transition-colors duration-200 group-hover:text-slate-800">WhatsApp</p>
            </a>

            <div class="group block" :title="t('footer.wechat')">
              <div class="overflow-hidden rounded-lg border border-slate-200 bg-white p-1.5 shadow-xs transition-all duration-200 ease-out group-hover:-translate-y-1 group-hover:border-brand-300 group-hover:shadow-md">
                <img :src="wechatQrUrl" :alt="t('footer.wechat')" width="96" height="143" class="h-28 w-auto object-contain">
              </div>
              <p class="mt-1.5 text-center text-xs font-medium text-slate-500 transition-colors duration-200 group-hover:text-slate-800">WeChat</p>
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
