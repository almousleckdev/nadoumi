<script setup lang="ts">
// Onboarding is a focused profile-completion workflow, not the dashboard and not the marketing site: no nav
// links, no account menu, nothing that lets a student wander off or sign out mid-flow. The page draws its own
// frame (sidebar stepper and action bar); this layout only provides the toast outlet.
</script>

<template>
  <div class="min-h-screen bg-slate-50">
    <ToastHost />
    <slot />
  </div>
</template>
