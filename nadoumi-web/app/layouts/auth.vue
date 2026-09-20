<script setup lang="ts">
import logoUrl from '~/assets/images/logo.jpg'

// Login/register: same background photo as the admin login (copied from
// nadoumi-admin/public/logo/background.png), centred card on top. No marketing
// header/footer: a focused entry point, not a page to browse from.
const localePath = useLocalePath()
</script>

<template>
  <div
    class="relative flex min-h-screen flex-col items-center justify-center overflow-hidden bg-[var(--ink-900)] bg-cover bg-center bg-no-repeat px-4 py-10 sm:py-16"
    style="background-image: url('/images/auth-background.png')"
  >
    <NuxtLink :to="localePath('/')" class="relative mb-8 rounded-2xl bg-white/95 px-5 py-3 shadow-md">
      <img :src="logoUrl" alt="Nadoumi" class="h-8 w-auto" width="128" height="32">
    </NuxtLink>
    <div class="relative w-full max-w-md">
      <slot />
    </div>
  </div>
</template>
