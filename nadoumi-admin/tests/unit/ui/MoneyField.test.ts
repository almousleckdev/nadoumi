import { describe, it, expect, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { mountOpts } from '../../helpers'
import MoneyField from '@/components/ui/MoneyField.vue'

vi.mock('@/api/fx', () => ({ cnyToUsdRate: vi.fn().mockResolvedValue(0.1) }))

const mountField = (props: Record<string, unknown> = {}) => mount(MoneyField, {
  props: { modelValue: 1000, label: 'Amount (RMB)', prop: 'amount', ...props },
  ...mountOpts(),
})

describe('MoneyField', () => {
  it('shows the USD equivalent at the current rate', async () => {
    const w = mountField()
    await flushPromises()

    expect(w.text()).toContain('≈ $100')
  })

  it('shows nothing for a zero amount', async () => {
    const w = mountField({ modelValue: 0 })
    await flushPromises()

    expect(w.text()).not.toContain('≈')
  })

  it('shows the note under the field when given', async () => {
    const w = mountField({ note: 'Converted at the configured rate.' })
    await flushPromises()

    expect(w.text()).toContain('Converted at the configured rate.')
  })
})
