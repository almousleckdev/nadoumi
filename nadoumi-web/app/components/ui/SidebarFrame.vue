<script setup lang="ts">
import logoUrl from '~/assets/images/logo.jpg'

/**
 * The dark student sidebar: brand header and a slot for the navigation. The dashboard and the onboarding
 * wizard both render inside it, so the colours, header and spacing exist once (and match the admin sidebar).
 * Layout classes (position, width, visibility) come from the parent through `class`.
 */
defineProps<{
  /** Where the logo leads; omitted in the onboarding wizard, which must not offer a way out. */
  homeTo?: string
}>()
</script>

<template>
  <aside class="flex flex-col bg-[var(--sidebar-bg)] text-[var(--sidebar-fg)]">
    <div class="flex h-16 shrink-0 items-center justify-center border-b border-white/10 px-4">
      <NuxtLink v-if="homeTo" :to="homeTo" class="block">
        <img :src="logoUrl" alt="Nadoumi" class="h-9 w-auto rounded-md bg-white p-1">
      </NuxtLink>
      <img v-else :src="logoUrl" alt="Nadoumi" class="h-9 w-auto rounded-md bg-white p-1">
    </div>
    <slot />
  </aside>
</template>
