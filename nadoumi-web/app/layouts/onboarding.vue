<script setup lang="ts">
// Onboarding is a focused profile-completion workflow — NOT the dashboard.
// The Nadoumi header/footer stay (same design system); no dashboard sidebar.
</script>

<template>
  <div class="flex min-h-screen flex-col bg-slate-50">
    <SiteHeader />
    <main class="mx-auto w-full max-w-3xl flex-1 px-4 py-10 sm:py-14">
      <slot />
    </main>
    <SiteFooter />
  </div>
</template>
