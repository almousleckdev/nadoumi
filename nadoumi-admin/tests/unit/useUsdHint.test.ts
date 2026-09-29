import { describe, it, expect, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'

const fx = vi.hoisted(() => ({ cnyToUsdRate: vi.fn() }))
vi.mock('@/api/fx', () => ({ cnyToUsdRate: fx.cnyToUsdRate, DEFAULT_CNY_USD_RATE: 0.1381 }))

import { useUsdHint } from '@/composables/useUsdHint'

describe('useUsdHint', () => {
  it('returns an empty hint for a missing or zero amount', () => {
    fx.cnyToUsdRate.mockResolvedValue(0.14)
    const { usdHint } = useUsdHint()
    expect(usdHint(null)).toBe('')
    expect(usdHint(0)).toBe('')
  })

  it('converts with the default rate until the live rate arrives', () => {
    fx.cnyToUsdRate.mockReturnValue(new Promise(() => {}))
    const { usdHint } = useUsdHint()
    expect(usdHint(10000)).toBe('≈ $1,381')
  })

  it('switches to the live rate once it resolves', async () => {
    fx.cnyToUsdRate.mockResolvedValue(0.2)
    const { usdHint } = useUsdHint()
    await flushPromises()
    expect(usdHint(10000)).toBe('≈ $2,000')
  })
})
