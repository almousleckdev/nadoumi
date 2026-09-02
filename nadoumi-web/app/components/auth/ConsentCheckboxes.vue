<script setup lang="ts">
const props = withDefaults(defineProps<{
  modelValue: { terms: boolean; privacy: boolean }
  invalid?: boolean
  /** one combined "Terms and Privacy Policy" checkbox instead of two */
  single?: boolean
}>(), { invalid: false, single: false })
const emit = defineEmits<{ 'update:modelValue': [value: { terms: boolean; privacy: boolean }] }>()

const { t } = useI18n()
const localePath = useLocalePath()

function set(part: 'terms' | 'privacy', checked: boolean) {
  emit('update:modelValue', { ...props.modelValue, [part]: checked })
}
function setBoth(checked: boolean) {
  emit('update:modelValue', { terms: checked, privacy: checked })
}
</script>

<template>
  <div class="grid gap-2">
    <NCheckbox
      v-if="single"
      id="accept-terms"
      :model-value="modelValue.terms && modelValue.privacy"
      @update:model-value="setBoth($event)"
    >
      <i18n-t keypath="auth.acceptBothShort">
        <template #terms>
          <NuxtLink :to="localePath('/terms')" class="text-brand-700 hover:underline">{{ t('footer.terms') }}</NuxtLink>
        </template>
        <template #privacy>
          <NuxtLink :to="localePath('/privacy')" class="text-brand-700 hover:underline">{{ t('footer.privacy') }}</NuxtLink>
        </template>
      </i18n-t>
    </NCheckbox>

    <template v-else>
      <NCheckbox id="accept-terms" :model-value="modelValue.terms" @update:model-value="set('terms', $event)">
        <i18n-t keypath="auth.acceptTermsShort">
          <template #terms>
            <NuxtLink :to="localePath('/terms')" class="text-brand-700 hover:underline">{{ t('footer.terms') }}</NuxtLink>
          </template>
        </i18n-t>
      </NCheckbox>
      <NCheckbox id="accept-privacy" :model-value="modelValue.privacy" @update:model-value="set('privacy', $event)">
        <i18n-t keypath="auth.acceptPrivacyShort">
          <template #privacy>
            <NuxtLink :to="localePath('/privacy')" class="text-brand-700 hover:underline">{{ t('footer.privacy') }}</NuxtLink>
          </template>
        </i18n-t>
      </NCheckbox>
    </template>

    <p
      v-if="invalid && (!modelValue.terms || !modelValue.privacy)"
      role="alert"
      class="text-xs text-red-600"
    >
      {{ t('auth.consentRequired') }}
    </p>
  </div>
</template>
