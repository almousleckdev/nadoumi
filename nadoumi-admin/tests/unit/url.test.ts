import { describe, it, expect } from 'vitest'
import { safeHref } from '@/utils/url'

describe('safeHref', () => {
  it('accepts an http/https URL', () => {
    expect(safeHref('https://example.edu')).toBe('https://example.edu')
    expect(safeHref('http://example.edu')).toBe('http://example.edu')
  })

  it('rejects a javascript: scheme', () => {
    expect(safeHref('javascript:alert(1)')).toBeNull()
  })

  it('rejects other executable/unexpected schemes', () => {
    expect(safeHref('data:text/html,<script>alert(1)</script>')).toBeNull()
    expect(safeHref('vbscript:msgbox(1)')).toBeNull()
  })

  it('rejects a non-URL string, empty, null, or non-string value', () => {
    expect(safeHref('not a url')).toBeNull()
    expect(safeHref('')).toBeNull()
    expect(safeHref(null)).toBeNull()
    expect(safeHref(undefined)).toBeNull()
    expect(safeHref(42)).toBeNull()
  })
})
