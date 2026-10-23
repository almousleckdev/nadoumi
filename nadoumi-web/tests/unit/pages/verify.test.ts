import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import VerifyPage from '~/pages/verify.vue'

const state = vi.hoisted(() => ({ query: {} as Record<string, string> }))
const api = vi.hoisted(() => ({ publicGet: vi.fn() }))
mockNuxtImport('useRoute', () => () => ({ query: state.query, path: '/verify', params: {} }))
mockNuxtImport('useApi', () => () => ({ publicGet: api.publicGet }))

beforeEach(() => api.publicGet.mockReset())

async function mountPage(query: Record<string, string>) {
  state.query = query
  const w = await mountSuspended(VerifyPage)
  await flushPromises()
  return w
}

describe('verify page', () => {
  it('asks to scan the QR code when there is no number or code', async () => {
    const w = await mountPage({})

    expect(api.publicGet).not.toHaveBeenCalled()
    expect(w.text()).toContain('Scan the QR code')
  })

  it('shows the kind, number and date of a valid document, and nothing personal', async () => {
    api.publicGet.mockResolvedValue({ valid: true, kind: 'RECEIPT', number: 'NAD-RCT-2026-000007', issuedOn: '2026-10-22' })
    const w = await mountPage({ number: 'NAD-RCT-2026-000007', code: 'ABCD2345EFGH' })

    expect(api.publicGet).toHaveBeenCalledWith('documents/verify', { number: 'NAD-RCT-2026-000007', code: 'ABCD2345EFGH' })
    expect(w.find('[data-test="valid"]').text()).toContain('Payment receipt')
    expect(w.text()).toContain('NAD-RCT-2026-000007')
    expect(w.text()).toContain('2026-10-22')
  })

  it('says it could not verify a document that does not match', async () => {
    api.publicGet.mockResolvedValue({ valid: false, kind: null, number: null, issuedOn: null })
    const w = await mountPage({ number: 'NAD-RCT-2026-000007', code: 'WRONGWRONGWR' })

    expect(w.find('[data-test="invalid"]').exists()).toBe(true)
    expect(w.find('[data-test="valid"]').exists()).toBe(false)
  })

  it('shows a retry message when the check itself fails', async () => {
    // other components share useApi, so only the verification call fails
    api.publicGet.mockImplementation(async (path: string) => {
      if (path === 'documents/verify') throw new Error('network')
      return []
    })
    const w = await mountPage({ number: 'NAD-RCT-2026-000007', code: 'ABCD2345EFGH' })

    expect(w.find('[data-test="failed"]').exists()).toBe(true)
  })
})
