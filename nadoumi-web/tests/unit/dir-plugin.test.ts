import { describe, it, expect } from 'vitest'
import { dirFor } from '~/plugins/dir'

describe('dir plugin — dirFor', () => {
  it('maps ar to rtl', () => {
    expect(dirFor('ar')).toBe('rtl')
  })
  it('maps en to ltr', () => {
    expect(dirFor('en')).toBe('ltr')
  })
  it('maps fr to ltr', () => {
    expect(dirFor('fr')).toBe('ltr')
  })
  it('maps zh to ltr', () => {
    expect(dirFor('zh')).toBe('ltr')
  })
})
