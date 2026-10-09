<script setup lang="ts">
import logo from '~/assets/images/logo.jpg'

// Sign in, register and password help share one frame: a brand panel on large screens and the form on a clean
// white surface. Below `lg` the panel collapses to a compact header so the form is always first on a phone.
const { t } = useI18n()
const localePath = useLocalePath()
const points = ['discover', 'apply', 'chat'] as const
</script>

<template>
  <div class="grid min-h-screen bg-white lg:grid-cols-[minmax(0,5fr)_minmax(0,6fr)]">
    <aside
      class="relative hidden overflow-hidden bg-[var(--ink-900)] text-white lg:flex lg:flex-col lg:justify-between lg:p-12 xl:p-16"
      style="background-image: url('/images/auth-background.png'); background-size: cover; background-position: center"
    >
      <div class="absolute inset-0 bg-gradient-to-r from-[var(--ink-900)] via-[var(--ink-900)]/92 to-[var(--ink-900)]/45" aria-hidden="true" />
      <NuxtLink :to="localePath('/')" class="relative inline-flex items-center gap-3 self-start rounded-lg focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-white">
        <img :src="logo" alt="Nadoumi" class="h-11 w-11 rounded-xl bg-white object-contain p-1">
        <span class="font-display text-xl font-bold tracking-tight">Nadoumi</span>
      </NuxtLink>

      <div class="relative max-w-md">
        <h2 class="font-display text-4xl font-bold leading-tight xl:text-5xl">{{ t('auth.panel.headline') }}</h2>
        <p class="mt-4 text-lg leading-relaxed text-slate-300">{{ t('auth.panel.subtitle') }}</p>
        <ul class="mt-8 grid gap-4">
          <li v-for="p in points" :key="p" class="flex items-start gap-3 text-slate-100">
            <span class="mt-0.5 grid h-6 w-6 shrink-0 place-items-center rounded-full bg-brand-500 text-white" aria-hidden="true">
              <svg viewBox="0 0 24 24" class="h-3.5 w-3.5" fill="none" stroke="currentColor" stroke-width="3"><path d="m5 12 4.5 4.5L19 7" stroke-linecap="round" stroke-linejoin="round" /></svg>
            </span>
            {{ t(`auth.panel.points.${p}`) }}
          </li>
        </ul>
      </div>

      <p class="relative text-sm text-slate-400">© {{ new Date().getFullYear() }} Nadoumi</p>
    </aside>

    <main class="flex flex-col px-5 py-6 sm:px-10">
      <div class="flex items-center justify-between">
        <NuxtLink :to="localePath('/')" class="inline-flex items-center gap-2 rounded-lg lg:invisible">
          <img :src="logo" alt="Nadoumi" class="h-9 w-9 rounded-lg object-contain">
          <span class="font-display text-lg font-bold text-slate-900">Nadoumi</span>
        </NuxtLink>
        <NLocaleSwitcher />
      </div>
      <div class="mx-auto flex w-full max-w-md flex-1 flex-col justify-center py-8">
        <slot />
      </div>
    </main>
  </div>
</template>
