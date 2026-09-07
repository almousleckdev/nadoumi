<template>
  <div class="flex min-h-screen flex-col bg-white">
    <SiteHeader />
    <!-- Marketing pages manage their own vertical rhythm and full-bleed sections. -->
    <main class="flex-1">
      <slot />
    </main>
    <SiteFooter />
    <AiAssistant />
  </div>
</template>
