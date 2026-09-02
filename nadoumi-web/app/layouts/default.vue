<template>
  <div class="flex min-h-screen flex-col">
    <SiteHeader />
    <main class="flex-1 py-10">
      <NContainer>
        <slot />
      </NContainer>
    </main>
    <SiteFooter />
  </div>
</template>
