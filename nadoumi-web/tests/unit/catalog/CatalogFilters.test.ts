import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import CatalogSearchForm from '~/components/catalog/CatalogSearchForm.vue'
import FilterSelect from '~/components/catalog/FilterSelect.vue'
import FilterTextInput from '~/components/catalog/FilterTextInput.vue'
import FilterToggle from '~/components/catalog/FilterToggle.vue'

describe('FilterTextInput', () => {
  it('commits the trimmed text on change', async () => {
    const w = await mountSuspended(FilterTextInput, { props: { placeholder: 'City' } })

    await w.find('input').setValue('  Beijing ')

    expect(w.emitted('commit')?.[0]).toEqual(['Beijing'])
  })

  it('capitalises when asked, and commits an empty string when cleared', async () => {
    const w = await mountSuspended(FilterTextInput, { props: { placeholder: 'Country', uppercase: true, maxlength: 2 } })

    await w.find('input').setValue('cn')
    await w.find('input').setValue('   ')

    expect(w.emitted('commit')).toEqual([['CN'], ['']])
    expect(w.find('input').classes()).toContain('uppercase')
    expect(w.find('input').attributes('maxlength')).toBe('2')
  })

  it('shows the current value', async () => {
    const w = await mountSuspended(FilterTextInput, { props: { placeholder: 'City', value: 'Shanghai' } })

    expect((w.find('input').element as HTMLInputElement).value).toBe('Shanghai')
  })
})

describe('FilterSelect', () => {
  const options = [{ value: '', label: 'Any funding' }, { value: 'FULLY', label: 'Fully funded' }]

  it('lists the options, names the control, and selects the current value', async () => {
    const w = await mountSuspended(FilterSelect, { props: { label: 'Funding', options, value: 'FULLY' } })

    expect(w.find('select').attributes('aria-label')).toBe('Funding')
    expect(w.findAll('option').map(o => o.text())).toEqual(['Any funding', 'Fully funded'])
    expect((w.find('select').element as HTMLSelectElement).value).toBe('FULLY')
  })

  it('commits the chosen value, with "" meaning any', async () => {
    const w = await mountSuspended(FilterSelect, { props: { label: 'Funding', options, value: 'FULLY' } })

    await w.find('select').setValue('')

    expect(w.emitted('commit')?.[0]).toEqual([''])
  })

  it('treats an unset or array value as the "any" choice', async () => {
    const w = await mountSuspended(FilterSelect, { props: { label: 'Funding', options, value: ['A', 'B'] } })

    expect((w.find('select').element as HTMLSelectElement).value).toBe('')
  })
})

describe('FilterToggle', () => {
  it('reflects and reports the checked state', async () => {
    const w = await mountSuspended(FilterToggle, { props: { checked: false, label: 'Featured' } })

    expect(w.text()).toContain('Featured')
    await w.find('input').setValue(true)

    expect(w.emitted('commit')?.[0]).toEqual([true])
  })
})

describe('CatalogSearchForm', () => {
  it('is a labelled search form that submits without reloading the page', async () => {
    const w = await mountSuspended(CatalogSearchForm, {
      props: { id: 'q', label: 'Search universities', placeholder: 'Search', modelValue: '' },
    })

    expect(w.find('form').attributes('role')).toBe('search')
    expect(w.find('label').text()).toBe('Search universities')
    await w.find('form').trigger('submit')

    expect(w.emitted('submit')).toHaveLength(1)
  })

  it('updates the model as the user types', async () => {
    const w = await mountSuspended(CatalogSearchForm, {
      props: { id: 'q', label: 'Search', placeholder: 'Search', modelValue: '' },
    })

    await w.find('input').setValue('fudan')

    expect(w.emitted('update:modelValue')?.[0]).toEqual(['fudan'])
  })
})
