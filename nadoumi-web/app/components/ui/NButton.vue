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
    <span v-if="loading" class="n-btn__spin" aria-hidden="true">
      <svg class="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
      </svg>
    </span>
    <slot />
  </NuxtLink>
  <button v-else :type="type" :disabled="isDisabled" :class="classes">
    <span v-if="loading" class="n-btn__spin" aria-hidden="true">
      <svg class="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
      </svg>
    </span>
    <slot />
  </button>
</template>
