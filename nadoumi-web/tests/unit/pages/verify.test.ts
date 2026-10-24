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

  it('shows the kind, number and date of a valid document', async () => {
    api.publicGet.mockResolvedValue({ valid: true, kind: 'RECEIPT', number: 'NAD-RCT-2026-000007', issuedOn: '2026-10-22', details: null })
    const w = await mountPage({ number: 'NAD-RCT-2026-000007', code: 'ABCD2345EFGH' })

    expect(api.publicGet).toHaveBeenCalledWith('documents/verify', { number: 'NAD-RCT-2026-000007', code: 'ABCD2345EFGH' })
    expect(w.find('[data-test="valid"]').text()).toContain('Payment receipt')
    expect(w.text()).toContain('NAD-RCT-2026-000007')
    expect(w.text()).toContain('2026-10-22')
  })

  it('shows the student, programme and fee line behind a valid application form', async () => {
    api.publicGet.mockResolvedValue({
      valid: true, kind: 'APPLICATION_FORM', number: 'NAD-APP-2026-000007', issuedOn: '2026-10-22',
      details: {
        applicantName: 'Amina Diallo', applicationReference: 'NAD2026007', opportunity: 'MBBS', universityName: 'Fudan University',
        intake: 'AUTUMN_SEPTEMBER', feeAmount: 300, currency: 'CNY', feeStatus: 'CONFIRMED',
        feePaidAt: '2026-10-22T06:30:00Z', issuedAt: '2026-10-22T06:31:00Z',
      },
    })
    const w = await mountPage({ number: 'NAD-APP-2026-000007', code: 'ABCD2345EFGH' })

    const text = w.find('[data-test="details"]').text()
    for (const part of ['Amina Diallo', 'NAD2026007', 'MBBS', 'Fudan University', 'Paid and confirmed']) expect(text).toContain(part)
  })

  it('says it could not verify a document that does not match', async () => {
    api.publicGet.mockResolvedValue({ valid: false, kind: null, number: null, issuedOn: null, details: null })
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
