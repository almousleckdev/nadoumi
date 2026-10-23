import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import PaymentPanel from '~/components/payment/PaymentPanel.vue'
import type { StudentPaymentDto } from '~/types/payment'

const api = vi.hoisted(() => ({
  overview: vi.fn(), start: vi.fn(), hold: vi.fn(), cancel: vi.fn(), qr: vi.fn(), submitProof: vi.fn(), documents: vi.fn(), document: vi.fn(), resendReceipt: vi.fn(),
}))
mockNuxtImport('useApplicationPayment', () => () => api)

const dto = (over: Partial<StudentPaymentDto> = {}): StudentPaymentDto => ({
  state: 'NOT_STARTED', required: true, settled: false, canStart: true, amount: 300, currency: 'CNY',
  payee: 'Nadoumi Education', reference: null, expiresAt: null, secondsLeft: 0, rejectionReason: null, ...over,
})

beforeEach(() => {
  Object.values(api).forEach(fn => fn.mockReset())
  api.documents.mockResolvedValue([])
  URL.createObjectURL = vi.fn(() => 'blob:qr')
  URL.revokeObjectURL = vi.fn()
})

describe('PaymentPanel', () => {
  it('offers Pay now for an unpaid draft and reports it as not settled', async () => {
    api.overview.mockResolvedValue(dto())
    const w = await mountSuspended(PaymentPanel, { props: { applicationId: 5 } })
    await flushPromises()

    expect(w.find('[data-test="pay"]').exists()).toBe(true)
    expect(w.emitted('settled')?.at(-1)).toEqual([false])
  })

  it('shows the QR, the countdown and the reference once a session is open', async () => {
    api.overview.mockResolvedValue(dto({ state: 'PENDING', reference: 'NAD-AB12CD34', secondsLeft: 872, canStart: false }))
    api.qr.mockResolvedValue(new Blob(['x'], { type: 'image/png' }))
    const w = await mountSuspended(PaymentPanel, { props: { applicationId: 5 } })
    await flushPromises()

    expect(w.find('[data-test="countdown"]').text()).toBe('14:32')
    expect(w.find('[data-test="reference"]').text()).toBe('NAD-AB12CD34')
    expect(w.find('img').attributes('src')).toBe('blob:qr')
    expect(w.find('[data-test="amount"]').classes()).toContain('text-red-600')
  })

  it('stops the countdown and holds the session as soon as the student starts the receipt', async () => {
    api.overview.mockResolvedValue(dto({ state: 'PENDING', reference: 'NAD-AB12CD34', secondsLeft: 600, canStart: false }))
    api.hold.mockResolvedValue(dto({ state: 'PENDING', reference: 'NAD-AB12CD34', secondsLeft: 1800, canStart: false }))
    api.qr.mockResolvedValue(new Blob(['x'], { type: 'image/png' }))
    const w = await mountSuspended(PaymentPanel, { props: { applicationId: 5 } })
    await flushPromises()

    await w.find('#payment-txn').setValue('BANK1234567')
    await flushPromises()

    expect(api.hold).toHaveBeenCalledOnce()
    expect(w.find('[data-test="countdown"]').exists()).toBe(false)
    expect(w.find('[data-test="payment-held"]').exists()).toBe(true)
  })

  it('lets the student submit once the payment is reported, before staff confirm it', async () => {
    api.overview.mockResolvedValue(dto({ state: 'PROOF_SUBMITTED', settled: true, reference: 'NAD-AB12CD34', canStart: false }))
    const w = await mountSuspended(PaymentPanel, { props: { applicationId: 5 } })
    await flushPromises()

    expect(w.find('[data-test="state-verifying"]').exists()).toBe(true)
    expect(w.emitted('settled')?.at(-1)).toEqual([true])
  })

  it('starts a session when Pay now is pressed', async () => {
    api.overview.mockResolvedValue(dto())
    api.start.mockResolvedValue(dto({ state: 'PENDING', reference: 'NAD-AB12CD34', secondsLeft: 900, canStart: false }))
    api.qr.mockResolvedValue(new Blob(['x']))
    const w = await mountSuspended(PaymentPanel, { props: { applicationId: 5 } })
    await flushPromises()

    await w.find('[data-test="pay"]').trigger('click')
    await flushPromises()

    expect(api.start).toHaveBeenCalledOnce()
    expect(w.find('[data-test="countdown"]').exists()).toBe(true)
  })

  it('reports settled once the fee is confirmed', async () => {
    api.overview.mockResolvedValue(dto({ state: 'CONFIRMED', settled: true, reference: 'NAD-AB12CD34', canStart: false }))
    const w = await mountSuspended(PaymentPanel, { props: { applicationId: 5 } })
    await flushPromises()

    expect(w.find('[data-test="state-confirmed"]').exists()).toBe(true)
    expect(w.emitted('settled')?.at(-1)).toEqual([true])
  })

  it('offers the receipt and the application form to download once the fee is confirmed', async () => {
    api.overview.mockResolvedValue(dto({ state: 'CONFIRMED', settled: true, reference: 'NAD-AB12CD34', canStart: false }))
    api.documents.mockResolvedValue([
      { kind: 'RECEIPT', number: 'NAD-RCT-2026-000007', version: 1, generatedAt: '2026-10-22T06:30:00', sizeBytes: 5000 },
      { kind: 'APPLICATION_FORM', number: 'NAD-APP-2026-000042', version: 1, generatedAt: '2026-10-22T06:30:00', sizeBytes: 9000 },
    ])
    api.document.mockResolvedValue(new Blob(['%PDF'], { type: 'application/pdf' }))
    const w = await mountSuspended(PaymentPanel, { props: { applicationId: 5 } })
    await flushPromises()

    expect(w.find('[data-test="download-RECEIPT"]').exists()).toBe(true)
    expect(w.find('[data-test="download-APPLICATION_FORM"]').exists()).toBe(true)
    await w.find('[data-test="download-RECEIPT"]').trigger('click')
    await flushPromises()
    expect(api.document).toHaveBeenCalledWith('RECEIPT')

    api.resendReceipt.mockResolvedValue(undefined)
    await w.find('[data-test="resend-receipt"]').trigger('click')
    await flushPromises()
    expect(api.resendReceipt).toHaveBeenCalledOnce()
  })

  it('tells the student the documents are being prepared until they exist', async () => {
    api.overview.mockResolvedValue(dto({ state: 'CONFIRMED', settled: true, canStart: false }))
    const w = await mountSuspended(PaymentPanel, { props: { applicationId: 5 } })
    await flushPromises()

    expect(w.find('[data-test="documents"]').text()).toContain('preparing')
    expect(w.find('[data-test="download-RECEIPT"]').exists()).toBe(false)
  })

  it('shows the staff reason after a rejection and lets the student pay again', async () => {
    api.overview.mockResolvedValue(dto({ state: 'REJECTED', rejectionReason: 'Amount does not match' }))
    const w = await mountSuspended(PaymentPanel, { props: { applicationId: 5 } })
    await flushPromises()

    expect(w.text()).toContain('Amount does not match')
    expect(w.find('[data-test="pay"]').exists()).toBe(true)
  })

  it('renders nothing when no fee is required', async () => {
    api.overview.mockResolvedValue(dto({ state: 'NOT_REQUIRED', required: false, settled: true }))
    const w = await mountSuspended(PaymentPanel, { props: { applicationId: 5 } })
    await flushPromises()

    expect(w.find('[data-test="pay"]').exists()).toBe(false)
    expect(w.emitted('settled')?.at(-1)).toEqual([true])
  })
})
