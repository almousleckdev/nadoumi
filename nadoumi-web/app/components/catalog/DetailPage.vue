<script setup lang="ts">
withDefaults(defineProps<{
  /** Show the sidebar above the main column on small screens (deadline / price panels). */
  asideFirst?: boolean
}>(), { asideFirst: false })
</script>

<template>
  <div class="bg-white">
    <slot name="hero" />
    <NContainer>
      <slot name="facts" />
      <div class="grid gap-8 py-8 lg:grid-cols-[minmax(0,1fr)_21rem] lg:gap-10 lg:py-10">
        <div class="min-w-0 space-y-6">
          <slot />
        </div>
        <aside
          v-if="$slots.aside"
          class="min-w-0 space-y-6 lg:sticky lg:top-24 lg:self-start"
          :class="asideFirst ? 'order-first lg:order-none' : ''"
        >
          <slot name="aside" />
        </aside>
      </div>
    </NContainer>
  </div>
</template>
