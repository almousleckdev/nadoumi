import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import AsyncState from '~/components/dashboard/AsyncState.vue'

describe('AsyncState', () => {
  it('shows the loading slot while pending', async () => {
    const w = await mountSuspended(AsyncState, {
      props: { pending: true },
      slots: { default: () => 'DATA', loading: () => 'LOADING' },
    })
    expect(w.text()).toContain('LOADING')
    expect(w.text()).not.toContain('DATA')
  })

  it('shows the error slot / message when error is set', async () => {
    const w = await mountSuspended(AsyncState, {
      props: { pending: false, error: 'boom' },
      slots: { default: () => 'DATA' },
    })
    expect(w.text()).toContain('boom')
    expect(w.text()).not.toContain('DATA')
  })

  it('shows the empty fallback when empty', async () => {
    const w = await mountSuspended(AsyncState, {
      props: { pending: false, empty: true },
      slots: { default: () => 'DATA' },
    })
    expect(w.text()).not.toContain('DATA')
  })

  it('renders the default slot when settled and non-empty', async () => {
    const w = await mountSuspended(AsyncState, {
      props: { pending: false },
      slots: { default: () => 'DATA' },
    })
    expect(w.text()).toContain('DATA')
  })
})
