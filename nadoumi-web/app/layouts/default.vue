<template>
  <div class="nad-shell">
    <AppHeader />
    <main class="nad-container nad-main">
      <slot />
    </main>
    <AppFooter />
  </div>
</template>

<style scoped>
.nad-shell { display: flex; flex-direction: column; min-height: 100vh; }
.nad-main { flex: 1; padding-top: 1.5rem; }
</style>
