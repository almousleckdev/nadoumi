<script setup lang="ts">
import { getCountryCallingCode, parsePhoneNumberFromString } from 'libphonenumber-js'
import type { CountryCode } from 'libphonenumber-js'
import { COUNTRY_CODES } from '~/utils/countries'

/**
 * A phone number field with a searchable country/dial-code picker, so the student
 * picks a country instead of typing "+86" by hand. The stored value is still one
 * E.164-ish string ("+8613800000000"), so no other form or validator changes.
 */
const props = withDefaults(defineProps<{
  id: string
  label: string
  hint?: string
  error?: string
  required?: boolean
  defaultCountry?: CountryCode
}>(), { hint: undefined, error: undefined, defaultCountry: 'CN' })
const model = defineModel<string>({ required: true })
const { t } = useI18n()
const { countryLabel } = useLocaleOptions()

const country = ref<CountryCode>(props.defaultCountry)
const national = ref('')
const query = ref('')
const open = ref(false)
const activeIndex = ref(-1)

/** Every country the app already offers, paired with its dial code (skipped if libphonenumber has none). */
const dialOptions = computed(() => COUNTRY_CODES
  .map(code => code as CountryCode)
  .filter(code => callingCodeOf(code) !== null)
  .map(code => ({ code, label: `${countryLabel(code)} (+${callingCodeOf(code)})` }))
  .sort((a, b) => a.label.localeCompare(b.label)))

function callingCodeOf(code: CountryCode): string | null {
  try {
    return getCountryCallingCode(code)
  }
  catch {
    return null
  }
}

interface DialOption { code: CountryCode, label: string }

const filtered = computed(() => {
  const q = query.value.trim().toLowerCase()
  if (!q) return dialOptions.value
  return dialOptions.value.filter((o: DialOption) => o.label.toLowerCase().includes(q))
})

const selectedLabel = computed(() => dialOptions.value.find((o: DialOption) => o.code === country.value)?.label ?? '')
const displayValue = computed(() => (open.value ? query.value : selectedLabel.value))

let internalUpdate = false
function applyFromValue(value: string) {
  const parsed = value ? parsePhoneNumberFromString(value) : undefined
  if (parsed?.country) {
    country.value = parsed.country
    national.value = parsed.nationalNumber
  }
  else {
    national.value = value.replace(/^\+\d*/, '')
  }
}
applyFromValue(model.value)
watch(() => model.value, (value: string) => {
  if (internalUpdate) { internalUpdate = false; return }
  applyFromValue(value)
})

function recompute() {
  const digits = national.value.replace(/\D/g, '')
  const next = digits ? `+${getCountryCallingCode(country.value)}${digits}` : ''
  if (next === model.value) return
  internalUpdate = true
  model.value = next
}

function onNationalInput(event: Event) {
  national.value = (event.target as HTMLInputElement).value
  recompute()
}

function chooseCountry(code: CountryCode) {
  country.value = code
  query.value = ''
  open.value = false
  recompute()
}

function onQueryInput(event: Event) {
  query.value = (event.target as HTMLInputElement).value
  open.value = true
  activeIndex.value = filtered.value.length ? 0 : -1
}

function openList() {
  open.value = true
  query.value = ''
  activeIndex.value = dialOptions.value.findIndex((o: DialOption) => o.code === country.value)
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'ArrowDown') {
    event.preventDefault()
    if (!open.value) { openList(); return }
    activeIndex.value = Math.min(activeIndex.value + 1, filtered.value.length - 1)
  }
  else if (event.key === 'ArrowUp') {
    event.preventDefault()
    activeIndex.value = Math.max(activeIndex.value - 1, 0)
  }
  else if (event.key === 'Enter') {
    if (!open.value) return
    event.preventDefault()
    const chosen = filtered.value[activeIndex.value]
    if (chosen) chooseCountry(chosen.code)
  }
  else if (event.key === 'Escape') {
    if (open.value) { open.value = false; query.value = '' }
  }
}
</script>

<template>
  <NField :label="label" :for="id" :hint="hint" :error="error" :required="required">
    <div class="flex gap-2">
      <div class="relative w-36 shrink-0">
        <input
          type="text"
          role="combobox"
          :value="displayValue"
          :aria-label="t('profileForm.countryCode')"
          aria-autocomplete="list"
          :aria-expanded="open"
          autocomplete="off"
          class="w-full rounded-md border border-slate-200 px-2 py-2 text-sm outline-none focus-visible:border-brand-500"
          @focus="openList"
          @input="onQueryInput"
          @keydown="onKeydown"
          @blur="open = false; query = ''"
        >
        <ul
          v-if="open"
          role="listbox"
          class="absolute z-20 mt-1 max-h-60 w-56 overflow-y-auto rounded-md border border-slate-200 bg-white py-1 shadow-lg"
        >
          <li v-if="!filtered.length" class="px-3 py-2 text-sm text-slate-400">{{ t('form.noMatches') }}</li>
          <li
            v-for="(o, i) in filtered"
            :key="o.code"
            role="option"
            :aria-selected="o.code === country"
            class="cursor-pointer px-3 py-2 text-sm"
            :class="i === activeIndex ? 'bg-brand-50 text-brand-800' : 'text-slate-700 hover:bg-slate-50'"
            @mousedown.prevent="chooseCountry(o.code)"
          >
            {{ o.label }}
          </li>
        </ul>
      </div>
      <input
        :id="id"
        type="tel"
        :value="national"
        autocomplete="tel-national"
        :aria-invalid="!!error"
        class="w-full rounded-md border px-3 py-2 text-base outline-none transition-colors"
        :class="error ? 'border-red-400 focus-visible:border-red-500' : 'border-slate-200 focus-visible:border-brand-500'"
        @input="onNationalInput"
      >
    </div>
  </NField>
</template>
