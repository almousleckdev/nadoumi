<script setup lang="ts">
const year = new Date().getFullYear()
</script>

<template>
  <footer class="nad-footer">
    <div class="nad-container">
      <p>© {{ year }} Nadoumi. International education platform.</p>
    </div>
  </footer>
</template>

<style scoped>
.nad-footer { border-top: 1px solid var(--nad-border); margin-top: 4rem; padding: 2rem 0; color: var(--nad-muted); font-size: 0.9rem; }
</style>
