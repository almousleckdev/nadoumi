<script setup lang="ts">
import logoUrl from '~/assets/images/logo.jpg'

// Onboarding is a focused profile-completion workflow — NOT the dashboard and NOT
// the marketing site. No nav links, no locale switcher, no account menu: nothing
// that lets a student wander off or sign out mid-flow.
</script>

<template>
  <div class="flex min-h-screen flex-col bg-slate-50">
    <header class="border-b border-slate-200 bg-white">
      <div class="mx-auto flex h-16 w-full max-w-3xl items-center px-4">
        <img :src="logoUrl" alt="Nadoumi" class="h-9 w-auto" width="144" height="36">
      </div>
    </header>
    <main class="mx-auto w-full max-w-3xl flex-1 px-4 py-10 sm:py-14">
      <slot />
    </main>
  </div>
</template>
