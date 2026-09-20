<script setup lang="ts">
// Login/register: same background photo as the admin login (copied from
// nadoumi-admin/public/logo/background.png), centred card on top. No logo
// mark and no marketing header/footer — matches the admin login exactly,
// a focused entry point, not a page to browse from.
</script>

<template>
  <div
    class="relative flex min-h-screen flex-col items-center justify-center overflow-hidden bg-[var(--ink-900)] bg-cover bg-center bg-no-repeat px-4 py-10 sm:py-16"
    style="background-image: url('/images/auth-background.png')"
  >
    <div class="relative w-full max-w-md">
      <slot />
    </div>
  </div>
</template>
