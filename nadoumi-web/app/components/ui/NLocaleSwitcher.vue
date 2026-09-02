<script setup lang="ts">
// `LocaleObject` is exported by @nuxtjs/i18n but does not flow through `useI18n().locales`
// in vue-tsc's template check (param stays implicit-any); a local shape keeps typecheck clean.
interface LocaleItem { code: string; name?: string }
const { locale, locales } = useI18n()
const switchLocalePath = useSwitchLocalePath()
const list = computed(() => locales.value as unknown as LocaleItem[])
</script>

<template>
  <NDropdown :label="(list.find((l: LocaleItem) => l.code === locale)?.name) ?? locale">
    <NuxtLink
      v-for="l in list"
      :key="l.code"
      :to="switchLocalePath(l.code)"
      :aria-current="l.code === locale ? 'true' : undefined"
      class="block px-3 py-2 text-sm hover:bg-slate-50"
      :class="l.code === locale ? 'font-semibold text-brand-700' : 'text-slate-700'"
    >
      {{ l.name }}
    </NuxtLink>
  </NDropdown>
</template>
