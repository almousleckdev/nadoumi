import type { VueWrapper } from '@vue/test-utils'

/**
 * Type into a FormComboboxField/FormPhoneField-style searchable combobox and click
 * the option matching `optionText`. Used wherever a country/language select was
 * replaced by the searchable combobox.
 */
export async function pickCombobox(w: VueWrapper, inputSelector: string, optionText: string) {
  const input = w.find(inputSelector)
  await input.trigger('focus')
  await input.setValue(optionText)
  await w.vm.$nextTick()
  const option = w.findAll('li[role="option"]').find(li => li.text().includes(optionText))
  if (!option) throw new Error(`No combobox option matching "${optionText}" for ${inputSelector}`)
  await option.trigger('mousedown')
}
