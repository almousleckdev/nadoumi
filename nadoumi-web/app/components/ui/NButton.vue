<script setup lang="ts">
const props = withDefaults(defineProps<{
  variant?: 'primary' | 'secondary' | 'ghost' | 'link'
  size?: 'sm' | 'md' | 'lg'
  type?: 'button' | 'submit'
  loading?: boolean
  disabled?: boolean
  block?: boolean
  to?: string
}>(), { variant: 'primary', size: 'md', type: 'button', to: undefined })

const isDisabled = computed(() => props.disabled || props.loading)

const base =
  'inline-flex items-center justify-center gap-2 rounded-md font-semibold ' +
  'transition-colors disabled:opacity-60 disabled:pointer-events-none'

const sizes: Record<string, string> = {
  sm: 'text-sm px-3 py-1.5',
  md: 'text-base px-4 py-2',
  lg: 'text-base px-5 py-2.5',
}

const variants: Record<string, string> = {
  primary: 'bg-brand-600 text-white hover:bg-brand-700',
  secondary: 'bg-white text-slate-900 border border-slate-200 hover:bg-slate-50',
  ghost: 'bg-transparent text-slate-700 hover:bg-slate-100',
  link: 'bg-transparent text-brand-700 hover:underline px-0 py-0',
}

const classes = computed(() => [
  base, sizes[props.size], variants[props.variant],
  props.block ? 'w-full' : '',
])
</script>

<template>
  <NuxtLink
    v-if="to"
    :to="isDisabled ? undefined : to"
    :aria-disabled="isDisabled ? 'true' : undefined"
    :tabindex="isDisabled ? -1 : undefined"
    :class="classes"
  >
    <NSpinner v-if="loading" size="sm" class="n-btn__spin" />
    <slot />
  </NuxtLink>
  <button v-else :type="type" :disabled="isDisabled" :class="classes">
    <NSpinner v-if="loading" size="sm" class="n-btn__spin" />
    <slot />
  </button>
</template>
