<script setup lang="ts">
// Auth screens: the Nadoumi header + footer stay; the form is centred in the
// remaining space. Same design system as the rest of the site.
</script>

<template>
  <div class="flex min-h-screen flex-col bg-slate-50">
    <SiteHeader />
    <main class="flex flex-1 items-center justify-center px-4 py-10 sm:py-16">
      <div class="w-full max-w-md">
        <slot />
      </div>
    </main>
    <SiteFooter />
  </div>
</template>
