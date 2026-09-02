<script setup lang="ts">
const { t } = useI18n()
const localePath = useLocalePath()
const year = new Date().getFullYear()
</script>

<template>
  <footer class="mt-20 border-t border-slate-200 bg-slate-50">
    <NContainer>
      <div class="grid gap-8 py-12 sm:grid-cols-2 lg:grid-cols-4">
        <div class="grid gap-3">
          <span class="font-display text-base font-bold">Nadoumi</span>
          <p class="text-sm text-slate-600">{{ t('footer.mission') }}</p>
          <NLocaleSwitcher />
        </div>
        <div class="grid content-start gap-2 text-sm">
          <p class="font-semibold text-slate-900">{{ t('footer.explore') }}</p>
          <NuxtLink :to="localePath('/scholarships')" class="text-slate-600 hover:text-slate-900">{{ t('nav.scholarships') }}</NuxtLink>
          <NuxtLink :to="localePath('/universities')" class="text-slate-600 hover:text-slate-900">{{ t('nav.universities') }}</NuxtLink>
          <NuxtLink :to="localePath('/programs')" class="text-slate-600 hover:text-slate-900">{{ t('nav.programs') }}</NuxtLink>
          <NuxtLink :to="localePath('/destinations')" class="text-slate-600 hover:text-slate-900">{{ t('nav.destinations') }}</NuxtLink>
        </div>
        <div class="grid content-start gap-2 text-sm">
          <p class="font-semibold text-slate-900">{{ t('footer.company') }}</p>
          <NuxtLink :to="localePath('/about')" class="text-slate-600 hover:text-slate-900">{{ t('nav.about') }}</NuxtLink>
          <NuxtLink :to="localePath('/contact')" class="text-slate-600 hover:text-slate-900">{{ t('nav.contact') }}</NuxtLink>
          <NuxtLink :to="localePath('/privacy')" class="text-slate-600 hover:text-slate-900">{{ t('footer.privacy') }}</NuxtLink>
          <NuxtLink :to="localePath('/terms')" class="text-slate-600 hover:text-slate-900">{{ t('footer.terms') }}</NuxtLink>
        </div>
        <div class="grid content-start gap-2 text-sm">
          <p class="font-semibold text-slate-900">{{ t('footer.getStarted') }}</p>
          <NuxtLink :to="localePath('/register')" class="text-slate-600 hover:text-slate-900">{{ t('nav.getStarted') }}</NuxtLink>
          <NuxtLink :to="localePath('/login')" class="text-slate-600 hover:text-slate-900">{{ t('nav.signIn') }}</NuxtLink>
          <a href="mailto:support@nadoumi.com" class="text-slate-600 hover:text-slate-900">{{ t('footer.contactEmail') }}</a>
        </div>
      </div>
      <div class="flex flex-col gap-2 border-t border-slate-200 py-6 text-xs text-slate-500 sm:flex-row sm:items-center sm:justify-between">
        <p>© {{ year }} {{ t('footer.rights') }}</p>
        <div class="flex gap-4">
          <NuxtLink :to="localePath('/privacy')" class="hover:text-slate-700">{{ t('footer.privacy') }}</NuxtLink>
          <NuxtLink :to="localePath('/terms')" class="hover:text-slate-700">{{ t('footer.terms') }}</NuxtLink>
        </div>
      </div>
    </NContainer>
  </footer>
</template>
