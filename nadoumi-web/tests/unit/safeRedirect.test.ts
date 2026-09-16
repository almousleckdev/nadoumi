import { describe, it, expect } from 'vitest'
import { isSafeRedirectPath } from '~/utils/safeRedirect'

describe('isSafeRedirectPath', () => {
  it('accepts a same-site path', () => {
    expect(isSafeRedirectPath('/dashboard/education')).toBe(true)
  })

  it('rejects a protocol-relative path', () => {
    expect(isSafeRedirectPath('//evil.com')).toBe(false)
  })

  it('rejects a backslash form a browser would treat as protocol-relative', () => {
    expect(isSafeRedirectPath('/\\evil.com')).toBe(false)
    expect(isSafeRedirectPath('\\\\evil.com')).toBe(false)
  })

  it('rejects a scheme separator', () => {
    expect(isSafeRedirectPath('/javascript:alert(1)')).toBe(false)
    expect(isSafeRedirectPath('https://evil.com')).toBe(false)
  })

  it('rejects a missing, empty, or non-rooted path', () => {
    expect(isSafeRedirectPath(undefined)).toBe(false)
    expect(isSafeRedirectPath(null)).toBe(false)
    expect(isSafeRedirectPath('')).toBe(false)
    expect(isSafeRedirectPath('dashboard')).toBe(false)
  })
})
